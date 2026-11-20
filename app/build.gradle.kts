plugins { id("com.android.application") }

android {
    namespace = "com.deepcell.game"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.deepcell.game"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
}

dependencies {
    // Only the standard AndroidX libraries a new Android Studio project includes.
    // appcompat: AppCompatActivity and the theme. core: NotificationCompat (Module 8).
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("androidx.core:core-ktx:1.19.1")
}
