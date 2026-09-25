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

// Release signing comes only from the environment (CI decodes the keystore on tag builds);
// without KMWAZI_KEYSTORE the release build stays unsigned, as local and PR builds do.
val releaseKeystore = providers.environmentVariable("KMWAZI_KEYSTORE").orNull

android {
    namespace = "com.github.asm0dey.kmwazi"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.asm0dey.kmwazi"
        minSdk = 23
        targetSdk = 37
        versionCode = 4
        versionName = "2.0.0"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("KMWAZI_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("KMWAZI_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("KMWAZI_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = VERSION_11
        targetCompatibility = VERSION_11
    }
    buildFeatures { compose = true }
    lint { checkDependencies = true }
}

kotlin {
    compilerOptions { jvmTarget = JVM_11 }
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    reporters {
        reporter(PLAIN)
        reporter(CHECKSTYLE)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.activity.compose)
}

licenseHeader {
    filesToScan.setFrom(fileTree("src") { include("**/*.kt") })
    header(rootProject.file("HEADER").readText())
}

tasks.named("check") { dependsOn("applyLicenseHeader") }
