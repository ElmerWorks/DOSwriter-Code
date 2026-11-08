import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.doswriter"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.doswriter"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Custom task to copy and label APKs in standard output locations
tasks.register("renameAllApks") {
    doLast {
        val dateStr = SimpleDateFormat("MMMddyy", Locale.US).format(Date()).uppercase(Locale.US)
        val releaseDir = File(projectDir, "release")
        if (!releaseDir.exists()) releaseDir.mkdirs()

        val apkOutputDir = file("${layout.buildDirectory.get().asFile}/outputs/apk/release")
        if (apkOutputDir.exists()) {
            apkOutputDir.listFiles()?.forEach { file ->
                if (file.extension == "apk") {
                    val newName = "DWTEv0.9-$dateStr.apk"
                    val destFile = File(releaseDir, newName)
                    file.copyTo(destFile, overwrite = true)
                    println("Copied release APK to ${destFile.absolutePath}")
                }
            }
        }
    }
}

tasks.whenTaskAdded {
    if (name.startsWith("assemble") || name.startsWith("bundle")) {
        finalizedBy("renameAllApks")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
