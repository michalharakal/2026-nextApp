#!/usr/bin/env python3
"""Write measured on-device numbers into the materialization profiles.

    scripts/apply-measurements.py --device "<what the numbers came from>" <golden-report.json> [...]

Each report is what RealCartridgeGoldenTest writes on the phone (one per pack and IREE device). The NLU profile of
the report's target gets latency p50/p95 of the typed golden set; the ASR profile gets the real-time factor of the
spoken utterance. `measured_on` names the device and run so the numbers are falsifiable; `methodology` points at the
golden set. Never copies anything from a blueprint's reference_measurements.
"""
import argparse, datetime, json, pathlib, sys

HERE = pathlib.Path(__file__).resolve().parent.parent
PROFILES = HERE / "cartridges" / "profiles"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--device", required=True, help="device and run description, e.g. 'Pixel 8a (Tensor G3, Mali GPU), Android 17'")
    ap.add_argument("--date", default=datetime.date.today().isoformat())
    ap.add_argument("reports", nargs="+")
    a = ap.parse_args()
    for path in a.reports:
        r = json.load(open(path))
        target = "vulkan-arm64" if "vulkan" in r["nlu"] else "cpu-arm64"
        backend = "Vulkan GPU" if r["device"] == "GPU" or (r["device"] == "AUTO" and target == "vulkan-arm64") else "CPU (local-task)"
        where = f"{a.device}, {backend}, IREE 3.11.0, {a.date}"
        nlu = json.load(open(PROFILES / f"nlu-functiongemma.{target}.home.json"))
        nlu["measurements"] = {"performance": {
            "latency_p50_ms": r["nlu_p50_ms"], "latency_p95_ms": r["nlu_p95_ms"],
            "measured_on": f"{where}; typed golden set of {r['total']} utterances against the packed catalog, {r['passed']}/{r['total']} correct; "
                           f"warm-up {r['nlu_warmup_ms'] / 1000:.1f} s, RSS {r['rss_end_mb']} MB",
            "methodology": "docs/golden-utterances.md (RealCartridgeGoldenTest, per-utterance tokenize + KV restore + one chunk call + greedy decode)",
        }}
        write(PROFILES / f"nlu-functiongemma.{target}.home.json", nlu)
        spoken = r.get("spoken")
        if spoken:
            asr = json.load(open(PROFILES / f"asr-moonshine.{target}.en.json"))
            asr["measurements"] = {"performance": {
                "rtf": round(spoken["asr_ms"] / spoken["audio_ms"], 3),
                "latency_p50_ms": spoken["asr_ms"],
                "measured_on": f"{where}; one text-to-speech utterance of {spoken['audio_ms']} ms streamed in 80 ms frames, {spoken['partials']} partials, exact transcript",
                "methodology": "docs/golden-utterances.md (RealCartridgeGoldenTest spoken path)",
            }}
            write(PROFILES / f"asr-moonshine.{target}.en.json", asr)

def write(path, doc):
    signing = doc.pop("signing", None)
    if signing: doc["signing"] = signing
    path.write_text(json.dumps(doc, indent=2, ensure_ascii=False) + "\n")
    print(f"{path.name}: {json.dumps(doc['measurements']['performance'])[:160]}…")

if __name__ == "__main__":
    sys.exit(main())
