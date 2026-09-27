# Contributing

- Run `scripts/check-terms.sh` before every commit. It greps the tracked and staged files against a private,
  git-ignored word list (`.check-terms.local`, one term per line) and fails on a hit. Keep product, customer and
  device-vendor names out of code, docs, commit messages and issues; describe hardware generically.
- Commits reference the work-package task they belong to (`B2: IntentMapper with room synonyms`).
- `./gradlew :core:jvmTest :cartridges:jvmTest :server:test` must be green; the desktop app must start on fakes.
- Model artifacts never enter the repository (`build/cartridges/`, `*.pem`, `*.key` are ignored). Materialization
  profiles are inputs, not outputs: no measured numbers copied from anywhere, no weights, no compiled modules.
