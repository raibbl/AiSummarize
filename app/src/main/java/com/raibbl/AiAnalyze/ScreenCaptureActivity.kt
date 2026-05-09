package com.raibbl.AiAnalyze

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.raibbl.AiAnalyze.data.billing.SubscriptionState
import com.raibbl.AiAnalyze.data.db.AppDatabase
import com.raibbl.AiAnalyze.data.db.SummaryItem
import com.raibbl.AiAnalyze.services.MediaProjectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ScreenCaptureActivity : Activity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var lastScreenshotPath: String? = null

    companion object {
        const val REQUEST_CODE_CAPTURE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        createNotificationChannel()

        // Ask user for screen capture consent (screenshot-only; no service needed)
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_CODE_CAPTURE) return

        if (resultCode != RESULT_OK || data == null) {
            Toast.makeText(this, "Screen capture permission denied.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val serviceIntent = Intent(this, MediaProjectionService::class.java).apply {
            putExtra(MediaProjectionService.EXTRA_RESULT_CODE, resultCode)
            putExtra(MediaProjectionService.EXTRA_DATA_INTENT, data)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
        else startService(serviceIntent)

        finish()
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    mediaProjection = null
                }
            }, Handler(Looper.getMainLooper()))

            captureScreen()
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Failed to start projection", e)
            Toast.makeText(this, "Failed to start projection: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    private fun captureScreen() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        val virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                val bitmap = imageToBitmap(image)
                image.close()

                bitmap?.let {
                    lastScreenshotPath = saveBitmapToFile(it)
                    analyzeTextWithMLKit(it)
                } ?: run {
                    Toast.makeText(this, "Failed to capture screen.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                Toast.makeText(this, "No image captured.", Toast.LENGTH_SHORT).show()
                finish()
            }

            virtualDisplay?.release()
            imageReader.close()
            mediaProjection?.stop()
        }, 300)
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        return try {
            val file = File(cacheDir, "screenshot_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val buffer = image.planes[0].buffer
        val pixelStride = image.planes[0].pixelStride
        val rowStride = image.planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun analyzeTextWithMLKit(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                summarizeTextWithVertexAI(visionText.text)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun summarizeTextWithVertexAI(inputText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check usage limit before summarizing
                val app = application.brieflyApp
                val isPro = app.billingManager.subscriptionState.value is SubscriptionState.Pro
                val freeLimit = app.remoteConfigManager.getFreeSummaryLimit()

                app.usageRepository.ensureAuthenticated()
                if (!app.usageRepository.canSummarize(isPro, freeLimit)) {
                    val resetDate = app.usageRepository.getResetDate()
                    Handler(Looper.getMainLooper()).post {
                        val intent = Intent(this@ScreenCaptureActivity, UpgradeActivity::class.java).apply {
                            putExtra(UpgradeActivity.EXTRA_FREE_LIMIT, freeLimit)
                            putExtra(UpgradeActivity.EXTRA_RESET_DATE, resetDate)
                        }
                        startActivity(intent)
                        finish()
                    }
                    return@launch
                }

                val summaryText = SummaryGeminiService.summarizeScreenText(inputText)

                val db = AppDatabase.getDatabase(this@ScreenCaptureActivity)
                val summaryDao = db.summaryDao()
                val summaryItem = SummaryItem(
                    type = "screenshot",
                    link = null,
                    title = "Screenshot summary",
                    summary = summaryText,
                    imagePath = lastScreenshotPath
                )
                summaryDao.insertSummary(summaryItem)

                // Increment usage count after successful summary
                app.usageRepository.incrementCount()

                Handler(Looper.getMainLooper()).post {
                    val intent = Intent(this@ScreenCaptureActivity, ScreenshotSummaryActivity::class.java).apply {
                        putExtra("EXTRA_SUMMARY", summaryText)
                        putExtra("EXTRA_SCREENSHOT_PATH", lastScreenshotPath)
                    }
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ScreenCaptureActivity, "Summarization failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "summary_channel",
                "Summary Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays the AI-generated summary"
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun showSummaryNotification(summary: String) {
        createNotificationChannel()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(this, "summary_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("AI Summary")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        notificationManager.notify(1001, notificationBuilder.build())
    }
}
