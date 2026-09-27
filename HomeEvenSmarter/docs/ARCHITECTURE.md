# Architecture

## One utterance

```
AudioSource (16 kHz mono float, 80 ms frames)
   │  push-to-talk: the frame flow completes when the button is released (Endpointer can end it on silence)
   ▼
AsrEngine.open() ── feed(frame) → partial? ── finish() → final transcript          Moonshine v2 streaming, IREE
   ▼
NluEngine.resolve(transcript, budget) → Call(name, args) | NoCall | Failed          FunctionGemma-270M, IREE, KV prefix cache
   ▼
ActionRouter.dispatch(Intent) → IntentMapper (entities) → HomeReducer → HomeStore   pure Kotlin
   │                                                                    ▲
   └─ NoCall / Failed / rejected ─► IntentEscalation (seam; None today) ┘           the hybrid step of the next talk
```

Every step publishes a `PipelineEvent`; the pipeline panel renders them (stage chips, partials, timing bars).

## Modules

- `:core` — everything that does not need a platform: `home/` (state, commands, reducer), `tools/` (catalog mirror,
  `IntentMapper`), `actions/` (router, `HomeActions`, `HomeStore`), `engine/` (contracts), `pipeline/`
  (`VoicePipeline`, `Endpointer`, `GoldenSet`), `hybrid/` (escalation seam), `fakes/`, `cartridge/` (descriptor,
  manifest, index, SHA-256 verifier).
- `:cartridges` — `CartridgeStore` (pack_dirs on disk), `CartridgeDownloader` (companion server → disk, resumable,
  digest-checked), `EngineFactory` (per platform). `androidMain` adapts the blueprint modules
  `sk.ainet.cartridge.asr.moonshine` and `sk.ainet.cartridge.nlu.functiongemma` to the engine contracts.
- `:app:shared` — Compose UI and `AppViewModel`; `AppEnvironment` is what a platform injects.
- `:app:androidApp` — `AudioRecord` microphone, permission, `Application`-scoped view model (engines survive rotation).
- `:app:desktopApp` — javax.sound microphone, fake engines (stage 2 adds a JVM runtime).
- `:server` — Ktor: `/cartridges/index.json` + static pack_dir files.

## Contracts mirrored from the cartridges

| App (`:core`) | Cartridge module | Note |
|---|---|---|
| `AsrEngine` / `AsrSession` | `StreamingAsrCartridge` / `StreamingAsrSession` | same `feed` / `finish` / `reset` |
| `NluEngine` / `NluOutcome` | `NluToolCallCartridge` / `NluResolution` | same `Call` / `NoCall` / `Failed` and `Timing` |
| `CartridgeDescriptor`, `CartridgeManifest` | spec `descriptor.json`, `manifest.json` | subset the app reads |

The mirror keeps `:core` free of the cartridge dependency so it runs in JVM tests with fakes and on platforms
without a runtime; the adapters in `:cartridges/androidMain` are a few lines each.

## The tool catalog is the contract

`cartridges/catalogs/home-tools.v1.json` is rendered into the language model's prompt at materialization; the app
mirrors its names in `HomeTools`, and `CatalogContractTest` fails when the two drift. Six functions, all-string
arguments with closed value sets where possible: a 270M model degrades quickly with larger catalogs.

## Where the cartridges live

`<app files>/cartridges/<cartridge id>/` — exactly the pack_dir a materialization writes, so the same directory can
come from the companion server or from `adb push`. The app verifies every artifact's SHA-256 against the manifest.
