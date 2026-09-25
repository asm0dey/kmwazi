plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.license)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.github.asm0dey.kmwazi.desktop.MainKt"
    }
}

ktlint { ignoreFailures.set(false) }

licenseHeader {
    filesToScan.setFrom(fileTree("src") { include("**/*.kt") })
    header(rootProject.file("HEADER").readText())
}

tasks.named("check") { dependsOn("applyLicenseHeader") }
