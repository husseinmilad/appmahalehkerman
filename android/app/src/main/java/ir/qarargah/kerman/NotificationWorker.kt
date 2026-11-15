package ir.qarargah.kerman

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/** نمایش یادآوری مراسم پیش‌رو در زمان مقرر. */
class NotificationWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val body = inputData.getString(KEY_BODY).orEmpty()
        val id = inputData.getInt(KEY_ID, NotificationHelper.stableId(title))

        NotificationHelper.show(
            applicationContext,
            id = id,
            title = title,
            body = body,
            channel = Notifications.CHANNEL_EVENTS
        )
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_ID = "id"
    }
}
