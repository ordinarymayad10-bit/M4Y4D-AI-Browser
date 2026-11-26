android {
    namespace = "com.m4yad.browser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.m4yad.browser"
        minSdk = 24
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
