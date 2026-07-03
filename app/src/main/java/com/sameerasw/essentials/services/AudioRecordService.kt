package com.sameerasw.essentials.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.R
import java.io.File

class AudioRecordService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var recordedFile: File? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (isRecording) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                val privateFile = recordedFile
                if (privateFile != null && privateFile.exists()) {
                    val publicUri = saveToPublicMusic(privateFile)
                    if (publicUri != null) {
                        privateFile.delete()
                        showSavedNotification(publicUri)
                    } else {
                        val authority = "${packageName}.fileprovider"
                        val privateUri = androidx.core.content.FileProvider.getUriForFile(this, authority, privateFile)
                        showSavedNotification(privateUri)
                    }
                }
            } catch (_: Exception) {}
            mediaRecorder = null
            isRecording = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Audio record permission required", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        val musicDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        if (musicDir == null) {
            Toast.makeText(this, "Failed to access storage", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        val outputFile = File(musicDir, "recording_${System.currentTimeMillis()}.m4a")
        recordedFile = outputFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true

            startForeground(NOTIF_ID, buildNotification())
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            val privateFile = recordedFile
            if (privateFile != null && privateFile.exists()) {
                val publicUri = saveToPublicMusic(privateFile)
                if (publicUri != null) {
                    privateFile.delete()
                    showSavedNotification(publicUri)
                } else {
                    val authority = "${packageName}.fileprovider"
                    val privateUri = androidx.core.content.FileProvider.getUriForFile(this, authority, privateFile)
                    showSavedNotification(privateUri)
                }
            }
        } catch (_: Exception) {
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show()
    }

    private fun saveToPublicMusic(file: File): android.net.Uri? {
        try {
            val resolver = contentResolver
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, file.name)
                put(android.provider.MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Essentials")
                    put(android.provider.MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = resolver.insert(collection, values) ?: return null

            resolver.openOutputStream(itemUri)?.use { outStream ->
                file.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(android.provider.MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }

            return itemUri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun showSavedNotification(uri: android.net.Uri) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "audio/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.rounded_mic_24)
                .setContentTitle(getString(R.string.audio_record_saved_title))
                .setContentText(getString(R.string.audio_record_saved_desc))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(43, notification)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create saved notification: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, AudioRecordService::class.java).apply {
            action = ACTION_TOGGLE_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.rounded_mic_24)
            .setContentTitle(getString(R.string.audio_record_notif_title))
            .setOngoing(true)
            .addAction(
                R.drawable.rounded_mic_24,
                getString(R.string.audio_record_notif_stop),
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val name = getString(R.string.audio_record_notif_channel_name)
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_TOGGLE_RECORDING = "com.sameerasw.essentials.ACTION_TOGGLE_RECORDING"
        const val NOTIF_ID = 42
        const val CHANNEL_ID = "audio_record_channel"
        var isRecording = false
            private set
    }
}
