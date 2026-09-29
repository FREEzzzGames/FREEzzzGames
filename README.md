# FREEzzzGames

FREEzzzGames arcade portal.

## Architecture

- Static portal: GitHub Pages.
- API: Node.js + PostgreSQL on Render.
- Telegram Mini App: official Telegram WebApp SDK on the client.
- Authentication: the browser sends Telegram `initData` to the API; the server validates the Telegram signature and creates an HttpOnly Secure SameSite=None session cookie.
- The Telegram bot token never reaches the browser.
- API requests use the secure session cookie; no bearer token is stored in localStorage.
- The old Telegram archive worker, archive topic configuration and redirect page have been removed.
