import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.license)
}

android {
    namespace = "com.github.asm0dey.kmwazi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.asm0dey.kmwazi"
        minSdk = 23
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = VERSION_11
        targetCompatibility = VERSION_11
    }

    buildFeatures {
        compose = true
    }

    lint {
        enable += setOf("ComposeUnstableReceiver", "ComposeModifierMissing")
        checkDependencies = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JVM_11
    }
}

ktlint {
    android.set(true)
    ignoreFailures.set(true)
    reporters {
        reporter(PLAIN)
        reporter(CHECKSTYLE)
    }
    filter {
        exclude { entry ->
            entry.file.toString().contains("generated")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Material Design 3
    implementation(libs.material3)

    // Android Studio Preview support
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)

    // Optional - Icons
    implementation(libs.material.icons.core)
    implementation(libs.material.icons.extended)

    // Optional - window size utils
    implementation(libs.adaptive)

    // Integration with activities
    implementation(libs.activity.compose)
    // Integration with ViewModels
    implementation(libs.lifecycle.viewmodel.compose)

    // Navigation-Compose
    implementation(libs.navigation.compose)

    // DataStore Preferences
    implementation(libs.datastore.preferences)
}

licenseHeader {
    filesToScan.setFrom(
        fileTree("src") {
            include("**/*.kt")
            include("**/*.java")
        },
    )
    header(rootProject.file("HEADER").readText())
}
