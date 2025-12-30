plugins {
    id("com.android.application")
}

android {
    namespace = "com.calculator.notepadapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.calculator.notepadapp"
        minSdk = 23
        targetSdk = 34
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
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.room.common.jvm)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Room 数据库
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Align Kotlin stdlib/transitive Kotlin deps used by AndroidX libraries
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.24"))

    // LiveData 和 ViewModel
    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")

    // SwipeRefreshLayout 下拉刷新
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // 添加这行来解决问题
    implementation("androidx.work:work-runtime:2.9.0")

    // Kotlin Metadata 版本对齐，避免编译期 Metadata 版本冲突
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    annotationProcessor("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
}
