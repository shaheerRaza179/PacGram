# PacGram backend

PacGram is a layered Spring Boot monolith. The API uses DTOs rather than exposing JPA entities, SQL Server with Flyway-managed schema migrations, JWT authentication, and a common JSON response/error format.

## Requirements

- Java 17+
- Maven 3.9+
- SQL Server Express/Developer with TCP/IP enabled
- A SQL Server login with permission to use the `pacgram` database

The React app in `../Frontend` uses this API for authentication and the supported social features. The administration dashboard is available in that same app at `/admin`.

When the backend finishes starting on a desktop, it automatically opens Swagger UI at `http://localhost:8080/swagger-ui/index.html` in the default browser. Set `SWAGGER_AUTO_OPEN=false` for servers/containers or to disable automatic browser launch; the URL remains available.

## Run with SQL Server Express and SSMS

1. Enable TCP/IP in SQL Server Configuration Manager and configure a known TCP port (the default used below is `1433`).
2. In SSMS, connect to the same SQL Server instance and run [`sql/create_database.sql`](./sql/create_database.sql).
3. In SSMS, execute [`sql/create_database.sql`](./sql/create_database.sql) as a SQL Server administrator. It creates the `pacgram` database and SQL login used by the application (`pacgram` / `pacsquare`) if they do not already exist. Then set a JWT secret and start the API:

```powershell
$env:JWT_SECRET = "<a random secret with at least 32 characters>"
$env:PACGRAM_ADMIN_PASSWORD = "<a strong, unique administrator password>"
mvn spring-boot:run
```

Flyway applies the versioned SQL Server migrations at startup. Hibernate uses `ddl-auto: validate`; it never updates the schema. If SQL Server Express uses a named instance or a different port, update `DB_URL` to match its configured TCP listener.

## API

- OpenAPI UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- API base URL: `http://localhost:8080/api`
- WebSocket/STOMP endpoint: `ws://localhost:8080/ws`
- WebSocket authentication: send `Authorization: Bearer <JWT>` in the STOMP `CONNECT` headers.
- Typing destination: send to `/app/conversations/{conversationId}/typing`; subscribe to `/user/queue/typing`.
- Live message destination: subscribe to `/topic/conversations/{conversationId}`.

Every successful HTTP response is wrapped as `{ "success": true, "data": ..., "timestamp": ... }`. Errors use `message`, `errorCode`, `status`, `path`, `fieldErrors`, and `timestamp`. Lists use Spring pagination parameters (`page`, `size`, `sort`); maximum page size is 100.

### Routes

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/auth/register` | Register |
| POST | `/api/auth/login` | Sign in with `email` or `username` and password |
| GET | `/api/auth/me` | Current identity |
| POST | `/api/auth/forgot-password` | Email a short-lived reset link |
| POST | `/api/auth/reset-password` | Set a new password using the emailed token |
| POST | `/api/auth/change-password` | Change the signed-in user's password |
| POST | `/api/admin/auth/login` | Sign in to the admin dashboard |
| GET | `/api/admin/overview` | User presence and community safety counts |
| GET | `/api/admin/users` | Search and page through user accounts by username, name, or email |
| GET | `/api/admin/users/{userId}` | Account profile, privacy, relationship, report, and block summary |
| GET | `/api/admin/users/{userId}/posts` | Paginated posts with engagement counts |
| GET | `/api/admin/users/{userId}/reports` | Paginated reports filed by or against this user |
| GET | `/api/admin/users/{userId}/blocks` | Paginated block relationships involving this user |
| PATCH | `/api/admin/users/{userId}/status` | Suspend or reactivate a user |
| GET, PATCH | `/api/admin/reports` | Review and update submitted user/content reports |
| GET | `/api/admin/audit` | Paginated log of administrative actions |
| GET, PUT | `/api/users/me` | View/update own profile |
| POST | `/api/users/me/avatar`, `/api/users/me/cover` | Upload own profile images (`file` multipart field) |
| GET | `/api/users/search?q=...`, `/api/users/suggested` | Search/suggest users |
| GET | `/api/users/{username}` | View profile |
| POST, DELETE | `/api/users/{id}/follow` | Follow/unfollow |
| POST, DELETE | `/api/users/{id}/block` | Block/unblock an account |
| GET | `/api/users/me/blocked` | List accounts you have blocked |
| GET | `/api/users/{id}/followers`, `/api/users/{id}/following` | Paginated relationships |
| POST | `/api/reports/with-evidence` | Submit a report with up to 3 screenshots or recordings |
| GET | `/api/reports/{reportId}/evidence/{evidenceId}` | Read evidence as the reporter or an administrator |
| GET, PUT | `/api/users/me/settings` | Privacy/activity/notification settings |
| DELETE | `/api/users/me` | Delete account (requires password confirmation) |
| POST | `/api/posts` | Create post (`caption` and optional `image` multipart fields) |
| GET | `/api/posts/feed`, `/api/posts/explore` | Paginated feed/discovery |
| GET | `/api/posts/{id}`, `/api/posts/user/{userId}`, `/api/posts/saved` | Post detail, profile posts, bookmarks |
| DELETE | `/api/posts/{id}` | Delete own post |
| POST, DELETE | `/api/posts/{id}/like`, `/api/posts/{id}/save` | Like/unlike and bookmark/unbookmark |
| GET, POST | `/api/posts/{id}/comments` | Paginated comments and add comment |
| GET | `/api/notifications`, `/api/notifications/unread-count` | Notification list/count |
| POST | `/api/notifications/{id}/read`, `/api/notifications/read-all` | Mark notifications read |
| GET | `/api/messages` | Paginated conversations |
| POST | `/api/messages/start/{userId}` | Start/reopen a direct conversation |
| GET, POST | `/api/messages/{conversationId}` | Paginated messages/send message |
| POST | `/api/messages/{conversationId}/read` | Mark incoming messages read |

Authentication endpoints are rate-limited to 30 requests per minute per client IP per application instance. For multi-instance deployments, put a shared rate limiter at the gateway/load balancer.

### Admin

The admin control room has its own `/admin` frontend route and `/api/admin/**` backend API, but is currently deployed as part of the existing React app and Spring Boot service rather than as separately deployable applications. The default admin username is `pacadmin`; set `PACGRAM_ADMIN_PASSWORD` in the backend process environment or deployment secret manager to provision the account. The password is BCrypt-hashed in the existing `users` table with an `ADMIN` role and is never embedded in source. When the variable is set, startup synchronizes the existing admin password hash to the configured value; leaving it empty does not reset an existing password. If the account has not been provisioned and the variable is empty, the backend still starts for regular users and logs a warning; admin sign-in remains unavailable until configured. If the configured username belongs to a regular account, startup stops with an explicit error rather than changing that account's permissions.

The overview reports total users, users active in the last 90 seconds, offline/suspended counts, distinct accounts with reports, block relationships, and report statuses; it refreshes every 30 seconds. Admins can search users by username, name, or email, open an account dossier with profile/privacy details, posts and engagement, reports made/received, and who blocked whom, suspend/reactivate accounts, review/resolve/dismiss reports, and inspect an audit log of admin actions. User activity is paginated. Normal users cannot access these APIs. Private message content is not generally searchable or visible to admins; a message is visible to moderators only when a user explicitly attaches it to a report.

Reports require an explanation of 10–1000 characters. Optional evidence accepts JPEG, PNG, GIF, MP4, or WebM files, up to 3 attachments and 20 MB per file. Evidence is stored outside the public media directory under `REPORT_EVIDENCE_DIR` (default `./private-report-evidence`) and is served only through authenticated, ownership-checked report/admin endpoints. Submitting a report automatically blocks the reported account and removes any mutual follow relationship. Either user can block or unblock an account; blocked accounts are excluded from profile/search/follow lists, posts, and direct conversations. Manage your own blocked list under account settings.

## Configuration

All application settings are in `src/main/resources/application.yml`; there are no environment-specific Spring profiles. The SQL connection defaults to `localhost:1433`, database `pacgram`, and login `pacgram`. Override `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` through environment variables when needed. Set `JWT_SECRET` to a unique random value of at least 32 characters. Configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, and `SMTP_STARTTLS` for password-reset email delivery. Do not expose reset tokens in API responses or logs.

## Tests

```powershell
mvn test
```

The integration tests use an isolated in-memory H2 database. Development and production use SQL Server and the Flyway migrations in `src/main/resources/db/migration`.
