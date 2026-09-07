import java.util.Properties

// Load local.properties (never committed to VCS)
val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}
fun localProp(key: String, fallback: String = "") =
    (localProps.getProperty(key) ?: System.getenv(key.uppercase().replace('.', '_')) ?: fallback)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.family4.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.family4.app"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        // API keys loaded from local.properties (gitignored)
        val mapsKey    = localProp("MAPS_API_KEY")
        val geminiKey  = localProp("GEMINI_API_KEY")
        val weatherKey = localProp("WEATHER_API_KEY")

        manifestPlaceholders["MAPS_API_KEY"] = mapsKey

        buildConfigField("String", "MAPS_API_KEY",        "\"$mapsKey\"")
        buildConfigField("String", "GEMINI_API_KEY",      "\"$geminiKey\"")
        buildConfigField("String", "WEATHER_API_KEY",     "\"$weatherKey\"")
        buildConfigField("String", "DRIVE_OWNER_EMAIL",   "\"paulmmoore3416@gmail.com\"")
        buildConfigField("String", "FAMILY4_DRIVE_FOLDER","\"Family4_AppData\"")
        buildConfigField("String", "TURN_SERVER_URL",     "\"turn:openrelay.metered.ca:80\"")
        buildConfigField("String", "TURN_USERNAME",       "\"openrelayproject\"")
        buildConfigField("String", "TURN_CREDENTIAL",     "\"openrelayproject\"")
        buildConfigField("String", "STUN_SERVER_URL",     "\"stun:stun.l.google.com:19302\"")
        buildConfigField("String", "BLUELINK_CLIENT_ID",     "\"${localProp("BLUELINK_CLIENT_ID")}\"")
        buildConfigField("String", "BLUELINK_CLIENT_SECRET", "\"${localProp("BLUELINK_CLIENT_SECRET")}\"")

        // Room schema export
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
    }

    signingConfigs {
        create("release") {
            val ksPath   = localProp("KEYSTORE_PATH")
            val ksPass   = localProp("KEYSTORE_PASSWORD")
            val keyAlias = localProp("KEY_ALIAS")
            val keyPass  = localProp("KEY_PASSWORD")
            if (ksPath.isNotEmpty()) {
                storeFile     = file(ksPath)
                storePassword = ksPass
                this.keyAlias = keyAlias
                keyPassword   = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Signing applied when KEYSTORE_PATH is set in local.properties or CI.
            val ksPath = localProp("KEYSTORE_PATH")
            if (ksPath.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.gridlayout)

    // MultiDex
    implementation("androidx.multidex:multidex:2.0.1")

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.camera.extensions)

    // Maps & Location
    implementation(libs.google.maps)
    implementation(libs.google.location)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Image Loading
    implementation(libs.coil)
    implementation(libs.glide)
    kapt(libs.glide.compiler)

    // WorkManager + Hilt-Work integration
    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)

    // Security
    implementation(libs.biometric)
    implementation(libs.security.crypto)
    implementation(libs.bouncycastle)

    // DataStore
    implementation(libs.datastore.preferences)

    // Paging
    implementation(libs.paging)

    // Media
    implementation(libs.exoplayer)
    implementation(libs.exoplayer.ui)

    // Android Auto / Car App Library
    implementation(libs.car.app)
    implementation(libs.car.app.projected)

    // Animations
    implementation(libs.lottie)

    // Charts
    implementation(libs.mpandroidchart)

    // Permissions
    implementation(libs.permissionx)

    // Firebase (BOM keeps all versions in sync)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)

    // Google Sign-In & Drive API
    implementation(libs.google.auth)
    implementation(libs.google.drive)
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)

    // Google Generative AI (Gemini)
    implementation(libs.generativeai)

    // Splash screen
    implementation(libs.splashscreen)
}
