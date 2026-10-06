package sk.ainet.examples.smarthome.screenshots

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.io.files.Path
import sk.ainet.examples.smarthome.AppEnvironment
import sk.ainet.examples.smarthome.AppViewModel
import sk.ainet.examples.smarthome.CartridgesState
import sk.ainet.examples.smarthome.EnginesState
import sk.ainet.examples.smarthome.GoldenResult
import sk.ainet.examples.smarthome.RunView
import sk.ainet.examples.smarthome.StageStatus
import sk.ainet.examples.smarthome.actions.ActionResult
import sk.ainet.examples.smarthome.cartridge.CartridgeDescriptor
import sk.ainet.examples.smarthome.cartridge.CartridgeIndex
import sk.ainet.examples.smarthome.cartridge.CartridgeManifest
import sk.ainet.examples.smarthome.cartridges.CartridgeStore
import sk.ainet.examples.smarthome.cartridges.DevicePreference
import sk.ainet.examples.smarthome.cartridges.DownloadEvent
import sk.ainet.examples.smarthome.cartridges.EngineFactory
import sk.ainet.examples.smarthome.cartridges.InstalledCartridge
import sk.ainet.examples.smarthome.engine.AsrEngine
import sk.ainet.examples.smarthome.engine.AudioSource
import sk.ainet.examples.smarthome.engine.EngineStatus
import sk.ainet.examples.smarthome.engine.NluEngine
import sk.ainet.examples.smarthome.engine.NluOutcome
import sk.ainet.examples.smarthome.home.BlindPosition
import sk.ainet.examples.smarthome.home.DeviceRef
import sk.ainet.examples.smarthome.home.Door
import sk.ainet.examples.smarthome.home.HomeState
import sk.ainet.examples.smarthome.home.Light
import sk.ainet.examples.smarthome.home.Room
import sk.ainet.examples.smarthome.home.RoomState
import sk.ainet.examples.smarthome.home.Thermostat
import sk.ainet.examples.smarthome.pipeline.GoldenSet
import sk.ainet.examples.smarthome.pipeline.PipelineRun
import sk.ainet.examples.smarthome.pipeline.Stage

/**
 * The hand-picked, hard-coded state every shot renders. Names, timings and sizes are chosen to look real — the
 * engine latencies are the measured vulkan-arm64 numbers from `cartridges/profiles/` — and to exercise the layout
 * (lights on/off/dimmed, blinds in all positions, one lock locked, a download in flight). Nothing here depends on
 * the clock, the host or randomness, so two runs render identical pixels.
 */
object DemoData {
    private const val ASR_ID = "asr-moonshine-v2-streaming-iree.vulkan-arm64.en"
    private const val NLU_ID = "nlu-functiongemma-270m-iree.vulkan-arm64.home"
    private const val STORE_ROOT = "/data/user/0/sk.ainet.examples.smarthome/files/cartridges"

    /** The home as the rooms grid shows it. The living room matches the run below: just dimmed to 20 %. */
    val home: HomeState = HomeState(
        rooms = mapOf(
            Room.LIVING_ROOM to RoomState(Room.LIVING_ROOM, Light(on = true, brightness = 20), Thermostat(21.5), BlindPosition.HALF),
            Room.KITCHEN to RoomState(Room.KITCHEN, Light(on = true, brightness = 100), Thermostat(21.0), BlindPosition.OPEN),
            Room.BEDROOM to RoomState(Room.BEDROOM, Light(on = false, brightness = 60), Thermostat(19.0), BlindPosition.CLOSED),
            Room.BATHROOM to RoomState(Room.BATHROOM, Light(on = false, brightness = 80), Thermostat(22.0), BlindPosition.HALF),
            Room.HALLWAY to RoomState(Room.HALLWAY, Light(on = true, brightness = 40), Thermostat(20.0), BlindPosition.OPEN),
        ),
        locks = mapOf(Door.FRONT_DOOR to true, Door.BACK_DOOR to false),
    )

    /** One finished utterance, as the pipeline panel and strip draw it. */
    private val run = RunView(
        stages = mapOf(
            Stage.LISTENING to StageStatus.DONE,
            Stage.ASR to StageStatus.DONE,
            Stage.NLU to StageStatus.DONE,
            Stage.ACTION to StageStatus.DONE,
            Stage.CLOUD to StageStatus.PENDING,
        ),
        source = "mic",
        partial = "Dim the living room light to twenty percent",
        transcript = "Dim the living room light to twenty percent",
        audioMs = 2480,
        asrMs = 1175,
        outcome = NluOutcome.Call(
            name = "set_light",
            args = mapOf("room" to "living_room", "state" to "on", "brightness" to "20"),
            raw = "set_light(room=\"living_room\", state=\"on\", brightness=\"20\")",
            timing = NluOutcome.Timing(tokenizeMs = 18, restoreMs = 240, chunkMs = 410, decodeMs = 2187, decodeTokens = 14),
        ),
        nluMs = 2855,
        actionMessage = "Living room light on at 20%",
        actionOk = true,
        totalMs = 6540,
    )

    private val engines = EnginesState(
        fakes = false,
        asr = EngineStatus.Ready(warmUpMs = 3200, detail = "vulkan"),
        nlu = EngineStatus.Ready(warmUpMs = 9400, detail = "vulkan"),
        device = DevicePreference.AUTO,
        asrId = ASR_ID,
        nluId = NLU_ID,
    )

    private val cartridges = CartridgesState(
        installed = listOf(
            installed(ASR_ID, family = "moonshine-v2", task = "asr", weightsBytes = 54_120_000, auxBytes = 4_200_000),
            installed(NLU_ID, family = "functiongemma-270m", task = "nlu", weightsBytes = 291_300_000, auxBytes = 5_240_000),
        ),
        index = CartridgeIndex(
            cartridges = listOf(
                indexEntry(ASR_ID, "moonshine-v2", "asr", accelerator = "vulkan", languages = listOf("en"), sizeBytes = 58_320_000),
                indexEntry("asr-moonshine-v2-streaming-iree.cpu-arm64.en", "moonshine-v2", "asr", accelerator = null, languages = listOf("en"), sizeBytes = 58_280_000),
                indexEntry(NLU_ID, "functiongemma-270m", "nlu", accelerator = "vulkan", languages = emptyList(), sizeBytes = 296_540_000),
                indexEntry("nlu-functiongemma-270m-iree.cpu-arm64.home", "functiongemma-270m", "nlu", accelerator = null, languages = emptyList(), sizeBytes = 296_480_000),
            ),
        ),
        downloads = mapOf(
            "nlu-functiongemma-270m-iree.cpu-arm64.home" to DownloadEvent.Progress(
                cartridgeId = "nlu-functiongemma-270m-iree.cpu-arm64.home",
                file = "artifacts/weights.irpa",
                bytesDone = 183_500_800,
                bytesTotal = 296_480_000,
            ),
        ),
        verification = mapOf(ASR_ID to "verified 2 artifacts"),
    )

    private val goldenNluMs = listOf(2731L, 2802L, 2855L, 2914L, 3066L, 2688L, 2797L, 2650L, 2733L, 2901L, 3240L, 3187L)

    /** The rehearsed golden set, all green, with fixed per-case latencies. */
    private val golden: List<GoldenResult> = GoldenSet.cases.mapIndexed { i, case ->
        val nluMs = goldenNluMs[i % goldenNluMs.size]
        val outcome = case.expectedTool?.let { NluOutcome.Call(it, emptyMap(), raw = "", timing = NluOutcome.Timing.NONE) }
            ?: NluOutcome.NoCall("outside the home catalog", NluOutcome.Timing.NONE)
        GoldenResult(
            case,
            PipelineRun(
                transcript = case.utterance,
                outcome = outcome,
                action = case.expectedTool?.let { ActionResult(it, ok = true, message = "ok") },
                escalation = null,
                asrMs = 0,
                nluMs = nluMs,
                totalMs = nluMs,
            ),
        )
    }

    private val log = listOf(
        "   4s  server lists 4 cartridges",
        "  31s  downloaded $ASR_ID (58 MB)",
        "  96s  downloaded $NLU_ID (296 MB)",
        " 103s  asr ready in 3200 ms ($ASR_ID)",
        " 112s  nlu ready in 9400 ms ($NLU_ID)",
        " 131s  \"Lock the front door\" → set_lock → Front door locked [asr 980 ms, nlu 2610 ms]",
        " 150s  \"Dim the living room light to twenty percent\" → set_light → Living room light on at 20% [asr 1175 ms, nlu 2855 ms]",
    )

    /** The environment of the shots: an Android-looking store path, engines present, a (silent) microphone. */
    fun environment(): AppEnvironment = AppEnvironment(
        platform = "android",
        store = CartridgeStore(Path(STORE_ROOT)),
        engineFactory = DemoEngineFactory,
        audioSource = SilentAudioSource,
        settings = AppEnvironment.MemorySettings(serverUrl = "http://192.168.1.23:8080"),
        startWithFakes = false,
    )

    fun present(vm: AppViewModel) {
        vm.presentDemo(
            engines = engines,
            cartridges = cartridges,
            run = run,
            log = log,
            golden = golden,
            recentlyChanged = setOf(DeviceRef.LightOf(Room.LIVING_ROOM)),
        )
    }

    private fun installed(id: String, family: String, task: String, weightsBytes: Long, auxBytes: Long): InstalledCartridge =
        InstalledCartridge(
            dir = Path(STORE_ROOT, id),
            descriptor = CartridgeDescriptor(
                id = id, version = "0.1.0", family = family, task = task,
                target = CartridgeDescriptor.Target(hardware = "arm64", abi = "arm64-v8a", accelerator = "vulkan"),
            ),
            manifest = CartridgeManifest(
                id = id, version = "0.1.0",
                artifacts = listOf(
                    CartridgeManifest.Artifact("weights", "artifacts/weights.irpa", digest = "0", size = weightsBytes),
                    CartridgeManifest.Artifact("aux", "artifacts/aux.bin", digest = "0", size = auxBytes),
                ),
            ),
        )

    private fun indexEntry(id: String, family: String, task: String, accelerator: String?, languages: List<String>, sizeBytes: Long): CartridgeIndex.Entry =
        CartridgeIndex.Entry(
            id = id, version = "0.1.0", family = family, task = task, abi = "arm64-v8a",
            accelerator = accelerator, languages = languages, sizeBytes = sizeBytes,
            files = listOf(
                CartridgeIndex.Entry.File("descriptor.json", 980),
                CartridgeIndex.Entry.File("manifest.json", 1240),
                CartridgeIndex.Entry.File("artifacts/weights.irpa", sizeBytes - 2220),
            ),
        )

    /** Marks the platform as having a runtime; never asked to build one — the shots show finished state only. */
    private object DemoEngineFactory : EngineFactory {
        override val platform: String = "android"
        override val available: Boolean = true
        override fun asr(cartridge: InstalledCartridge, language: String, device: DevicePreference): AsrEngine = error("demo only")
        override fun nlu(cartridge: InstalledCartridge, device: DevicePreference): NluEngine = error("demo only")
    }

    /** Makes the talk button render enabled ("hold to talk"); never read. */
    private object SilentAudioSource : AudioSource {
        override fun frames(): Flow<FloatArray> = emptyFlow()
    }
}
