# WebView + JavaScript interface
-keepclassmembers class ir.qarargah.kerman.MainActivity$AndroidBridge {
    public *;
}
-keep class ir.qarargah.kerman.MainActivity { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# AndroidX WebKit
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**

# WorkManager workers are instantiated reflectively
-keep class ir.qarargah.kerman.NotificationWorker { *; }
