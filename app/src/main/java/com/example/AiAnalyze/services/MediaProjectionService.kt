package com.raibbl.AiAnalyze.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.projection.MediaProjection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.raibbl.AiAnalyze.R

class MediaProjectionService : Service() {

    companion object {
        const val CHANNEL_ID = "media_projection_service_channel"
        const val NOTIFICATION_ID = 1
        var mediaProjection: MediaProjection? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Display the notification to keep the service in the foreground
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Perform screen capture logic here if needed
        return START_STICKY
    }

    override fun onDestroy() {
        mediaProjection?.stop()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Projection Service",
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
            .setContentTitle("Screen Capture Running")
            .setContentText("Capturing your screen.")
            .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app icon
            .build()
    }
}
