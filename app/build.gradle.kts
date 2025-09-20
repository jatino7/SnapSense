plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.o7solutions.snapsense"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.o7solutions.snapsense"
        minSdk = 26
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.gridlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


//    OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


//    cameraX
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

//    lottie
    implementation("com.airbnb.android:lottie:6.4.0")// latest version


//    Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")


//    mlkit
    implementation("com.google.mlkit:barcode-scanning:17.3.0") // latest version

//    cloudinary
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.MikeOrtiz:TouchImageView:3.6")


    implementation("com.intuit.sdp:sdp-android:1.1.1")   // for scalable dp
    implementation("com.intuit.ssp:ssp-android:1.1.1")   // for scalable sp

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    //    firebase
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")


}

