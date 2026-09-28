# HomeEvenSmarter

A small smart-home app that listens. Say *"turn on the kitchen light"* and the light in the kitchen turns on —
with **both models running on the phone**: Moonshine v2 streaming speech recognition and a FunctionGemma-270M
language model that turns the sentence into a function call. The app is the companion demo of the talk
*From Dumb Client to Hybrid Intelligence* (next.app devcon 2026, Berlin).

It exists to show one thing: how little app code an advanced on-device AI pipeline needs when the models are
packaged as **cartridges** built from public **blueprints** with [SKaiNET](https://github.com/SKaiNET-developers/SKaiNET)
(Kotlin DSL → StableHLO → IREE).

```
mic ─► Moonshine v2 (streaming ASR, IREE) ─► "close the living room blinds"
      ─► FunctionGemma-270M (NLU, IREE, KV-cache) ─► set_blinds(room="living_room", position="closed")
      ─► home model ─► the blinds in the UI close
```

The UI shows the pipeline while it runs: partial transcripts, the exact function call, per-stage timings, and a
greyed-out "cloud" stage that the next talk fills in (hybrid escalation for what the local model cannot answer).

## No weights in this repository

Nothing model-related is committed. The two cartridges are *materialized* from their public blueprints in
[SKaiNET-cartridge-blueprints](https://github.com/SKaiNET-developers/SKaiNET-cartridge-blueprints):

| Cartridge | Blueprint | Weights (downloaded by the blueprint) | Size on device |
|---|---|---|---|
| speech → text, English | `asr-moonshine-v2-streaming-iree` | `moonshine-ai/moonshine-streaming-tiny` (MIT) | ~180 MB |
| text → function call | `nlu-functiongemma-270m-iree` | `unsloth/functiongemma-270m-it-GGUF` (Gemma Terms of Use) | ~1.7 GB |

This repository holds the *inputs* to that materialization: the home tool catalog
(`cartridges/catalogs/home-tools.v1.json`) and the materialization profiles (`cartridges/profiles/`).
The FunctionGemma profile records acceptance of the [Gemma Terms of Use](https://ai.google.dev/gemma/terms);
whoever materializes the cartridge accepts them — edit `accepted_by` before you build.

## Build the cartridges

Prerequisites: JDK 21, Docker, a sibling checkout of `SKaiNET-cartridge-blueprints` (`../../` relative to this
directory, or `BLUEPRINTS_DIR=…`), and an Ed25519 signing key:

```
openssl genpkey -algorithm ed25519 -out ~/keys/homeevensmarter-dev.pem
export CARTRIDGE_SIGNING_KEY="$(cat ~/keys/homeevensmarter-dev.pem)"
scripts/materialize.sh all vulkan-arm64        # or: asr|nlu, vulkan-arm64|cpu-arm64|both
```

The IREE tools image (`skainet/iree-compiler:3.11.0`) is built on first use from
[SKaiNET-IREE-tools](https://github.com/SKaiNET-developers/SKaiNET-IREE-tools). Results land in
`build/cartridges/<cartridge id>/` as plain pack_dirs (`descriptor.json`, signed `manifest.json`, `artifacts/`).

## Get them onto the phone

Run the companion server on the laptop and let the app pull the cartridges over WiFi:

```
scripts/serve-cartridges.sh                     # prints the URL to type into the app's Cartridges screen
```

The app downloads every file, verifies each SHA-256 against the signed manifest, then loads the engines
("Load & warm up" — the language model prefills its catalog once, which takes a while). `scripts/sideload-cartridges.sh`
pushes the same pack_dirs with adb if there is no network.

## Run

- Android (the demo target, arm64): `./gradlew :app:androidApp:installDebug`
- Desktop (rehearsal build; same UI on built-in fake engines, real ones are the next stage): `./gradlew :app:desktopApp:run`
- Companion server: `./gradlew :server:run` (or `scripts/serve-cartridges.sh`)
- Tests: `./gradlew :core:jvmTest :cartridges:jvmTest :server:test`

The app starts on **fake engines** (scripted partials, keyword rules) so the whole UI works anywhere; switch them off
on the Cartridges screen once the real cartridges are loaded.

## Layout

| Module | What |
|---|---|
| `:core` | the home model, the tool → command mapping, the voice pipeline, engine contracts, fakes, manifest verification — pure Kotlin |
| `:cartridges` | cartridge store and downloader; Android adapters over the blueprint modules' APIs (`sk.ainet.cartridge.*`) |
| `:app:shared` | Compose Multiplatform UI: home, pipeline visualizer, cartridges |
| `:app:androidApp`, `:app:desktopApp` | platform entry points: microphone, storage, settings |
| `:server` | the companion server that serves materialized cartridges |
| `cartridges/catalogs`, `cartridges/profiles`, `scripts/` | materialization inputs and the scripts around them |

More in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/DEMO.md](docs/DEMO.md).

## License

MIT for the code in this repository. The models keep their own licenses: Moonshine is MIT, FunctionGemma is under
the Gemma Terms of Use (see below). The blueprints record the license per source, and the materialization writes
the effective license into every cartridge manifest.

## FunctionGemma Integration and Licensing

This software integrates with Google's **FunctionGemma (`google/functiongemma-270m-it`)** model.

The source code of this project is licensed under the **MIT License**. The MIT License applies only to the software and source code contained in this repository.

### Model weights are not distributed

This repository does **not** include, redistribute, sublicense, or package any FunctionGemma model weights.

Instead, the software contains integration code that allows the required model files to be downloaded separately from their official distribution source, such as Hugging Face:

`google/functiongemma-270m-it`

(The materialization profile in `cartridges/profiles/` fetches the Q8_0 GGUF conversion `unsloth/functiongemma-270m-it-GGUF` of that model, pinned by revision and digest in the blueprint.)

The FunctionGemma model, model weights, and related Google materials are separate third-party components and are **not covered by this project's MIT License**.

### Gemma Terms of Use

FunctionGemma is provided by Google and is subject to the **Gemma Terms of Use** and applicable Gemma usage policies.

Users who download or use FunctionGemma are responsible for reviewing and complying with the applicable Google terms:

https://ai.google.dev/gemma/terms

Official FunctionGemma model page:

https://huggingface.co/google/functiongemma-270m-it

Downloading or using FunctionGemma through this software does not grant any additional rights to the model beyond those provided by Google under the applicable Gemma terms.

### Separation of licenses

In summary:

- **This application's source code:** MIT License
- **FunctionGemma model and model weights:** Google Gemma Terms of Use
- **FunctionGemma weights included in this repository:** None

The FunctionGemma model is obtained separately by the user or by the application from its external distribution source and remains subject to its original terms.

### Third-party components

Other third-party libraries, models, and dependencies used by this project may be subject to their own licenses and terms. Users are responsible for complying with the applicable licenses for those components.

This project is not affiliated with, endorsed by, or sponsored by Google. Google, Gemma, and related names may be trademarks of their respective owners.
