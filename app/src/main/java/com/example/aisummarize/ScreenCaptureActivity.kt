package com.example.aisummarize

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import com.example.aisummarize.services.MediaProjectionService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class ScreenCaptureActivity : Activity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var projectionResult: Intent? = null
    private var projectionResultCode: Int = 0

    companion object {
        const val REQUEST_CODE_CAPTURE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, REQUEST_CODE_CAPTURE)
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
                Toast.makeText(this, "Detected text: ${visionText.text}", Toast.LENGTH_LONG).show()

                println(visionText.text)
            }
            .addOnFailureListener { e ->
                // Handle errors
                Toast.makeText(this, "Text recognition failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
