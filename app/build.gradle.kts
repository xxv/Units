import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release signing is configured out-of-band so that no secrets live in the
// repository. Create a keystore.properties in the project root (it is
// .gitignore'd) holding storeFile/storePassword/keyAlias/keyPassword, or set
// the equivalent UNITS_* environment variables on your build machine. Without
// either, the release build still works, but comes out unsigned.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "UNITS_KEYSTORE")

android {
    namespace = "info.staticfree.android.units"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "info.staticfree.android.units"

        // Android 6.0: the oldest release the AndroidX libraries below still
        // support. Nothing in the app itself needs anything newer.
        minSdk = 23

        // Google Play requires new releases to target a recent API level.
        targetSdk = 36

        versionCode = 10
        versionName = "1.1"
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingValue("storePassword", "UNITS_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "UNITS_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "UNITS_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    bundle {
        // Units ships ~20 translations and locale-specific unit-usage data, all
        // of it tiny. Keeping every language in the base APK means the app keeps
        // working when someone switches their device language after install.
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            // An editor backup file that was accidentally committed to
            // libs/andro-views.jar back in 2011, plus debug metadata that the
            // AndroidX transitive dependencies bring along.
            excludes += setOf(
                "**/.*.un~",
                "DebugProbesKt.bin",
                "META-INF/*.version",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false

        // The translations are contributed by volunteers and are always
        // partial; missing ones fall back to English, which is fine.
        disable += setOf(
            "MissingTranslation",
            "ExtraTranslation",
            "Untranslatable",
            // net.sourceforge.unitsinjava is vendored upstream; it is not
            // reformatted here so that it stays diffable against a new drop.
            "SuspiciousIndentation",
        )

        // Long-standing habits in this codebase that are worth knowing about
        // but aren't worth churning a decade-old app over.
        warning += setOf(
            "Range",             // Cursor.getColumnIndex() results used unchecked
            "RestrictedApi",     // dispatchKeyEvent() on AndroidX's ComponentActivity
            "StaticFieldLeak",
            "HandlerLeak",
            "UnusedResources",
        )
    }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.cursoradapter)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.loader)

    // Swipeable numeric keypad. Prebuilt jar; no upstream source release.
    implementation(files("libs/andro-views.jar"))
}
