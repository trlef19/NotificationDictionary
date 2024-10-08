plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}

android {
    defaultConfig {
        applicationId = "com.xtreak.notificationdictionary"
        minSdk = 31
        targetSdk = 35
        compileSdk = 35
        versionCode = 23
        versionName = "0.0.23"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    namespace = "com.xtreak.notificationdictionary"
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("io.github.medyo:android-about-page:2.0.0")
    implementation("com.suddenh4x.ratingdialog:awesome-app-rating:2.7.0")
    implementation("com.huxq17.pump:download:1.3.10")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("de.cketti.library.changelog:ckchangelog:1.2.2")
    implementation("io.sentry:sentry-android:7.14.0")
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.2.3")
    implementation("com.mikepenz:aboutlibraries:11.2.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.runtime:runtime-android:1.6.8")
    implementation(platform("androidx.compose:compose-bom:2024.08.00"))
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.compose.material3:material3-android:1.2.1")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.6.8")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")

    val roomVersion = "2.6.1"

    implementation("androidx.room:room-runtime:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
