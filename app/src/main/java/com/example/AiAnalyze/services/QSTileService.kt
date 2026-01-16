package com.raibbl.AiAnalyze.services

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.raibbl.AiAnalyze.ScreenCaptureActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions


class QSTileService : TileService() {

    private lateinit var mediaProjectionManager: MediaProjectionManager


    // Called when the user adds your tile.
    override fun onTileAdded() {
        ensureTextRecognitionModelReady()
        super.onTileAdded()

    }

    override fun onStartListening() {
        super.onStartListening()
        mediaProjectionManager =
            getSystemService(Activity.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    // Called when your app can no longer update your tile.
    override fun onStopListening() {
        super.onStopListening()
    }


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onClick() {
        super.onClick()

        // Create an intent for ScreenCaptureActivity
        val intent = Intent(this, ScreenCaptureActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required for launching from a service

        // Wrap the intent in a PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use startActivityAndCollapse with the PendingIntent
        startActivityAndCollapse(pendingIntent)
    }

    // Called when the user removes your tile.
    override fun onTileRemoved() {
        super.onTileRemoved()
    }

    private fun ensureTextRecognitionModelReady() {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(InputImage.fromBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), 0))
            .addOnSuccessListener {
                Toast.makeText(this, "Text recognition model is ready!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load text recognition model: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }



}
