# Tea attendance API

Spring Boot + MySQL backend for recording daily drinks, who paid, weekly Saturday settlement, payment QR links, notifications, and group chat.

## Start

1. Create a MySQL user that can create/use the `tea_attendance` database, or create it manually.
2. Set optional environment variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
3. From this folder run `mvn spring-boot:run`.

The API starts at `http://localhost:8081/api`. The React Vite dev server at `http://localhost:5173` is allowed by default. Schema migrations run automatically through Flyway.

## Deploy without CORS errors

The frontend can be deployed in either of these ways:

1. **Same domain:** publish the API behind `/api` on the same domain as the frontend. The production frontend uses `/api` automatically, so the browser makes no cross-origin request.
2. **Separate domains:** set these two build/runtime variables with your real HTTPS domains (do not add a trailing slash):

```text
VITE_API_URL=https://api.example.com/api
CORS_ALLOWED_ORIGINS=https://app.example.com,https://www.app.example.com
```

`CORS_ALLOWED_ORIGINS` is an exact, comma-separated allow-list. Do not use `*`, because the API accepts authenticated requests. Restart the backend after changing it, and rebuild the frontend after changing `VITE_API_URL`.

## Main routes

- `GET/POST/PUT /api/members`
- `GET/POST/PUT/DELETE /api/expenses` and `GET/POST /api/attendances`
- `GET/POST /api/weeks/{weekStart}/settlement`, plus `/calculate` and `/finalize`
- `POST /api/settlement-transfers/{id}/mark-paid`
- `GET/POST /api/chat/messages`
- `GET /api/notifications?memberId=...`

Every Saturday at 18:00 (Vietnam time), the API calculates and finalizes that Monday–Saturday period. The scheduler can be disabled with `APP_SCHEDULER_ENABLED=false`.

For a recipient's real VietQR image, supply their bank BIN and account number when creating/updating a member. A transfer response contains a ready-to-render `qrImageUrl`.
