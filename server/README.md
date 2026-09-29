# FREEzzzGames Telegram Chat backend

GitHub Pages is static hosting, so this Node.js backend must run separately with PostgreSQL. The portal calls `/api` by default; with a separate API host, set `window.FREEZZ_CHAT_API` to that API's `/api` base before the portal script.

## Features
- Telegram Mini App authentication with server-side HMAC validation.
- Three public rooms: main, games, relax.
- PostgreSQL message history.
- Telegram forum-topic archive mirror for public rooms.
- Private 1-to-1 messages.
- Public profiles and server-side player stats.
- HttpOnly session cookie, message length/rate limits.

## Setup
1. Create PostgreSQL.
2. Run `psql "$DATABASE_URL" -f schema.sql`.
3. Fill `.env` from `.env.example`.
4. Add the bot to the Telegram archive supergroup and give it permission to post to the three forum topics.
5. `npm install && npm start`.
6. Put the API behind HTTPS.
7. Configure the Mini App URL in BotFather.
8. If API is separate from GitHub Pages, configure `window.FREEZZ_CHAT_API` and `ALLOWED_ORIGIN`.

Telegram is the archive mirror, not the application's database. The app needs its own indexed database for reliable history and private conversations. Public messages are mirrored to Telegram forum topics using Bot API `message_thread_id`.

Keep the bot token server-side. Add moderation/report/block endpoints and stronger distributed rate limiting before a public launch.
