plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.browxer.macrobenchmark"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    // The app under test — variants are matched with :app (debug/release).
    targetProjectPath = ":app"

    // Lets the benchmark APK instrument itself so release-style
    // (non-debuggable) builds can also be measured.
    experimentalProperties["android.experimental.self-instrumenting-macrobenchmark"] = "true"
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.uiautomator)
}
