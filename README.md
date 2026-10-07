[![From Dumb Client to Hybrid Intelligence — next.app devcon 2026, Berlin](https://harakal.de/assets/banners/nextapp-devcon-2026.png)](https://harakal.de/talks/from-dumb-client-to-hybrid-intelligence-nextapp-devcon-berlin-2026)

# From Dumb Client to Hybrid Intelligence

**Building Production On-Device AI Systems** · [next.app devcon 2026, Berlin](https://harakal.de/talks/from-dumb-client-to-hybrid-intelligence-nextapp-devcon-berlin-2026) · Michal Harakal

Voice assistants don't need the cloud to be smart. This talk builds a production-shaped
smart-home voice pipeline that runs **entirely on the phone** — streaming speech recognition
(Moonshine v2) and function-calling language understanding (FunctionGemma 270M), packaged as
signed, verifiable **model cartridges** built from public blueprints — and then takes the one
honest step into *hybrid*: a companion middleware that delivers models down and executes the few
tools that genuinely need the outside world, over one small REST API. Local first, cloud where it
earns its place, and the boundary visible on screen the whole time.

## In this repository

| | |
|---|---|
| [**HomeEvenSmarter**](HomeEvenSmarter/) | The companion demo app: Kotlin Multiplatform, Compose, on-device ASR + NLU via [SKaiNET](https://github.com/SKaiNET-developers/SKaiNET)/IREE, Ktor companion server |
| [Architecture (C4)](HomeEvenSmarter/docs/c4/) | Context → containers → components, as Mermaid diagrams |
| [Demo flow diagram](HomeEvenSmarter/docs/demo-architecture.svg) | The one-slide version: building blocks, local/cloud border, numbered data flow |
| [`v0.2.0`](../../releases/tag/v0.2.0) | The exact version shown on stage, with reproduction notes |
