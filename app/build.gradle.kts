plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.calculator.notepadapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.calculator.notepadapp"
        minSdk = 21
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
        // Enable desugaring so we can use java.time APIs on older Android versions
        isCoreLibraryDesugaringEnabled = true
    }
}

// E:/AndroidProjects/app/build.gradle.kts

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Room 数据库
    implementation("androidx.room:room-runtime:2.6.0")
    annotationProcessor("androidx.room:room-compiler:2.6.0")

    // LiveData 和 ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    // 协程（LifecycleScope 需要）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")


    // SwipeRefreshLayout 下拉刷新
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // 添加这行来解决问题
    implementation("androidx.work:work-runtime:2.9.0")

    // Desugaring support for java.time APIs used in WorkManager configuration
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
