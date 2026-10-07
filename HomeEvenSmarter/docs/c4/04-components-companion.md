# Level 3 — Components: the companion

A deliberately small Ktor application: parameters in, testable pieces, no DI framework.

```mermaid
C4Component
  title Components — companion server (module :server)

  Container_Boundary(srv, "Companion server") {
    Component(app, "Application", "Application.kt", "Config from env/props; wires catalog + tool registry; prints reachable URLs")
    Component(catalog, "CartridgeCatalog", "CartridgeCatalog.kt", "Scans pack_dirs; serves /cartridges/index.json + files (Range)")
    Component(routes, "ToolRoutes", "tools/ToolRoutes.kt", "GET /tools · POST /tools/…; missing body = empty call")
    Component(registry, "ToolRegistry", "tools/ToolRegistry.kt", "Routes a call to whichever backend declares the tool; soft-fails known tools")
    Component(weatherb, "WeatherBackend", "tools/WeatherBackend.kt", "get_weather; env key; pinned upstream endpoints; MockEngine-tested")
    Component(spi, "ToolBackend SPI", ":core remote/", "ToolBackend + ToolBackendProvider; ServiceLoader discovery")
    Component(api, "openapi.yaml", "resources", "The REST contract, served at /openapi.yaml; a route test keeps it honest")
  }

  Container_Ext(control, "Control backend plug-in", "private sibling", "building-bus gateway")
  System_Ext(weather, "Weather API", "HTTPS")
  Container_Ext(phone, "App", "phone / desktop")

  Rel(phone, routes, "POST /tools/get_weather")
  Rel(phone, catalog, "download cartridges")
  Rel(app, registry, "builds from discovered backends")
  Rel(routes, registry, "execute(name, args)")
  Rel(registry, weatherb, "get_weather")
  Rel(registry, control, "home commands (when present)")
  Rel(spi, control, "implemented by")
  Rel(weatherb, weather, "HTTPS")
```

- The wire types and the `ToolBackend` SPI live in `:core`, so a backend depends on tiny pure
  Kotlin — never on Ktor or the server.
- Everything is exercised by `testApplication` tests: no-backend soft failures, 404 for unknown
  tools, backend pass-through, and the self-served OpenAPI document.
