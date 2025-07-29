import java.util.Properties

val keystoreFile = System.getenv("KEYSTORE_PATH")
val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
val keyAlias = System.getenv("KEY_ALIAS")
val keyPassword = System.getenv("KEY_PASSWORD")

val localKeystorePropertiesFile = File(rootDir, "keystore.properties")
val localKeystoreProperties =
    Properties().apply {
        if (localKeystorePropertiesFile.exists()) {
            load(localKeystorePropertiesFile.inputStream())
        }
    }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.jpleatherland.peakform"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.jpleatherland.peakform"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        disable += "NullSafeMutableLiveData"
    }
    signingConfigs {
//        create("debugRelease") {
//            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
//            storePassword = "android"
//            keyAlias = "androiddebugkey"
//            keyPassword = "android"
//        }
        create("release") {
            storeFile = file(keystoreFile ?: localKeystoreProperties["storeFile"] as String)
            storePassword = keystorePassword ?: localKeystoreProperties["storePassword"] as String
            keyAlias = keyAlias ?: localKeystoreProperties["keyAlias"] as String
            keyPassword = keyPassword ?: localKeystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
//        getByName("debugRelease") {
//            signingConfig = signingConfigs.getByName("debugRelease")
//        }
    }
}

ktlint {
    version.set("1.6.0")
    android.set(true)
    enableExperimentalRules.set(true)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation)
    implementation(libs.androidx.datastore)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    implementation(libs.mpandroidchart)

    implementation(libs.health.connect)
    implementation(libs.androidx.ui.text.google.fonts)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    ksp(libs.room.compiler)
}
