# Deployment checklist

## Render

The repository contains a `render.yaml` Blueprint for the API and PostgreSQL database.

1. Create/import the repository in Render and deploy the Blueprint.
2. Set the secret `TELEGRAM_BOT_TOKEN`.
3. Set `TELEGRAM_ARCHIVE_CHAT_ID`.
4. Set the three forum topic IDs:
   - `TG_ROOM_MAIN_THREAD_ID`
   - `TG_ROOM_GAMES_THREAD_ID`
   - `TG_ROOM_RELAX_THREAD_ID`
5. Add the Telegram bot to the archive supergroup and allow it to post in all three topics.
6. Confirm `/api/health` returns JSON with `ok: true`.
7. The frontend is configured to use the Render API URL fallback; if the service name or URL is changed, update `CHAT_API_BASE` in `index.html`.

## Telegram forum

Create one Telegram supergroup with Topics enabled and three topics named:
- 🏠 Основная
- 🎮 Игры
- 🌙 Relax

The IDs of those topics are the values used by the three thread environment variables.

## Important

The database is application storage. Telegram receives a mirror of public-room messages. Private messages are not copied into the public archive.
