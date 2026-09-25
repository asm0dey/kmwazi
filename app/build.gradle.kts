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
    compileSdk = 37

    defaultConfig {
        applicationId = "com.github.asm0dey.kmwazi"
        minSdk = 23
        targetSdk = 37
        versionCode = 4
        versionName = "2.0.0"
    }

    buildTypes {
        release {
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
