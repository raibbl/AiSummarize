package com.example.aisummarize

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.aisummarize.services.MediaProjectionService
import com.example.aisummarize.data.db.AppDatabase
import com.example.aisummarize.data.db.SummaryItem
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream


class ScreenCaptureActivity : Activity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var projectionResult: Intent? = null
    private var projectionResultCode: Int = 0
    private var lastScreenshotPath: String? = null

    companion object {
        const val REQUEST_CODE_CAPTURE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_CAPTURE)
        createNotificationChannel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_CAPTURE && resultCode == RESULT_OK && data != null) {
            // Store the projection result
            projectionResult = data
            projectionResultCode = resultCode

            // Start the service first
            val serviceIntent = Intent(this, MediaProjectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            // Wait for service to be fully started
            Handler(Looper.getMainLooper()).postDelayed({
                startProjection()
            }, 500) // Give the service more time to start
        } else {
            Toast.makeText(this, "Screen capture permission denied.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    private fun startProjection() {
        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(
                projectionResultCode,
                projectionResult!!
            )

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    mediaProjection = null
                }
            }, Handler(Looper.getMainLooper()))

            captureScreen()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start projection: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }



    private fun captureScreen() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        val imageReader = android.media.ImageReader.newInstance(
            width, height, android.graphics.PixelFormat.RGBA_8888, 2
        )

        val virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
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
                    // Save bitmap so we can show it later in the screenshot summary screen
                    lastScreenshotPath = saveBitmapToFile(it)
                    analyzeTextWithMLKit(it)
                } ?: run {
                    Toast.makeText(this, "Failed to capture screen.", Toast.LENGTH_SHORT).show()
                    finish()
                }
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

    private fun imageToBitmap(image: android.media.Image): Bitmap? {
        val buffer = image.planes[0].buffer
        val pixelStride = image.planes[0].pixelStride
        val rowStride = image.planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        // Create a Bitmap
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // Crop the extra padding
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }


    private fun analyzeTextWithMLKit(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Display the recognized text
                //Toast.makeText(this, "Detected text: ${visionText.text}", Toast.LENGTH_LONG).show()
                summarizeTextWithVertexAI(visionText.text)
                println(visionText.text)
            }
            .addOnFailureListener { e ->
                // Handle errors
                Toast.makeText(this, "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun summarizeTextWithVertexAI(inputText: String) {
        // Use a coroutine to call Gemini and then persist the result
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summaryText = SummaryGeminiService.summarizeScreenText(inputText)

                // Save summary in Room so it behaves like link-based summaries
                val db = AppDatabase.getDatabase(this@ScreenCaptureActivity)
                val summaryDao = db.summaryDao()
                val summaryItem = SummaryItem(
                    type = "screenshot",
                    link = null,
                    title = "Screenshot summary",
                    summary = summaryText,
                    imagePath = lastScreenshotPath
                )
                val id = summaryDao.insertSummary(summaryItem).toInt()

                // Launch screenshot-specific summary screen with image + text
                Handler(Looper.getMainLooper()).post {
                    val intent = Intent(this@ScreenCaptureActivity, ScreenshotSummaryActivity::class.java).apply {
                        putExtra("EXTRA_SUMMARY", summaryText)
                        putExtra("EXTRA_SCREENSHOT_PATH", lastScreenshotPath)
                    }
                    println("Launching ScreenshotSummaryActivity for screenshot summary id=$id, path=$lastScreenshotPath")
                    startActivity(intent)
                    println("Summary: $summaryText")
                    finish()
                }
            } catch (e: Exception) {
                // Handle errors
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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showSummaryNotification(summary: String) {
        createNotificationChannel()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationBuilder = NotificationCompat.Builder(this, "summary_channel")
            .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
            .setContentTitle("AI Summary")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary)) // Expandable text
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)

        // Show the notification
        notificationManager.notify(1001, notificationBuilder.build())
    }


}
