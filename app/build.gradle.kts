import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.alarm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.alarm"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // [핵심 수정] local.properties에서 읽어온 API 키의 양쪽 끝에 있을 수 있는 따옴표를 제거합니다.
        // 이렇게 하면 local.properties에 키를 "YOUR_KEY"로 저장하든 YOUR_KEY로 저장하든 모두 정상적으로 처리됩니다.
        var weatherApiKey = localProperties.getProperty("weather.api.key", "")
        if (weatherApiKey.startsWith("\"") && weatherApiKey.endsWith("\"")) {
            weatherApiKey = weatherApiKey.substring(1, weatherApiKey.length - 1)
        }
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")
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
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    val roomVersion = "2.6.1"

    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
