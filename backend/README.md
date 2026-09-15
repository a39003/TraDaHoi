# Tea attendance API

Spring Boot + MySQL backend for recording daily drinks, who paid, weekly Saturday settlement, payment QR links, notifications, and group chat.

## Start

1. Create a MySQL user that can create/use the `tea_attendance` database, or create it manually.
2. Set optional environment variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
3. From this folder run `mvn spring-boot:run`.

The API starts at `http://localhost:8081/api`. The React Vite dev server at `http://localhost:5173` is allowed by default. Schema migrations run automatically through Flyway.

## Main routes

- `GET/POST/PUT /api/members`
- `GET/POST/PUT/DELETE /api/expenses` and `GET/POST /api/attendances`
- `GET/POST /api/weeks/{weekStart}/settlement`, plus `/calculate` and `/finalize`
- `POST /api/settlement-transfers/{id}/mark-paid`
- `GET/POST /api/chat/messages`
- `GET /api/notifications?memberId=...`

Every Saturday at 18:00 (Vietnam time), the API calculates and finalizes that Monday–Saturday period. The scheduler can be disabled with `APP_SCHEDULER_ENABLED=false`.

For a recipient's real VietQR image, supply their bank BIN and account number when creating/updating a member. A transfer response contains a ready-to-render `qrImageUrl`.
