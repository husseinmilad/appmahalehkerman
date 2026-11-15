package ir.qarargah.kerman

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class QarargahApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
    }
}

object Notifications {
    /** یادآوری مراسمات */
    const val CHANNEL_EVENTS = "events"

    /** اعلان‌های عمومی قرارگاه (خبر، اطلاعیه) */
    const val CHANNEL_NEWS = "news"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EVENTS,
                ctx.getString(R.string.channel_events),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = ctx.getString(R.string.channel_events_desc)
                enableVibration(true)
            }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NEWS,
                ctx.getString(R.string.channel_news),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ctx.getString(R.string.channel_news_desc)
            }
        )
    }
}
