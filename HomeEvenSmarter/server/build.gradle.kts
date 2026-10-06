
// Companion server run on the presenter's laptop: serves materialized cartridges to the phone over WiFi.
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
}

group = "sk.ainet.examples.smarthome"
version = "1.0.0"
application {
    mainClass = "sk.ainet.examples.smarthome.ApplicationKt"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serverPartialContent)
    implementation(libs.ktor.serializationKotlinxJson)
    // outbound calls of the tool backends (weather); same client stack the app modules use
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientMock)
    testImplementation(libs.kotlinx.coroutinesTest)
    testImplementation(libs.kotlin.testJunit)
}
