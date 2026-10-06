# Hybrid: companion middleware, remote tools, live control state

The next step after the on-device demo: the phone's NLU emits one tool that needs live data
(`get_weather`), and the companion server grows into a generic middleware that executes *remote
tools* — the weather sample in this repository, and a building-bus control backend that lives in a
private sibling project. The app only ever speaks the companion REST API; it runs fully without a
companion, and the companion runs fully without a control backend.

Work packages continue the commit convention `<ID>: summary`. Groups A–G are taken; this plan owns
H–L. One checkbox = one commit. Tasks marked *(private)* are tracked here but executed in the
control-backend sibling project.

## H — catalog v3 + domain

- [ ] H1: suspend Handler and ActionRouter.dispatch
      `core/.../actions/Actions.kt`, `ActionRouterTest` — remote tools do network I/O; the only
      dispatch call site (`VoicePipeline.resolveAndAct`) is already suspend.
- [ ] H2: tool catalog v3 adds get_weather
      `cartridges/catalogs/home-tools.v1.json` in place (id → `home-tools.v3`): args `when`
      ∈ now|today|tomorrow, optional `place`, nothing required.
- [ ] H3: HomeTools mirror, home/remote split, contract test
      `HomeTools.GET_WEATHER` + `homeCommandNames`; `HomeActions.handlers()` registers only home
      commands; `CatalogContractTest` round-trips home commands only.
- [ ] H4: keyword NLU resolves weather
      Weather branch in `KeywordNluEngine` *before* the status rule; `VoicePipelineTest` escalation
      case switches to an out-of-catalog utterance.
- [ ] H5: golden set expects get_weather; remote-aware scoring
      `GoldenCase.remote` flag — a remote tool's NLU hit passes even when no companion is reachable
      (the golden set separates understanding errors from infrastructure).

## I — companion middleware

- [ ] I1: remote-tool wire types and backend SPI in :core
      `remote/RemoteTools.kt`: ToolCall/ToolResult/ToolDescriptor/ToolIndex, DeviceState/
      ControlState, `ToolBackend` + `ToolBackendProvider` (ServiceLoader SPI); JSON round-trip test.
- [ ] I2: tool registry and REST routes on the companion server
      `GET /tools`, `POST /tools/{name}` (unknown → 404; home command without backend → ok=false
      "no control backend"); ServiceLoader discovery in `main()`.
- [ ] I3: weather backend with env-configured API key
      OpenWeatherMap sample backend: `OPENWEATHER_API_KEY`, `HOME_LAT`, `HOME_LON`; injectable
      engine, MockEngine tests; no key → backend simply absent.
- [ ] I4: reactively aggregated control state endpoint
      Compose-runtime (molecule) presenter stitches every backend's device-state flow into one
      `ControlState` StateFlow; `GET /control/state`.
- [ ] I5: (optional) control state push channel
      SSE `GET /control/events`; app subscription deferred.

## J — app integration

- [ ] J1: companion tool client in :cartridges
      `CompanionToolClient` beside `CartridgeDownloader`: short timeout, injectable engine,
      `tools()` / `call()` / `controlState()`.
- [ ] J2: remote handlers registered on the action router
      `AppViewModel.rebuildPipeline()` registers `get_weather` → companion call; unreachable
      companion → failed action → the existing escalation path reports it.
- [ ] J3: companion reachability in the log; cloud stage lights up
      `fetchIndex()` also lists the companion's tools; a remote tool's successful action marks the
      cloud stage done in the pipeline panel.

## K — control backend (private sibling; K5 in this repo)

- [ ] K1: sibling project skeleton on the bus library *(private)*
      Depends only on this repo's `:core` SPI and the bus library composite; JVM 21.
- [ ] K2: device registry per the device model *(private)*
      JSON registry (device id, room, capabilities, command/state mappings), path via
      `CONTROL_DEVICES_FILE`; validation tests.
- [ ] K3: command execution with distinct failure reasons *(private)*
      Exact device match, normalized brightness, bounded retry, readback; failures distinguish
      gateway unreachable / tunnel unavailable / timeout / readback mismatch.
- [ ] K4: device state flow for the aggregator *(private)*
      Group observations stitched into `deviceStates()`, gated by connection state; gateway endpoint
      via `CONTROL_GATEWAY_HOST` / `CONTROL_GATEWAY_PORT`.
- [ ] K5: optional composite wiring; absent sibling stays green
      Conditional `includeBuild` behind `-PcontrolBackendDir=`; `runtimeOnly` dependency on the
      server under the same gate; a clean checkout builds without it.
- [ ] K6: installation smoke checklist *(private)*
      Switch a light over REST, watch `/control/state` reflect the readback, unplug drill.

## L — docs + materialization + verification

- [x] L1: this plan
- [ ] L2: architecture doc: companion middleware section
- [ ] L3: README: companion environment variables
      `OPENWEATHER_API_KEY`, `HOME_LAT`/`HOME_LON`, `CONTROL_DEVICES_FILE`,
      `CONTROL_GATEWAY_HOST`/`PORT`, `-PcontrolBackendDir=` — and the promise that everything runs
      without any of them.
- [ ] L4: NLU cartridge re-materialized with catalog v3
      Profile `cartridge.version` → 0.2.0; `scripts/materialize.sh nlu both`; measured numbers
      re-captured on device, honestly.
- [ ] L5: end-to-end verification pass
      Server curl matrix, desktop demo on fakes with and without companion, golden set, no-sibling
      build check; demo script line in `docs/DEMO.md`.

## Risks

- The 270M NLU's accuracy may dip when the catalog grows to seven functions — re-measure and update
  the profiles rather than assuming.
- The Compose-runtime dependency on the server is new; if the compiler plugin fights the server
  build, the same `StateFlow` seam can be fed by a plain `combine` until it is resolved.
- The weather provider's REST paths are pinned as constants so an upstream API change is one line.
