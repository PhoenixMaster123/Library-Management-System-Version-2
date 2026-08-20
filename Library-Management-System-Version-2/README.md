# Library Management System — Backend

The Spring Boot backend, built in Hexagonal Architecture. It owns the catalogue, members, loans
and authentication, and serves the REST API that `frontend/` consumes.

This is one of four components — see the [root README](../README.md) for the whole picture and for
running everything together.

## Architecture

The domain sits in the middle and depends on nothing. Everything reaching in or out does so
through a port, and adapters implement those ports:

```
adapters/input/rest/     REST controllers          ─┐
adapters/input/web/      Thymeleaf pages           ─┤→  domain/port/input/   (use cases)
                                                    │
                          domain/model/  domain/services/
                                                    │
adapters/output/repositories/  JPA               ─┐ │
adapters/output/catalog/       Open Library       ─┤←┘  domain/port/output/
adapters/output/notification/  OpenFeign          ─┤
adapters/output/events/        Kafka producer     ─┘
```

**Input ports** (`domain/port/input/`) — `BookUseCase`, `AuthorUseCase`, `CustomerUseCase`,
`TransactionUseCase`, `ReminderUseCase`.

**Output ports** (`domain/port/output/`) — the four repository ports, plus `BookCatalogPort`
(Open Library), `NotificationPort` (Notification-Service), `LoanEventPort` (Kafka) and
`ReminderPreferencePort`.

The point of the outward ports is that all three external systems are optional. Each has an
adapter that can be switched off by configuration, and borrowing a book still succeeds when
Kafka and Notification-Service are both unreachable.

## Running

```bash
./mvnw spring-boot:run       # http://localhost:9092
```

With an empty catalogue the application stocks itself from Open Library on first start
(~40 books across 12 subjects). The `dev` profile loads the bundled JSON fixture instead and
never makes that call:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

|        | default profile                          | `dev` profile                             |
| ------ | ---------------------------------------- | ----------------------------------------- |
| Seeder | `CatalogSeeder` — Open Library over HTTP | `DatabaseSeeder` — `resources/files/json` |

Data lives in H2 in-memory and is gone on restart. The console is at
[`/h2-console`](http://localhost:9092/h2-console) (JDBC URL `jdbc:h2:mem:library_ms`,
user `root`, password `12345`), and API docs at `/swagger-ui.html`.

### Staying signed in across restarts

`library.jwt.secret` is the signing key. Set it and a token keeps working across restarts; leave it
blank and a fresh key is generated per start-up, which invalidates every token that was ever
issued.

That blank-by-default behaviour used to be the only behaviour, and with `spring-boot-devtools` on
the classpath a restart happens every time a class changes — so anyone using the application was
signed out constantly and had to clear the browser's stored token by hand. The bundled value keeps
local development usable:

```properties
library.jwt.secret=${LIBRARY_JWT_SECRET:local-development-only-signing-key-change-me}
```

**Override it by environment anywhere real.** A signing key committed to source is a key anyone
with the source can mint valid tokens with. Minimum 32 characters; a shorter one is rejected at
start-up rather than silently weakening the signature.

> Note the database is in-memory, so self-registered members do not survive a restart either. The
> administrator is recreated each time by `DataInitializer`; members must register again.

### The administrator account

`DataInitializer` creates exactly one account at startup: **`admin` / `admin`**. Self-registration
only ever creates members, so without it the admin screens have no way in.

```bash
export LIBRARY_ADMIN_PASSWORD=…    # override outside local runs; blank skips creating it
```

The application logs a warning while the default password is still in use.

## Reloading on change

`spring-boot-devtools` restarts the application when the classes under `target/classes` change —
so a restart only happens once something has **recompiled**. From the command line
`./mvnw spring-boot:run` handles that itself. In IntelliJ it takes two settings, and without both
nothing appears to happen:

- **Settings → Build → Compiler → Build project automatically**
- **Settings → Advanced Settings → Allow auto-make to start even if developed application is
  currently running**

Sessions now survive those restarts (see `library.jwt.secret` above), so a reload no longer signs
you out.

The React frontend is separate and needs none of this: Vite's dev server hot-reloads on save.

## Static analysis

Two tools, split by what they look for. Both rulesets live in the **repository root**, not in this
module, because Notification-Service and Analytics-Service use the same two files - one definition
of "correct" for all three services rather than three copies that drift apart:

| Tool       | Looks for               | Config                                | Phase      |
| ---------- | ----------------------- | ------------------------------------- | ---------- |
| Checkstyle | layout, naming, imports | `../config/checkstyle/checkstyle.xml` | `validate` |
| PMD        | bug patterns            | `../config/pmd/ruleset.xml`           | `verify`   |

Both fail the build. Checkstyle runs at `validate` so a style failure costs seconds rather than a
full build; PMD runs at `verify` because it wants compiled classes.

```bash
./mvnw checkstyle:check      # style only
./mvnw pmd:check             # bugs only
./mvnw verify                # both, plus the tests
./mvnw verify -Dcheckstyle.skip=true -Dpmd.skip=true
```

Both rulesets are deliberately narrow. Checkstyle's bundled `sun_checks.xml` reports **1806**
violations on this codebase — demanding Javadoc on every method and an 80-column limit — and a
ruleset that size is one people switch off within a week. The rules kept are the ones that catch a
real problem or a real inconsistency.

Where a rule is excluded, the reason is written next to it. The substantial ones:

- **`GuardLogStatement`** — SLF4J's placeholders already defer formatting, so `isDebugEnabled()`
  guards buy nothing. 35 of PMD's 58 initial findings, all noise.
- **`LooseCoupling`** — narrowed rather than removed, via `allowedTypes`: it fired only on Spring's
  `HttpHeaders`, which `ResponseEntity` requires by that exact type.
- **`ReturnEmptyCollectionRatherThanNull`** — `JwtService.getClaims` returns `null` for "no valid
  token" and every caller checks it. An empty `Claims` would make an unauthenticated request look
  authenticated.

## Security

Two filter chains, because this application serves both an API and server-rendered pages
(`SecurityConfig.java`):

| Chain                    | Matches                                                                                 | Session       | Auth               |
| ------------------------ | --------------------------------------------------------------------------------------- | ------------- | ------------------ |
| `apiSecurityFilterChain` | `/api/**`, `/admin/**`, `/books/**`, `/authors/**`, `/customers/**`, `/transactions/**` | stateless     | JWT bearer token   |
| `webSecurityFilterChain` | everything else (`/`, `/login`)                                                         | `IF_REQUIRED` | form login, cookie |

The API chain answers with status codes rather than redirects — 401 when the token is missing or
rejected, 403 when the account may not go there. That is why logout has two doors: `/api/logout`
answers in JSON for the SPA, `/logout` for the browser form.

Admin-only: `/admin/**`, `/customers/**`, and `/transactions/history/**` — reading someone else's
loans by id is administrator work, so members use `/transactions/me` instead.

## API

| Root             | Purpose                                                            |
| ---------------- | ------------------------------------------------------------------ |
| `/api`           | login, register, logout, current user                              |
| `/api/profile`   | the signed-in account's own details                                |
| `/api/reminders` | due-date reminder sweep                                            |
| `/books`         | catalogue — `/books/paginated` to list, `/books?query=…` to search |
| `/authors`       | authors                                                            |
| `/customers`     | members (admin)                                                    |
| `/transactions`  | borrow, return, history                                            |
| `/admin`         | administrative operations, including `/admin/analytics`            |

Responses carry HATEOAS links and HTTP cache headers. The API has quirks the frontend has to work
around — paginated endpoints answer 404 rather than an empty page, some routes return plain text,
and identifiers are `bookId` / `customerId` rather than `id`. They are catalogued in
[`frontend/README.md`](../frontend/README.md#backend-quirks-encoded-here).

## Configuration

All in `src/main/resources/application.properties`:

| Property                           | Default       | Effect                                                                                |
| ---------------------------------- | ------------- | ------------------------------------------------------------------------------------- |
| `library.events.enabled`           | `true`        | Publish loan events to Kafka on 9094.                                                 |
| `notification.enabled`             | `true`        | Call Notification-Service on 9093 when a book is borrowed.                            |
| `library.catalog.seed.enabled`     | `true`        | Stock an empty catalogue from Open Library.                                           |
| `library.catalog.seed.per-subject` | `40`          | How many books per subject to fetch.                                                  |
| `library.reminders.cron`           | `0 0 8 * * *` | Daily sweep for loans falling due.                                                    |
| `library.reminders.days-before`    | `3`           | How far ahead that sweep looks.                                                       |
| `analytics.enabled`                | `true`        | Read statistics from Analytics-Service on 9095 for the Insights page.                 |
| `library.jwt.secret`               | dev key       | JWT signing key. Blank means a new key per start-up, signing everyone out on restart. |
| `spring.cache.type`                | `simple`      | In-memory cache.                                                                      |

Two settings are deliberate and worth not "tidying up":

- **`spring.cache.type=simple`** — `spring-boot-starter-data-redis` is on the classpath, so Boot
  would otherwise auto-select Redis and every cached call would fail against a server that is not
  running. Set it to `redis` and uncomment `spring.data.redis.*` once one is.
- **Feign timeouts of 2s connect / 3s read** — on both the notification and analytics clients. A
  slow or dead service must not stall a borrow request or hang an admin page.

`GET /admin/analytics` answers **503**, not zeroed figures, when Analytics-Service cannot be
reached. The two are not the same thing: zeros would claim the library has never lent a book. The
port returns an empty `Optional` and the frontend renders that as an explanation.

## Testing

```bash
./mvnw test                          # 118 unit tests, ~1 min
./mvnw verify                        # those plus 143 integration tests, ~3.5 min
./mvnw -f pom-docker.xml verify      # integration tests against Docker
```

> **If the suite fails in ways that make no sense, check what else is running.** A
> `spring-boot:run` or IDE-launched application serves from `target/classes`, which the build
> rewrites underneath it. That used to produce `No qualifying bean` and `ClassNotFoundException`
> for classes plainly present on disk — phantom failures with nothing to do with the code under
> test, and it took two debugging passes to recognise them as such.
>
> It no longer reproduces: the suite now builds ~15 Spring contexts instead of ~100, and passes
> with a backend running alongside it. Worth knowing anyway, because the symptom is so misleading.

Two plugins split the work by filename: **surefire** runs `*Test` at the `test` phase, **failsafe**
runs `*IT` at `integration-test`. The suffix is the whole mechanism — a new integration test is
picked up by being named `…IT.java` and by nothing else.

|         | surefire                   | failsafe           |
| ------- | -------------------------- | ------------------ |
| Phase   | `test`                     | `integration-test` |
| Matches | `*Test`, `Test*`, `*Tests` | `*IT`              |
| Count   | 118                        | 143                |

### Test isolation

The integration tests do **not** use `@DirtiesContext`. They used to, on every class, which rebuilt
the Spring context between test methods — about a hundred application start-ups per run, and most
of a ten-minute suite.

`app.support.TestStateResetListener` does the same job before each test method for a fraction of
the cost, registered for every Spring test through `src/test/resources/META-INF/spring.factories`.
It restores the three things the annotation was quietly providing:

| What                           | Why it matters                                                                                                                                                                                   |
| ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| A wiped and re-seeded database | Closing a context drops the in-memory H2. Without that, classes using `@MockitoBean` get a second context on the same `jdbc:h2:mem:library_ms` and each seeder adds another copy of the fixture. |
| The dev fixture                | `DatabaseSeeder` is a `CommandLineRunner`, so a new context re-seeded the shelves; the first `@AfterEach` wipes them for everyone after it.                                                      |
| Empty caches                   | Entries outlive the rows they came from, so a later test can be answered from data an earlier one deleted.                                                                                       |

It runs before `@BeforeEach`, so anything a test sets up for itself survives. A test that needs an
*empty* table has to say so — see `CustomerUseCaseIT`, which counts every row.

`OpenLibraryCacheIT` is the one class that keeps `@DirtiesContext`, for an unrelated reason: its
`MockRestServiceServer` expectations accumulate for the life of the context and it asserts exact
call counts.

## Technologies

Java 21 · Spring Boot 3.5.4 · Spring Data JPA (Hibernate) · Spring Security + JWT (jjwt) ·
Spring HATEOAS · Spring Kafka · Spring Cloud OpenFeign · Spring Cache · Thymeleaf ·
springdoc OpenAPI · ModelMapper · Lombok · H2 · MySQL · Actuator · JUnit · Mockito

## License ⚖️

MIT — see [LICENSE](../LICENSE).
