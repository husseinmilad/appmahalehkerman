# کارهایی که فقط با دسترسی ادمینِ مخزن انجام می‌شود

توکن فعلی به بخش **Settings** و **Pages** دسترسی ندارد؛ این دو مورد را یک‌بار در مرورگر انجام دهید.

## ۱) فعال‌سازی نسخهٔ وب (۲ کلیک)

**Settings ← Pages** ← در بخش *Source* گزینهٔ **Deploy from a branch** را انتخاب کنید ←
**Branch: `main`** و **Folder: `/docs`** ← **Save**

پس از حدود یک دقیقه:

| آدرس | محتوا |
|---|---|
| `https://husseinmilad.github.io/appmahalehkerman/` | صفحهٔ معرفی و دانلود |
| `https://husseinmilad.github.io/appmahalehkerman/docs/` | خود برنامه |

> ⚠️ بدون این مرحله، آدرس `AppUrl` داخل اپ اندروید (که اکنون به همین آدرس اشاره می‌کند) باز نمی‌شود؛ ولی اپ به‌صورت آفلاین از بستهٔ داخلی خودش کار می‌کند.

## ۲) امضای رسمی نسخهٔ انتشار (اختیاری)

بدون این مرحله، فایل `qarargah-kerman-1.0.0.apk` با امضای debug ساخته می‌شود که برای نصب مستقیم و تست کافی است، اما برای انتشار در Google Play یا به‌روزرسانی بدون حذف برنامه لازم است کلید ثابت داشته باشید.

```bash
keytool -genkeypair -v -keystore release.jks -alias qarargah \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks > release.jks.b64        # در ویندوز: certutil -encode release.jks r.txt
```

سپس **Settings ← Secrets and variables ← Actions**, این چهار Secret را بسازید:

| نام Secret | مقدار |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | محتوای فایل `release.jks.b64` |
| `ANDROID_KEYSTORE_PASSWORD` | رمز keystore |
| `ANDROID_KEY_ALIAS` | `qarargah` |
| `ANDROID_KEY_PASSWORD` | رمز کلید |

از آن پس هر بیلد، APK امضاشدهٔ رسمی می‌سازد. فایل `release.jks` را جای امن نگه دارید و **هرگز** در مخزن نگذارید.

## ۳) تنظیمات پیشنهادی مخزن

- **About** ← توضیح: `قرارگاه مردمی مدافعان وطن — محله کرمان | اپ اندروید + نسخه وب`
- **About ← Topics:** `android`, `pwa`, `kotlin`, `persian`, `webview`
- **Releases ← v1.0.0 ← Edit:** تیک *Set as the latest release* (اکنون فعال است)

## ۴) به‌روزرسانی نسخه (برای آینده)

```bash
# android/version.properties
VERSION_NAME=1.0.1
VERSION_CODE=2
```

با هر push روی `main`، Actions نسخهٔ جدید را می‌سازد و در Releases با برچسب `v1.0.1` منتشر می‌کند.
