# Level 3 — Components: the app

One utterance, module by module. Everything below is shared Kotlin Multiplatform code; the Android
and desktop apps only inject platform pieces (engine factory, microphone, settings) through
`AppEnvironment`.

```mermaid
C4Component
  title Components — app (modules :app:shared, :core, :cartridges, :app:design)

  Container_Boundary(appb, "HomeEvenSmarter app") {
    Component(ui, "Compose UI", ":app:shared ui/ + :app:design", "Home grid, pipeline panel, cartridges screen; SKaiNET theme tokens")
    Component(vm, "AppViewModel", ":app:shared", "Single owner of app state: engines, cartridges, runs, golden set")
    Component(pipeline, "VoicePipeline", ":core pipeline/", "State machine: listening → ASR → NLU → act; emits the events the panel draws")
    Component(engines, "Engine contracts + fakes", ":core engine/, fakes/", "AsrEngine / NluEngine; scripted fakes so the UI runs anywhere")
    Component(factory, "EngineFactory", ":cartridges (per platform)", "Builds real engines from installed packs (Android: IREE runtime bindings)")
    Component(router, "ActionRouter + HomeActions", ":core actions/, tools/", "Typed call → IntentMapper → HomeReducer; suspend handlers")
    Component(home, "Home model", ":core home/", "Rooms, lights, locks; pure reducer, trivially testable")
    Component(remote, "CompanionToolClient", ":cartridges", "POST /tools/…; short timeouts, soft failures")
    Component(cstore, "CartridgeStore + Downloader", ":cartridges", "Pack scan, resume-capable download, digest verification")
  }

  Container_Ext(companion, "Companion server", "Ktor", "registry + remote tools")

  Rel(ui, vm, "state in, intents out")
  Rel(vm, pipeline, "runs utterances / typed text")
  Rel(pipeline, engines, "feed frames, resolve transcript")
  Rel(factory, engines, "provides real implementations")
  Rel(cstore, factory, "installed packs")
  Rel(pipeline, router, "dispatch typed call")
  Rel(router, home, "home commands (local, offline)")
  Rel(router, remote, "remote tools (get_weather)")
  Rel(remote, companion, "REST")
  Rel(cstore, companion, "download packs")
```

- The **escalation seam** (`:core hybrid/`) hangs off the pipeline: a failed or unanswerable call
  lands there instead of crashing; today's remote tools make the first rung real.
- Swapping fakes for real engines changes **no line** of UI or pipeline code — only what
  `EngineFactory` returns.
