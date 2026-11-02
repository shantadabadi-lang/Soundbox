plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "np.mero.soundbox"
    compileSdk = 34
    defaultConfig {
        applicationId = "np.mero.soundbox"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1-capture"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
