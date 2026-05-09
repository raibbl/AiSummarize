package com.raibbl.AiAnalyze.services

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.raibbl.AiAnalyze.R
import com.raibbl.AiAnalyze.ScreenshotSummaryActivity
import com.raibbl.AiAnalyze.SummaryGeminiService
import com.raibbl.AiAnalyze.data.db.AppDatabase
import com.raibbl.AiAnalyze.data.db.SummaryItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MediaProjectionService : Service() {

    companion object {
        const val CHANNEL_ID = "media_projection_service_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA_INTENT = "data"
    }

    private var mediaProjection: MediaProjection? = null
    private var lastScreenshotPath: String? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: android.hardware.display.VirtualDisplay? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED

        val dataIntent: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA_INTENT)
        }

        if (resultCode != Activity.RESULT_OK || dataIntent == null) {
            Log.e("MPS", "Missing projection extras; stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        // REQUIRED: MediaProjection needs a typed FGS on your device/SDK
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        try {
            val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mgr.getMediaProjection(resultCode, dataIntent)

            if (mediaProjection == null) {
                Log.e("MPS", "getMediaProjection returned null")
                stopSelf()
                return START_NOT_STICKY
            }

            // ✅ REQUIRED on newer Android: register callback BEFORE createVirtualDisplay
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d("MPS", "MediaProjection stopped")
                    cleanup()
                    stopSelf()
                }
            }, Handler(Looper.getMainLooper()))

            captureOnce()

        } catch (e: Exception) {
            Log.e("MPS", "Failed to start projection in service", e)
            cleanup()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun captureOnce() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null
        )

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val img = imageReader?.acquireLatestImage()
                if (img == null) {
                    Log.e("MPS", "No image captured")
                    cleanup()
                    stopSelf()
                    return@postDelayed
                }

                val bitmap = imageToBitmap(img)
                img.close()

                if (bitmap == null) {
                    Log.e("MPS", "Bitmap conversion failed")
                    cleanup()
                    stopSelf()
                    return@postDelayed
                }

                lastScreenshotPath = saveBitmapToFile(bitmap)
                analyzeTextWithMLKit(bitmap)

                // do NOT cleanup yet; we cleanup after ML/summarization finishes
                // (but we can release VD/reader right away after image acquired)
                virtualDisplay?.release()
                virtualDisplay = null
                imageReader?.close()
                imageReader = null

                // You can also stop projection now; bitmap is already captured
                mediaProjection?.stop()
                mediaProjection = null

            } catch (e: Exception) {
                Log.e("MPS", "Capture failed", e)
                cleanup()
                stopSelf()
            }
        }, 300)
    }

    private fun analyzeTextWithMLKit(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                summarizeTextWithGeminiAndPersist(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e("MPS", "Text recognition failed", e)
                cleanup()
                stopSelf()
            }
    }

    private fun summarizeTextWithGeminiAndPersist(inputText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summaryText = SummaryGeminiService.summarizeScreenText(inputText)

                val db = AppDatabase.getDatabase(this@MediaProjectionService)
                val summaryDao = db.summaryDao()
                val summaryItem = SummaryItem(
                    type = "screenshot",
                    link = null,
                    title = "Screenshot summary",
                    summary = summaryText,
                    imagePath = lastScreenshotPath
                )
                summaryDao.insertSummary(summaryItem)

                Handler(Looper.getMainLooper()).post {
                    // Start UI from Service
                    val uiIntent = Intent(this@MediaProjectionService, ScreenshotSummaryActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("EXTRA_SUMMARY", summaryText)
                        putExtra("EXTRA_SCREENSHOT_PATH", lastScreenshotPath)
                    }
                    startActivity(uiIntent)

                    cleanup()
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e("MPS", "Summarization failed", e)
                Handler(Looper.getMainLooper()).post {
                    cleanup()
                    stopSelf()
                }
            }
        }
    }

    private fun saveBitmapToFile(bitmap: Bitmap): String? {
        return try {
            val file = File(cacheDir, "screenshot_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("MPS", "Save bitmap failed", e)
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

    private fun cleanup() {
        try { virtualDisplay?.release() } catch (_: Exception) {}
        virtualDisplay = null
        try { imageReader?.close() } catch (_: Exception) {}
        imageReader = null
        try { mediaProjection?.stop() } catch (_: Exception) {}
        mediaProjection = null
        stopForeground(true)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Projection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                lightColor = Color.BLUE
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Capturing screenshot")
            .setContentText("Working…")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }
}
