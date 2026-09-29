# Deployment checklist

## Render

1. Create/import the repository in Render and deploy the Blueprint.
2. Set the server secret `TELEGRAM_BOT_TOKEN`.
3. Confirm `ALLOWED_ORIGIN` is the exact GitHub Pages origin.
4. Confirm `/api/health` returns JSON with `ok: true`.
5. The frontend uses the Render API fallback configured in `index.html` unless `window.FREEZZ_CHAT_API` is supplied.
6. Run the database migration before starting the API.

## Telegram Mini App

1. Configure the Mini App URL in BotFather.
2. The Mini App loads the official Telegram WebApp SDK.
3. The client sends only Telegram `initData` to `POST /api/auth/telegram/session`.
4. The server validates the signature and creates the secure session cookie.
5. All subsequent chat/profile/stats requests use that cookie.

## Security

- Never put the bot token in the GitHub Pages files.
- Never store the session token in localStorage.
- Keep HTTPS enabled on the API.
- The application database is PostgreSQL; Telegram is not used as a message archive.
