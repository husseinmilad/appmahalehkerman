import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * نسخه از فایل version.properties خوانده می‌شود تا هم اپ اندروید و هم وب یکسان بمانند.
 */
val appVersion: Properties by lazy {
    Properties().apply {
        rootProject.file("version.properties").inputStream().use { load(it) }
    }
}
val versionNameValue = appVersion.getProperty("VERSION_NAME", "1.0.0")
val versionCodeValue = appVersion.getProperty("VERSION_CODE", "1").trim().toInt()

/** آدرسی که داخل اپ بارگذاری می‌شود. با -PappUrl=... قابل تغییر است. */
val appUrl: String = (project.findProperty("appUrl") as String?) ?: "https://husseinmilad.github.io/appmahalehkerman/"

/**
 * فایل index.html همان برنامهٔ وب است که در ریشهٔ مخزن زندگی می‌کند (پوشهٔ docs/).
 * هنگام بیلد داخل assets کپی می‌شود تا اپ حتی بدون اینترنت هم باز شود.
 */
val webRoot: File = rootProject.file("../docs")
val offlineBundleDir: File = layout.buildDirectory.dir("generated/offlineBundle").get().asFile

val prepareOfflineBundle by tasks.registering(Sync::class) {
    group = "build"
    description = "کپی کردن برنامهٔ وب داخل assets برای اجرای آفلاین"
    from(webRoot) {
        include("index.html", "offline.html", "manifest.webmanifest")
        into("web")
    }
    from(webRoot.resolve("icons")) {
        include("icon-192.png", "icon-512.png", "maskable-512.png", "apple-touch-icon.png")
        into("web/icons")
    }
    into(offlineBundleDir)
}

android {
    namespace = "ir.qarargah.kerman"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.qarargah.kerman"
        minSdk = 23
        targetSdk = 35
        versionCode = versionCodeValue
        versionName = versionNameValue
        resourceConfigurations += listOf("fa", "en")
        buildConfigField("String", "APP_URL", "\"$appUrl\"")
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    signingConfigs {
        create("release") {
            // امضای نسخهٔ انتشار از Secrets گیت‌هاب خوانده می‌شود.
            val keyStorePath = System.getenv("KEYSTORE_PATH")
            if (!keyStorePath.isNullOrBlank() && file(keyStorePath).exists()) {
                storeFile = file(keyStorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile?.exists() == true) releaseSigning else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf("META-INF/*.kotlin_module", "DebugProbesKt.bin")
    }

    androidResources {
        noCompress += listOf("html", "js", "webmanifest")
    }

    sourceSets["main"].assets.srcDir(offlineBundleDir)

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = false
    }
}

tasks.named("preBuild") { dependsOn(prepareOfflineBundle) }
tasks.withType<com.android.build.gradle.tasks.MergeSourceSetFolders>().configureEach {
    dependsOn(prepareOfflineBundle)
}
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(prepareOfflineBundle)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
