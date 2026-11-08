package com.sameerasw.essentials.debug

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.NotificationListener

class FakeLiveUpdatesReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        if (intent.getBooleanExtra("stop", false)) {
            stop(app)
            return
        }
        start(app, intent.getIntExtra("count", 3).coerceIn(1, 10), intent.getLongExtra("duration", 20_000L))
    }

    companion object {
        private const val CHANNEL = "fake_live_updates"
        private const val TICK_MS = 500L
        private val handler = Handler(Looper.getMainLooper())
        private var running: Runnable? = null
        private var posted = 0

        private val titles = listOf("Downloading video.mp4", "Uploading photos", "Installing update", "Syncing files", "Backing up", "Exporting project", "Copying music", "Rendering", "Importing contacts", "Sending files")

        private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

        fun start(context: Context, count: Int, duration: Long) {
            stop(context)
            manager(context).createNotificationChannel(NotificationChannel(CHANNEL, "Fake live updates", NotificationManager.IMPORTANCE_LOW))
            posted = count
            val started = System.currentTimeMillis()
            val tick = object : Runnable {
                override fun run() {
                    val elapsed = System.currentTimeMillis() - started
                    var active = 0
                    repeat(count) { i ->
                        val span = duration * (i + 2) / (count + 1)
                        val pct = (elapsed * 100 / span).toInt()
                        if (pct >= 100) {
                            manager(context).cancel(NotificationListener.FAKE_LIVE_UPDATE_TAG, i)
                        } else {
                            active++
                            post(context, i, pct, indeterminate = i == count - 1 && count > 2 && pct < 30)
                        }
                    }
                    if (active > 0) handler.postDelayed(this, TICK_MS) else running = null
                }
            }
            running = tick
            handler.post(tick)
        }

        fun stop(context: Context) {
            running?.let(handler::removeCallbacks)
            running = null
            repeat(maxOf(posted, 10)) { manager(context).cancel(NotificationListener.FAKE_LIVE_UPDATE_TAG, it) }
        }

        private fun post(context: Context, id: Int, pct: Int, indeterminate: Boolean) {
            val notification = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(titles[id % titles.size])
                .setContentText(if (indeterminate) "Preparing…" else "$pct%")
                .setProgress(100, pct, indeterminate)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
            manager(context).notify(NotificationListener.FAKE_LIVE_UPDATE_TAG, id, notification)
        }
    }
}
