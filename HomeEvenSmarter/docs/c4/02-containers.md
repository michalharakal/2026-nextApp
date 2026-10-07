# Level 2 — Containers

What actually runs, and what travels between the pieces. The Gradle modules `:core`, `:cartridges`,
`:app:shared` and `:app:design` are libraries compiled *into* the apps, so they appear at level 3,
not here.

```mermaid
C4Container
  title Containers — HomeEvenSmarter

  Person(resident, "Resident / Presenter")

  System_Boundary(device, "Device (phone, arm64 — the demo target)") {
    Container(android, "Android app", "Kotlin, Compose MP, IREE/Vulkan", "Mic → streaming ASR → NLU → typed call → home model + pipeline UI")
    ContainerDb(store, "Cartridge store", "app files dir, pack_dirs", "Signed model packs; every artifact digest-verified against the manifest")
  }

  System_Boundary(laptop, "Laptop (presenter's machine)") {
    Container(server, "Companion server", "Kotlin, Ktor/Netty", "Cartridge registry + remote-tool middleware; documents itself at /openapi.yaml")
    ContainerDb(packs, "Materialized cartridges", "build/cartridges pack_dirs", "Output of scripts/materialize.sh (blueprints + pinned IREE toolchain)")
    Container_Ext(control, "Control backend plug-in", "private sibling project, ServiceLoader", "Implements the ToolBackend SPI; drives the building-bus gateway")
    Container(desktop, "Desktop app", "Kotlin, Compose Desktop", "Rehearsal build: same shared UI on fake engines")
  }

  System_Ext(weather, "Weather API", "Open weather data, HTTPS")

  Rel(resident, android, "speaks, taps")
  Rel(android, server, "GET /cartridges/*  ·  POST /tools/…", "REST (WiFi or adb-reverse USB)")
  Rel(android, store, "loads engines from")
  Rel(server, packs, "scans, serves (Range-resumable)")
  Rel(server, control, "discovers via SPI, dispatches declared tools")
  Rel(server, weather, "get_weather", "HTTPS, key from env")
  Rel(resident, desktop, "fallback demo")
  Rel(desktop, server, "same REST API")
```

- The cartridge store's on-disk layout **is** the pack_dir layout, so the same packs arrive over
  REST (resumable, digest-checked) or over USB (`scripts/sideload-cartridges.sh`).
- The control backend is `runtimeOnly` and discovered at startup — absent, the server answers
  control tools with a soft `"no control backend"`.
