# Notification-Service

Sends and records notifications for the library, and stores each member's notification
preferences. One of four components; see the [root README](../README.md) for the whole picture.

The Maven project is one level down, in `Notification-Service/`.

## Running

Needs MySQL on **localhost:3306**; the schema `notification_service` is created on connect.

```bash
cd ..                                              # the repository root
./mvnw -pl Notification-Service spring-boot:run     # http://localhost:9093
```

It is optional: the library calls it over OpenFeign with short timeouts (2s connect, 3s read) and
borrowing a book still succeeds when this service is down.

## Email is opt-in

`notification.mail.enabled` is **`false`** by default. Notifications are still persisted and
returned with status `PENDING`, so nothing fails against an unconfigured mailbox. To send real
mail, fill in the SMTP credentials and flip the flag:

```properties
spring.mail.username=…
spring.mail.password=…
notification.mail.enabled=true
```

The defaults point at Gmail SMTP on 587 with STARTTLS.

## Endpoints

All under `/api/v1/notifications` — the URL the library is configured to call
(`notification.service.url`).

| Method | Path                         | Purpose                            |
| ------ | ---------------------------- | ---------------------------------- |
| `POST` | `/`                          | send a notification (201)          |
| `GET`  | `/?userId=<uuid>`            | that member's notification history |
| `POST` | `/preferences`               | upsert a member's preference (201) |
| `GET`  | `/preferences?userId=<uuid>` | read a member's preference         |
| `GET`  | `/test`                      | liveness check, returns plain text |

Preferences are **upserted** through one endpoint rather than split into create and update.
Members are identified by a `userId` UUID supplied by the caller; this service has no
authentication of its own. `ApiExceptionHandler` maps failures to error responses.

### Status and channel

A notification is **always persisted**; `NotificationStatus` records what became of the send
attempt:

| Status      | Meaning                                                                  |
| ----------- | ------------------------------------------------------------------------ |
| `PENDING`   | stored but not dispatched — what you get while mail delivery is disabled |
| `SUCCEEDED` | handed off to the mail server without error                              |
| `FAILED`    | dispatch attempted and failed; see `failureReason`                       |

`NotificationType` is the delivery channel, and currently has one value: `EMAIL`.

## Testing

```bash
cd ..                                # the repository root
./mvnw -pl Notification-Service test
```
