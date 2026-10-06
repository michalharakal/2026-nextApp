import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The SKaiNET look as a standalone module: brand color tokens (sampled from the project logo), the Material
// theme built on them, and the logo itself as a compose resource. UI modules depend on this and nothing else
// for theming, so the look is swappable and reusable by other SKaiNET sample apps.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    android {
        namespace = "sk.ainet.examples.smarthome.app.design"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        androidResources { enable = true }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(compose.material3)
            api(libs.compose.ui)
            implementation(libs.compose.components.resources)
        }
    }
}
