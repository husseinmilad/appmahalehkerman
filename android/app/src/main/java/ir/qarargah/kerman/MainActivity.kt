package ir.qarargah.kerman

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * پوستهٔ اندروید برنامهٔ «قرارگاه مردمی مدافعان وطن — محله کرمان».
 *
 * محتوا همان وب‌اپ مخزن است (docs/index.html) که از GitHub Pages نمایش داده می‌شود؛
 * نسخهٔ آفلاین هم داخل assets بسته‌بندی شده تا اپ در حالت بی‌اینترنت هم باز شود.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var progress: ProgressBar
    private lateinit var offlineBanner: View
    private lateinit var root: FrameLayout

    private val remoteUrl: String get() = BuildConfig.APP_URL
    private val bridge = AndroidBridge()

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notifyResult(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        root = findViewById(R.id.root)
        webView = findViewById(R.id.web)
        swipe = findViewById(R.id.swipe)
        progress = findViewById(R.id.progress)
        offlineBanner = findViewById(R.id.offline_banner)

        setupWebView()
        setupSwipe()
        setupInsets()
        setupBackNavigation()

        val startUrl = intent?.data?.toString()?.takeIf { isTrusted(it) } ?: remoteUrl
        if (savedInstanceState == null) {
            loadSmart(startUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    // ---------------------------------------------------------------- راه‌اندازی

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setBackgroundColor(getColor(R.color.brand_green))

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "$userAgentString QarargahKerman/${BuildConfig.VERSION_NAME}"
            allowFileAccess = false
            allowContentAccess = false
        }

        CookieManager.getInstance().setAcceptCookie(true)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false)
        }

        webView.addJavascriptInterface(bridge, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                return if (isTrusted(url.toString())) {
                    false
                } else {
                    openExternal(url)
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progress.visibility = View.GONE
                swipe.isRefreshing = false
                applyThemeColor()
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    progress.visibility = View.GONE
                    swipe.isRefreshing = false
                    loadOfflineBundle()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    progress.visibility = View.VISIBLE
                    progress.progress = newProgress
                } else {
                    progress.visibility = View.GONE
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, false, false)
            }
        }
    }

    private fun setupSwipe() {
        swipe.setColorSchemeColors(getColor(R.color.brand_accent))
        swipe.setProgressBackgroundColorSchemeColor(getColor(R.color.brand_green))
        swipe.setOnRefreshListener {
            if (isOnline()) {
                webView.reload()
            } else {
                swipe.isRefreshing = false
                loadOfflineBundle()
                ToastCompat.short(this, getString(R.string.offline_mode))
            }
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(offlineBanner) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> { topMargin = bars.top }
            insets
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // ابتدا به خود برنامهٔ وب اطلاع می‌دهیم (بستن پنجرهٔ عکس، بازگشت از صفحه)
                webView.evaluateJavascript(
                    "(function(){try{return window.onBack&&window.onBack()?'1':'0'}catch(e){return '0'}})()"
                ) { result ->
                    val handled = result?.contains("1") == true
                    when {
                        handled -> Unit
                        webView.canGoBack() -> webView.goBack()
                        else -> finish()
                    }
                }
            }
        })
    }

    // ---------------------------------------------------------------- بارگذاری

    private fun loadSmart(url: String) {
        if (!isOnline()) {
            loadOfflineBundle()
            return
        }
        progress.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    /** نسخهٔ ذخیره‌شده در اپ، برای وقتی که اینترنت نیست یا سرور پاسخ نمی‌دهد. */
    private fun loadOfflineBundle() {
        offlineBanner.visibility = View.VISIBLE
        webView.loadUrl("file:///android_asset/web/index.html")
    }

    private fun isTrusted(url: String): Boolean =
        url.startsWith(BuildConfig.APP_URL) || url.startsWith("file:///android_asset/")

    private fun openExternal(uri: Uri) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            ToastCompat.short(this, getString(R.string.no_app_for_link))
        }
    }

    private fun applyThemeColor() {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        root.setBackgroundColor(
            getColor(if (night) R.color.brand_green_dark else R.color.brand_green)
        )
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // ---------------------------------------------------------------- اعلان‌ها

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            notifyResult(true)
            return
        }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            notifyResult(true)
        } else {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun notifyResult(granted: Boolean) {
        val msg = getString(if (granted) R.string.notif_enabled else R.string.notif_denied)
            .replace("'", "\\'")
        webView.post {
            webView.evaluateJavascript("try{toast('$msg')}catch(e){}", null)
        }
    }

    // ---------------------------------------------------------------- چرخهٔ عمر

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val url = intent.data?.toString() ?: return
        if (isTrusted(url)) loadSmart(url) else openExternal(Uri.parse(url))
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- پل JavaScript

    /**
     * پلی بین وب‌اپ و اندروید. وب‌اپ این توابع را با window.Android صدا می‌زند:
     *   Android.notify(title, body)
     *   Android.askPermission()
     *   Android.schedule(json)   ← [{id,title,body,at(ms)}]
     *   Android.version()        ← "1.0.0"
     *   Android.share(text)
     *   Android.exit()
     */
    inner class AndroidBridge {

        @JavascriptInterface
        fun notify(title: String, body: String) {
            NotificationHelper.show(
                this@MainActivity,
                id = NotificationHelper.stableId(title + body),
                title = title,
                body = body,
                channel = Notifications.CHANNEL_NEWS
            )
        }

        @JavascriptInterface
        fun askPermission() {
            runOnUiThread { ensureNotificationPermission() }
        }

        @JavascriptInterface
        fun schedule(json: String) {
            Scheduler.sync(this@MainActivity, json)
        }

        @JavascriptInterface
        fun version(): String = BuildConfig.VERSION_NAME

        @JavascriptInterface
        fun share(text: String) {
            runOnUiThread {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(send, getString(R.string.share_app)))
            }
        }

        @JavascriptInterface
        fun exit() {
            runOnUiThread { finish() }
        }
    }
}
