# Documentation

|                                           |                                                         |
| ----------------------------------------- | ------------------------------------------------------- |
| [`architecture/`](architecture/README.md) | How the system is put together — one page per component |
| [`decisions/`](decisions/)                | Design specs, dated, recording what was agreed and why  |

**Architecture** describes how things are *now*. **Decisions** are a history: each file captures a
design at the point it was agreed, and is not rewritten afterwards. Where the two disagree, the
architecture pages are right and the spec has simply aged.

## Architecture

- [Overview](architecture/README.md) — the four components, how they fit, and what happens when
  each is missing
- [Library backend](architecture/library-backend.md) — hexagonal architecture, ports and adapters,
  security
- [Frontend](architecture/frontend.md) — routes, API client, error handling
- [Notification-Service](architecture/notification-service.md)
- [Analytics-Service](architecture/analytics-service.md)

## Running the system

Setup and commands live in the READMEs next to the code, not here, so they stay near what they
describe:

- [Root README](../README.md) — starting everything, ports, testing, CI
- [Backend](../Library-Management-System-Version-2/README.md)
- [Frontend](../frontend/README.md)
- [Notification-Service](../Notification-Service/README.md)
- [Analytics-Service](../Analytics-Service/README.md)
