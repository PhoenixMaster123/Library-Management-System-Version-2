# Analytics-Service

Consumes loan events from Kafka and keeps a running tally of what the library lends. One of four
components — see the [root README](../README.md) for the whole picture.

## Running

Needs a Kafka broker on **localhost:9094**.

```bash
cd ..                                           # the repository root
./mvnw -pl Analytics-Service spring-boot:run    # http://localhost:9095
```

Without a broker the service still starts and simply never sees an event
(`spring.kafka.listener.missing-topics-fatal=false` keeps it from logging a stack trace every few
seconds). It is optional: the library works whether or not this is running.

> **On the ports.** The broker owns 9094 and HTTP is on 9095. These were both 9094 at one point,
> which cannot work — the service cannot bind a port and also dial the broker on it. The broker's
> 9094 is the fixed point, since the library is configured to publish there, so this service moved
> instead.

## Endpoints

| Endpoint                                       | Returns                                                                       |
| ---------------------------------------------- | ----------------------------------------------------------------------------- |
| `GET /api/v1/analytics/summary`                | totals across all loans                                                       |
| `GET /api/v1/analytics/popular-books?limit=10` | most-borrowed books, with borrow/return counts and how many are currently out |

## Who reads it

The library backend, on behalf of the admin **Insights** page. The browser never calls this
service directly - it has no authentication of its own, so the read goes through the library's
`/admin/analytics`, which is already behind a JWT and an admin role check:

```
browser --JWT--> library :9092 --Feign--> analytics :9095
```

When this service is down that endpoint answers `503` and the page says so, rather than showing
zeros - which would read as a library that has never lent a book.

### Being up is not the same as being fed

The summary carries `streamConnected` alongside the totals, because "reachable" and "receiving
events" are different things and only one of them makes the numbers mean anything:

| This service | The broker | `streamConnected` | The page shows                         |
| ------------ | ---------- | ----------------- | -------------------------------------- |
| down         | —          | (503)             | "Analytics-Service isn't running"      |
| up           | down       | `false`           | the figures, marked **not up to date** |
| up           | up         | `true`            | the figures                            |

Without that middle row a healthy service with no broker behind it reports zeros that read as
fact - and a library with a book out is told nothing has ever been borrowed.

`streamConnected` is read from the listener container's partition assignments: a consumer holding
assignments is by definition talking to a broker, and the answer costs no I/O. It is also `false`
in the moments after start-up, before the group has rebalanced, and when the broker is up but the
topic does not exist yet - in each case nothing can arrive, which is what the caller is asking.

## How it works

The library publishes a `LoanEvent` to `library.loans` whenever a book is borrowed or returned.
`LoanEventListener` folds each event into a `BookStat` row keyed by book.

```
library (:9092) ──LoanEvent──▶ Kafka library.loans (:9094) ──▶ LoanEventListener ──▶ BookStat
```

`LoanEvent` is a record kept structurally identical to the producer's — **the topic is the contract
between the two services**, so changing one side means changing the other.

Storage is H2 in-memory and deliberately disposable: the statistics are a projection that can be
rebuilt from the topic, so nothing here is a source of truth. The consumer reads with
`auto-offset-reset=earliest`, so a restart replays the topic from the beginning.

## Testing

```bash
cd ..                             # the repository root
./mvnw -pl Analytics-Service test
```
