// Store listing screenshots as a build artifact: `./gradlew screenshots` renders the real shared UI headlessly
// (Skia off-screen surface, no emulator, no display) onto exact store pixel sizes and writes the trees that
// fastlane consumes under build/store/. Deliberately not wired into check/test — it writes files, it asserts nothing.
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
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesCore)
    implementation(libs.kotlinx.ioCore)
}

tasks.register<JavaExec>("screenshots") {
    group = "publishing"
    description = "Deletes build/store and regenerates every store listing screenshot, headlessly."
    mainClass = "sk.ainet.examples.smarthome.screenshots.MainKt"
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("java.awt.headless", "true")
    systemProperty("user.timezone", "UTC")
    // A plain File keeps the task configuration-cache compatible.
    val storeDir = rootProject.layout.buildDirectory.dir("store").get().asFile
    args(storeDir.absolutePath)
    doFirst { storeDir.deleteRecursively() }
    outputs.upToDateWhen { false }
}
