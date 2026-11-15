package ir.qarargah.kerman

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/** Toast امن، حتی وقتی روی ترد اصلی نیستیم. */
object ToastCompat {
    fun short(ctx: Context, msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(ctx.applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
