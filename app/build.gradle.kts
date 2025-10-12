plugins {
    alias(libs.plugins.android.application)
    // KSP 플러그인 라인 제거
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
    // Java 프로젝트에서 Room 사용 시 dataBinding 또는 viewBinding 관련 설정이 필요할 수 있으나,
    // 현재는 Room 어노테이션 프로세서만 집중합니다.
}

dependencies {
    val roomVersion = "2.6.1" // ROOM 라이브러리 버전 정의

    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion") // Java 프로젝트에서는 이것을 사용
    // ksp("androidx.room:room-compiler:$roomVersion") // Kotlin 및 KSP 사용 시 -> 주석 처리 또는 삭제
    implementation("androidx.room:room-ktx:$roomVersion") // Kotlin Coroutines 및 Flow 지원 (Java에서도 LiveData 사용 시 유용)

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
