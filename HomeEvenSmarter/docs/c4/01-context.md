# Level 1 — System context

The resident speaks to the app; everything that must work lives on the device. The companion is an
optional laptop on the same network: it distributes model cartridges and executes the few tools
that genuinely need the outside world.

```mermaid
C4Context
  title System context — HomeEvenSmarter

  Person(resident, "Resident / Presenter", "Speaks a command, watches the home react")

  Enterprise_Boundary(local, "Home LAN — no internet required") {
    System(app, "HomeEvenSmarter app", "Android (demo target) and desktop. On-device ASR + NLU, home model, pipeline UI.")
    System(companion, "Companion server", "Laptop middleware: cartridge registry and remote-tool API (REST, OpenAPI).")
  }

  System_Ext(weather, "Weather API", "Open weather data; the sample remote tool's upstream.")
  System_Ext(bus, "Building-bus installation", "Real lights and sensors behind a gateway; driven by a private control backend plug-in.")

  Rel(resident, app, "push-to-talk, taps")
  Rel(app, companion, "cartridge downloads, POST /tools/…", "REST over WiFi/USB")
  Rel(companion, weather, "current weather + forecast", "HTTPS, API key from env")
  Rel(companion, bus, "switch lights, read states", "gateway protocol (private plug-in)")
```

- The **app never knows** what sits behind the companion: weather, a building bus, or nothing.
- Without a companion the app still listens, understands and controls its home model; remote tools
  fail softly and land on the escalation seam.
