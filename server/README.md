# FREEzzzGames API backend

The Node.js backend provides player accounts, chat, profiles and statistics for the portal.

## Architecture

- Telegram Mini App sends official Telegram `initData` to the backend.
- The backend validates the HMAC signature and rejects stale authorization data.
- A random server-side session is stored as a hash in PostgreSQL.
- The browser receives only an HttpOnly Secure SameSite=None session cookie.
- No bearer session token is exposed to JavaScript or localStorage.
- Telegram is the identity provider; PostgreSQL is the application database.
- Public-room messages are stored only in PostgreSQL. The old Telegram archive mirror has been removed.

## Setup

1. Create PostgreSQL.
2. Run `psql "$DATABASE_URL" -f schema.sql`.
3. Fill `.env` from `.env.example`.
4. Set the Telegram bot token only on the server.
5. `npm install && npm start`.
6. Put the API behind HTTPS.
7. Configure the Mini App URL in BotFather.
8. Keep `ALLOWED_ORIGIN` equal to the actual portal origin.

Before a public launch, add moderation/report/block endpoints and distributed rate limiting.
