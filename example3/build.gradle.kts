import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun getAppSdkAccessKey(): String {
    // Try to load from .env file first
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        val properties = Properties()
        envFile.inputStream().use { properties.load(it) }
        val key = properties.getProperty("APPSDK_ACCESS_KEY")
        if (!key.isNullOrBlank()) {
            return key
        }
    }

    // Fall back to environment variable (GitHub Actions secret)
    val envKey = System.getenv("APPSDK_ACCESS_KEY")
    if (!envKey.isNullOrBlank()) {
        return envKey
    }

    // Neither .env file nor environment variable found
    throw GradleException(
        "APPSDK_ACCESS_KEY not found. Please provide it via:\n" +
        "  1. A .env file in the project root with APPSDK_ACCESS_KEY=<your-key>\n" +
        "  2. An environment variable APPSDK_ACCESS_KEY (used by GitHub Actions)"
    )
}

android {
    namespace = "de.ihreapotheke.iasdkexample"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.ihreapotheke.iasdkexample3"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "APPSDK_ACCESS_KEY",
            "\"${getAppSdkAccessKey()}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    flavorDimensions += "env"

    productFlavors {
        create("staging") {
            dimension = "env"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }
        create("prod") {
            dimension = "env"
        }
    }

    kotlin.compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
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
    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    // IA SDK dependencies
    implementation(libs.bundles.iasdk.example3)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
