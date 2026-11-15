package ir.qarargah.kerman

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.WorkManager
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * زمان‌بندی یادآوری مراسمات. وب‌اپ فهرست یادآوری‌ها را به‌صورت JSON می‌فرستد:
 * [{ "id": "...", "title": "...", "body": "...", "at": 1760000000000 }]
 *
 * برای هر یادآوری یک WorkManager با تأخیر مشخص ثبت می‌شود؛ پس از ری‌استارت گوشی هم
 * WorkManager خودش آن‌ها را بازسازی می‌کند.
 */
object Scheduler {

    private const val TAG = "qarargah-reminder-"
    private const val MIN_DELAY_MS = 15_000L // WorkManager حداقل چند ثانیه‌ای اجرا می‌شود
    private const val MAX_ITEMS = 60

    fun sync(ctx: Context, json: String) {
        val items = runCatching { JSONArray(json.ifBlank { "[]" }) }.getOrElse { JSONArray() }
        val wm = WorkManager.getInstance(ctx)

        // پاک کردن یادآوری‌های قبلی
        wm.cancelAllWorkByTag(TAG + "all")

        val now = System.currentTimeMillis()
        for (i in 0 until minOf(items.length(), MAX_ITEMS)) {
            val o = items.optJSONObject(i) ?: continue
            val at = o.optLong("at", 0L)
            if (at <= now) continue
            val delay = at - now
            if (delay < MIN_DELAY_MS) continue

            val data = Data.Builder()
                .putString(NotificationWorker.KEY_TITLE, o.optString("title"))
                .putString(NotificationWorker.KEY_BODY, o.optString("body"))
                .putInt(NotificationWorker.KEY_ID, NotificationHelper.stableId(o.optString("id") + at))
                .build()

            val request = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .addTag(TAG + "all")
                .build()

            wm.enqueueUniqueWork(TAG + o.optString("id", i.toString()), ExistingWorkPolicy.REPLACE, request)
        }
    }
}
