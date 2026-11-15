package ir.qarargah.kerman

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * پس از روشن شدن گوشی یا به‌روزرسانی برنامه، کانال‌های اعلان بازسازی می‌شوند.
 * زمان‌بندی WorkManager توسط خود سیستم پایدار می‌ماند.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Notifications.ensureChannels(context)
        }
    }
}
