import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Compose Multiplatform UI shared by the Android and desktop apps: the home, the pipeline visualizer and the
// cartridge status screen. The apps inject platform pieces (engines, microphone, settings) through AppEnvironment.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    android {
        namespace = "sk.ainet.examples.smarthome.app.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        androidResources { enable = true }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
        }
        commonMain.dependencies {
            api(project(":core"))
            api(project(":cartridges"))
            api(project(":app:design"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            // the compose plugin's paired material3 — pinning a different (alpha) version broke binary
            // compatibility with foundation on Android (AbstractMethodError in OutlinedTextFieldDefaults)
            implementation(compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
