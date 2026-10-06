package sk.ainet.examples.smarthome.screenshots

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import sk.ainet.examples.smarthome.AppViewModel
import sk.ainet.examples.smarthome.actions.HomeStore
import sk.ainet.examples.smarthome.ui.App
import java.io.File
import java.util.Locale
import java.util.TimeZone
import kotlin.system.exitProcess

/**
 * The matrix loop: every shot x locale x theme x device spec, rendered headlessly and written where the upload
 * tool expects it:
 *
 * ```
 * build/store/play/<locale>/images/<formFactor>/NN_name.png   ← fastlane supply
 * build/store/gallery/<locale>/NN_name.png                    ← hackathon/droidcon galleries
 * ```
 *
 * A dark variant (once the app has a second theme) inserts a `dark/` segment before the file name.
 * Run with `./gradlew screenshots`; the task deletes `build/store` first.
 */
fun main(args: Array<String>) {
    val outRoot = File(args.firstOrNull() ?: "build/store")
    TimeZone.setDefault(TimeZone.getTimeZone("UTC")) // timestamps rendered by the app must not depend on the host

    val vm = AppViewModel(DemoData.environment(), CoroutineScope(SupervisorJob() + Dispatchers.Default), HomeStore(DemoData.home))
    Thread.sleep(300) // let the constructor's (empty) cartridge rescan land before the demo state goes in
    DemoData.present(vm)

    var written = 0
    for (locale in StoreSpecs.locales) {
        Locale.setDefault(Locale.forLanguageTag(locale)) // the UI's own formatting must follow, not just the folder
        for (spec in StoreSpecs.devices) {
            for (theme in StoreSpecs.themes) {
                for (shot in Shots.all) {
                    if (spec.store in shot.skipStores) continue
                    val png = Renderer.render(spec) { App(vm, start = shot.destination) }
                    val file = outputFile(outRoot, spec, locale, theme, shot)
                    file.parentFile.mkdirs()
                    file.writeBytes(png)
                    written++
                    println("  ${file.toRelativeString(outRoot)}  ${spec.widthPx}x${spec.heightPx}px @${spec.density}x")
                }
            }
        }
    }
    println("$written screenshots → $outRoot")
    exitProcess(0)
}

private fun outputFile(root: File, spec: DeviceSpec, locale: String, theme: ShotTheme, shot: Shot): File {
    val storeDir = when (spec.store) {
        StoreTarget.PLAY -> File(root, "play/$locale/images/${spec.storeSubfolder}")
        StoreTarget.GALLERY -> File(root, "gallery/$locale")
    }
    val themed = if (theme.pathSegment.isEmpty()) storeDir else File(storeDir, theme.pathSegment)
    return File(themed, shot.fileName)
}
