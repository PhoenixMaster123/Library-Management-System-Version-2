
# Library Management System — Frontend

React 19 + TypeScript + Vite, talking to the Spring Boot backend on port 9092.

This is one of four components — see the [root README](../README.md) for the whole picture.

## Running

The backend must be running first:

```bash
cd ..                                                            # the repository root
./mvnw -pl Library-Management-System-Version-2 spring-boot:run    # http://localhost:9092
```

Then:

```bash
npm install
npm run dev                      # http://localhost:5174
```

Sign in as the administrator the backend bootstraps at startup — **`admin` / `admin`** — or
register your own account from the sign-in page. Registration only ever creates *members*, so the
bootstrap administrator is the only way into the admin screens. (Its password comes from
`LIBRARY_ADMIN_PASSWORD`, defaulting to `admin`.)

## Scripts

| Command              | What it does                                                                                  |
| -------------------- | --------------------------------------------------------------------------------------------- |
| `npm run dev`        | Dev server with HMR on :5174                                                                  |
| `npm run build`      | Type-check (`tsc`) then production build to `dist/`                                           |
| `npm run preview`    | Serve the built `dist/` locally                                                               |
| `npm test`           | Unit tests (Vitest + Testing Library), jsdom, no server needed                                |
| `npm run test:watch` | The same, in watch mode                                                                       |
| `npm run test:e2e`   | End-to-end (Playwright, real Chromium). Starts the dev server; **needs the backend on :9092** |

`npm run test:e2e` skips with an explanation rather than failing when the backend is absent — a
missing server is not a broken frontend, and a suite that goes red for that trains people to
ignore red suites.

## How it talks to the backend

The backend has **no CORS configuration**, so the browser would block a direct call from
:5174 to :9092. Instead `vite.config.ts` proxies everything under `/backend`:

```
browser → /backend/books/paginated → vite proxy → http://localhost:9092/books/paginated
```

Because the browser only ever sees same-origin requests, no CORS headers are needed.

**For production** you have two options:
1. Serve `dist/` from the Spring app (copy into `src/main/resources/static/`), which keeps
   everything same-origin; or
2. Deploy separately and add a `CorsConfigurationSource` bean to `SecurityConfig.java`
   allowing your frontend's origin.

## Authentication

`POST /api/login` returns a JWT in the response body (and in an `Authorization` header),
together with the `username` and `role` of the account. The token is stored in `localStorage`
under `library.jwt`, the identity under `library.session`, and the token is attached to every
subsequent request by `src/api/client.ts`.

- **401** means the token is missing, expired or rejected: the client drops it and redirects
  to `/login`.
- **403** means the token is fine but the account may not go there — a member asking for the
  customer list. The token is *kept* and the error is shown in place, because signing the user
  out would be the wrong answer.

`POST /api/register` creates an account plus the matching library membership in one call, and
the client signs the new member straight in. `GET /api/me` re-checks a stored token on start-up.
`POST /api/logout` ends any server-side session and answers in JSON; the client clears its token
either way.

The backend runs two security filter chains (see `SecurityConfig.java`): the API paths are
stateless and answer with status codes, while the server-rendered pages on `/` and `/login` use
the classic username/password form with a session cookie. That is why logout has two doors —
`/api/logout` for this client, `/logout` for the browser form.

### Roles

The JWT carries a `role` claim, which the backend turns back into `ROLE_USER` / `ROLE_ADMIN`.

| Screen              | USER                             | ADMIN                                             |
| ------------------- | -------------------------------- | ------------------------------------------------- |
| Books               | yes                              | yes — plus add, edit, delete and catalogue import |
| Discover            | yes                              | yes                                               |
| Account             | yes                              | yes                                               |
| Loans               | no                               | yes                                               |
| Insights            | no                               | yes                                               |
| Customers (members) | no — `/customers/**` answers 403 | yes                                               |

The nav hides the Loans, Insights and Customers tabs for members; `RequireAdmin` in `src/auth/RequireAuth.tsx`
explains the restriction if someone types the URL. Both are conveniences — the real rule lives
in `SecurityConfig.java`.

> Note: `localStorage` is readable by any JavaScript on the page, so it is vulnerable to XSS.
> It is the pragmatic choice for a stateless JWT backend like this one; if you need stronger
> guarantees, move to an httpOnly cookie, which requires backend changes.

## Layout

```
src/
  api/
    client.ts        fetch wrapper: JWT header, error mapping, 404-empty-page handling
    services.ts      typed calls per resource (books, authors, customers, transactions)
  auth/
    AuthContext.tsx  session (username + role), login/register/logout, re-checked via /api/me
    RequireAuth.tsx  route guards: RequireAuth + RequireAdmin
  components/
    Layout.tsx         nav shell + <Outlet/>, hides admin-only tabs
    TableStates.tsx    skeleton rows, empty state, search box
    Pagination.tsx     page controls + page-size select
    Modal.tsx          dialog shell used by the detail and form panels
    BookDetail.tsx     one book, with borrow/return and the admin edit entry point
    BookForm.tsx       add/edit a book (admin)
    CatalogImport.tsx  bulk import from Open Library (admin)
    CustomerDetail.tsx one member and their loans
    LoansTable.tsx     shared loan table used by Loans and Account
  hooks/
    useApiCall.ts      loading/error/data state, ignores out-of-order responses
  pages/
    LoginPage.tsx
    RegisterPage.tsx     sign-up, then straight into the app
    BooksPage.tsx        paginated catalogue + debounced search, admin row actions
    DiscoverPage.tsx     browsing by subject
    AccountPage.tsx      the signed-in account: profile and own loans (tabbed)
    LoansPage.tsx        every loan in the library (admin only)
    InsightsPage.tsx     borrowing statistics from Analytics-Service (admin only)
                         three states: unreachable / connected-but-unfed / live
    CustomersPage.tsx    paginated member list + debounced search (admin only)
  types/
    domain.ts          types mirroring the backend JSON
```

## Deploying to a static host

`npm run build` produces a `dist/` that any static host can serve, but two things have to be set at
build time:

```bash
VITE_BASE_PATH=/my-repo/            # when served from a subdirectory
VITE_API_BASE_URL=https://api.example.com   # no dev proxy exists outside `npm run dev`
```

`VITE_API_BASE_URL` defaults to `/backend`, the dev proxy path. Point it at a real backend origin
and add CORS on the Spring side, since the requests are then cross-origin. Left unset on a static
host, the app loads and every call fails — which the UI reports as "can't reach the library" rather
than breaking.

For an SPA, also copy `index.html` to `404.html`: a static host has no rewrite rule, so a deep link
is a 404 until the same document is served for it.

## Demo mode

Built with `VITE_DEMO=true`, the app answers its own requests from `src/api/demo/` instead of the
network: an in-memory library in `store.ts`, and the rules in `backend.ts` - a member borrows only
against their own membership, an administrator sees the admin screens, a book is out or it is not.
It exists so the published site works with nothing hosted behind it.

`client.ts` routes there inside `request()`, so no page or service knows the difference, and the
same `ApiError`/`ForbiddenError`/`UnauthorizedError` come back either way. The import is dynamic
and the flag is static, so a normal build drops the whole thing.

It is not the real backend: data is per-browser, the token is a username in a string, and Discover
returns invented results rather than searching Open Library.

## Error messages

Nothing in the UI shows an HTTP status code. `Request failed with status 502` is a fact about
HTTP, not about the library, and there is nothing a reader can do with it.

`client.ts` resolves a failure in this order:

1. **The server's own message**, when it reads like a sentence — validation errors are written for
   people and beat anything the client can invent.
2. **A sentence for the status class** otherwise: a 502/503/504 becomes "The library server is not
   responding. It may be restarting — try again in a moment."
3. **`OfflineError`** when `fetch` rejects outright, meaning the request never arrived. The
   browser's own wording for this is "Failed to fetch"; ours says the library can't be reached.

HTML bodies are discarded before display — both the Vite proxy and Spring's error page answer with
HTML when the backend is down, and a raw `<!doctype html>` must never reach the user. Bodies over
300 characters are dropped for the same reason.

Errors that a reload would fix render through `ErrorNotice`, which offers a **Try again** button,
so recovering from a restarting backend is one click rather than a page refresh.

## Backend quirks encoded here

These are real behaviours of the API that the client works around — worth knowing before
you add screens:

- **Paginated endpoints return 404, not an empty page**, when a page has no rows.
  `api.getPage()` translates that into an empty result.
- **Field names are not `id`.** They are `bookId`, `customerId`, `authorId`, `transactionId`.
- **`available`**, not `isAvailable` — Jackson strips the `is` prefix from the Java getter.
- **Some endpoints return plain text**, not JSON (e.g. `POST /transactions/borrowBook/...`).
  The client falls back to returning the raw string when the body isn't valid JSON.
- **`GET /books` with no query parameter** answers `"No search criteria provided"`.
  Use `/books/paginated` to list, `/books?query=…` to search.
- **Search endpoints answer 404 with plain text** when nothing matches; `api.getList()`
  turns that into an empty array.
- **`GET /admin/analytics` answers 503 when Analytics-Service is down.** `analyticsApi.overview`
  resolves that to `null` instead of throwing, so `InsightsPage` can tell "could not be read" from
  "nothing to read" — zeroed tiles would claim the library has never lent a book. This has to live
  in the service layer because `useApiCall` keeps only the error *message*, not its status.
- **`/books?query=…` returns a bare array, not a page object** — no totals — so `BooksPage`
  infers "is there a next page" from whether the page came back full.

## Adding a screen

1. Add the call to `src/api/services.ts` (types come from `src/types/domain.ts`).
2. Build the page in `src/pages/`, using `useApiCall` for loading/error state.
3. Register the route in `src/App.tsx` inside the `RequireAuth` block.

## Table column widths

`BooksPage` renders with `table-layout: fixed`, so a column never grows to fit its contents —
every width in the `.books-table` rules has to be wide enough for the widest thing that column
can hold. The status pill is `white-space: nowrap` and cannot shrink, and nothing in the table
clips, so a column that is too narrow does not truncate: its contents spill over the next column.

The admin view carries a sixth column of row actions, which leaves less room for the other five.
That is why the widths are declared twice — `.books-table` for the reader's five columns, and
`.books-table.has-actions` for the admin's six. `BooksPage` adds `has-actions` when `isAdmin`.
If you add a column, or widen the contents of one, adjust both sets and the `min-width` with it.
