import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.license)
}

kotlin {
    android {
        namespace = "com.github.asm0dey.kmwazi.shared"
        compileSdk = 37
        minSdk = 23
        androidResources.enable = true
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.lifecycle.viewmodel.compose)
            api(libs.datastore.preferences.core)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.navigationevent.compose)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.okio)
        }
        named("desktopTest") {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.property)
                implementation(libs.archunit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.compose.ui.test)
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "com.github.asm0dey.kmwazi.resources"
}

tasks.named<Test>("desktopTest") {
    useJUnitPlatform()
}

ktlint {
    ignoreFailures.set(false)
    reporters {
        reporter(PLAIN)
        reporter(CHECKSTYLE)
    }
    filter {
        exclude { it.file.path.contains("generated") }
    }
}

licenseHeader {
    filesToScan.setFrom(fileTree("src") { include("**/*.kt") })
    header(rootProject.file("HEADER").readText())
}

tasks.named("check") { dependsOn("applyLicenseHeader") }
