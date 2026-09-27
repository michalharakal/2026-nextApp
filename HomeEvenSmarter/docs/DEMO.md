# Demo checklist

## The day before
1. `scripts/materialize.sh all vulkan-arm64` (and `cpu-arm64` as fallback) — verify `build/cartridges/` holds both packs.
2. `scripts/device-check.sh` — arm64, ≥ 6 GB RAM, ≥ 3 GB free, developer mode on.
3. `./gradlew :app:androidApp:installDebug`; grant the microphone permission.
4. `scripts/serve-cartridges.sh`; in the app → Cartridges → enter the printed URL → Fetch → Download both.
5. Verify both packs on the phone (Cartridges → Verify).
6. Load & warm up. Note the warm-up seconds.
7. Run golden set (typed): expect 10/10 in-catalog names, the two off-catalog utterances on the cloud chip.
8. Speak the ten utterances in `docs/golden-utterances.md`. Note misses.
9. Switch device cpu ↔ gpu once each; keep the faster one.

## Before going on stage
- Phone: airplane mode + WiFi off (nothing is needed once loaded), brightness max, do-not-disturb, charger.
- App open, engines warm, Home screen visible; rotate once to confirm the engines survive.
- Laptop: desktop app on fake engines as the fallback (`./gradlew :app:desktopApp:run`).
- External microphone if the room is loud; push-to-talk needs no silence detection.

## During the demo
1. Show the Cartridges screen: two cartridges, verified, loaded — models as dependencies.
2. Home + Pipeline side by side. Hold to talk: "turn on the kitchen light".
3. Point at the partials, the function call, the timing bars.
4. "What is the weather like tomorrow" → no call → the greyed cloud stage: the next step.

## If something fails
- Engines fail to load → toggle "fake engines" on: the UI keeps working, say so.
- Wrong function → type the sentence in the pipeline panel to show it is the ASR, not the NLU (or the other way round).
- App killed (memory) → relaunch, Load & warm up again (tens of seconds; talk about warm-up meanwhile).
