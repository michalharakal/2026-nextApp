# Architecture (C4)

The system in three zoom levels, as Mermaid C4 diagrams that GitHub renders inline:

| Level | Diagram | Question it answers |
|---|---|---|
| 1 | [System context](01-context.md) | Who talks to what, and where the local/cloud border is |
| 2 | [Containers](02-containers.md) | What actually runs, and what travels between the pieces |
| 3 | [Components — app](03-components-app.md) | How an utterance flows through the app's modules |
| 3 | [Components — companion](04-components-companion.md) | How the middleware routes tools and serves cartridges |

The one-slide version of the same story (numbered flow, SKaiNET look) is
[`../demo-architecture.svg`](../demo-architecture.svg); the prose version is
[`../ARCHITECTURE.md`](../ARCHITECTURE.md).

Two invariants every level repeats:

- **Local first.** Speech recognition, language understanding and home control run on the device;
  the demo works with no internet at all.
- **One companion, two payloads.** The same REST server delivers intelligence down (signed model
  cartridges) and executes delegated calls up (remote tools) — the private control backend plugs
  into the same seam without the public code knowing it exists.
