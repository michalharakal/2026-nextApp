import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":app:shared"))
    implementation(project(":cartridges"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.kotlinx.ioCore)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "sk.ainet.examples.smarthome.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "sk.ainet.examples.smarthome"
            packageVersion = "1.0.0"
        }
    }
}
