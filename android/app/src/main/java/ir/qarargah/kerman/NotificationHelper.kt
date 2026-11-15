package ir.qarargah.kerman

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.math.abs

object NotificationHelper {

    fun stableId(key: String): Int = abs(key.hashCode() % 100_000)

    fun show(
        ctx: Context,
        id: Int,
        title: String,
        body: String,
        channel: String = Notifications.CHANNEL_EVENTS
    ) {
        Notifications.ensureChannels(ctx)

        val open = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            ctx,
            id,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ctx.getColor(R.color.brand_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(id, notification)
        } catch (e: SecurityException) {
            // اجازهٔ اعلان داده نشده است
        }
    }

    fun cancelAll(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.cancelAll()
    }

    fun canNotify(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()
        } else true
}
