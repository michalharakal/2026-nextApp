package sk.ainet.examples.smarthome.screenshots

/** Where a rendered file is published; decides the output tree an upload tool consumes. */
enum class StoreTarget { PLAY, GALLERY }

/**
 * A store screenshot slot: exact pixels, plus the density that maps them back to a plausible device.
 * `widthPx / density x heightPx / density` is the logical size the layout sees, so the composition looks like a
 * real phone or tablet, never a stretched one.
 */
data class DeviceSpec(
    val id: String,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val store: StoreTarget,
    /** Form-factor folder inside the store tree (fastlane supply's `images/<formFactor>`); empty when the store has none. */
    val storeSubfolder: String = "",
)

/**
 * The theme axis of the matrix. This app ships exactly one theme (the dark projector palette in
 * `HomeEvenSmarterTheme`), so the axis has a single default entry — screenshots must never show UI the reviewed
 * binary does not contain. When a light theme lands, add `DARK("dark")` here and the default becomes the light set;
 * the path mapper already inserts [pathSegment] before the file name.
 */
enum class ShotTheme(val pathSegment: String) { DEFAULT("") }

/**
 * Store pixel rules, re-verified against current store documentation on 2026-10-06:
 *
 * - *Google Play*: screenshot aspect ratio must not exceed 2:1 — a raw capture from a modern 20:9 phone
 *   (1080x2400) is rejected. Rendered as exact 9:16 / 16:9 instead: phone 1440x2560 @3x (= 480x853 logical dp,
 *   a plausible phone) and 10" tablet 1620x2880 @2x (= 810x1440 logical dp).
 * - *App Store Connect* (6.9" 1320x2868 @3x, 6.5" 1284x2778 @3x): not rendered — the project currently ships no
 *   iOS target. Add the specs back with the iOS app.
 * - *Gallery* (hackathon/droidcon listing): the wide projector layout of the desktop app, 2560x1600 @2x
 *   (= 1280x800 logical dp, above the 840 dp breakpoint, so home and pipeline render side by side).
 */
object StoreSpecs {
    val devices: List<DeviceSpec> = listOf(
        DeviceSpec("play-phone", 1440, 2560, 3f, StoreTarget.PLAY, "phoneScreenshots"),
        DeviceSpec("play-tablet-10", 1620, 2880, 2f, StoreTarget.PLAY, "tenInchScreenshots"),
        DeviceSpec("gallery-wide", 2560, 1600, 2f, StoreTarget.GALLERY),
    )

    /**
     * Every store listing language. The app's strings are hard-coded source English today, so the matrix has one
     * locale; a new listing language first needs localized string resources in `app/shared`, then its tag here.
     * The renderer still pins the JVM default locale per run so locale-sensitive formatting cannot drift.
     */
    val locales: List<String> = listOf("en-US")

    val themes: List<ShotTheme> = ShotTheme.entries
}
