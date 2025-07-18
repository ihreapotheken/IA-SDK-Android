plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "de.ihreapotheke.iasdkexample"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.ihreapotheke.iasdkexample"
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
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

    // IA SDK staging dependencies
    "stagingImplementation"(libs.iasdk.staging.integrations)
    "stagingImplementation"(libs.iasdk.staging.otc)
    "stagingImplementation"(libs.iasdk.staging.ordering)
    "stagingImplementation"(libs.iasdk.staging.pharmacy)
    "stagingImplementation"(libs.iasdk.staging.rx)

    // IA SDK prod dependencies
    "prodImplementation"(libs.iasdk.integrations)
    "prodImplementation"(libs.iasdk.otc)
    "prodImplementation"(libs.iasdk.ordering)
    "prodImplementation"(libs.iasdk.pharmacy)
    "prodImplementation"(libs.iasdk.rx)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
