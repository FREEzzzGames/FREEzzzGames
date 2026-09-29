CREATE TABLE IF NOT EXISTS players (
 telegram_id BIGINT PRIMARY KEY, username TEXT NOT NULL DEFAULT '', first_name TEXT NOT NULL DEFAULT '', last_name TEXT NOT NULL DEFAULT '',
 avatar TEXT NOT NULL DEFAULT '👾', language_code TEXT NOT NULL DEFAULT '', created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), last_seen TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE TABLE IF NOT EXISTS sessions (token_hash TEXT PRIMARY KEY, telegram_id BIGINT NOT NULL REFERENCES players(telegram_id) ON DELETE CASCADE, expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL DEFAULT NOW());
CREATE TABLE IF NOT EXISTS room_messages (
 id BIGSERIAL PRIMARY KEY, room TEXT NOT NULL CHECK(room IN ('main','games','relax')), telegram_id BIGINT NOT NULL REFERENCES players(telegram_id) ON DELETE CASCADE,
 text TEXT NOT NULL CHECK(char_length(text) BETWEEN 1 AND 500), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS room_messages_room_id_idx ON room_messages(room,id DESC);
CREATE TABLE IF NOT EXISTS dm_conversations (player_low BIGINT NOT NULL, player_high BIGINT NOT NULL, updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), PRIMARY KEY(player_low,player_high), CHECK(player_low<player_high));
CREATE TABLE IF NOT EXISTS dm_messages (
 id BIGSERIAL PRIMARY KEY, player_low BIGINT NOT NULL, player_high BIGINT NOT NULL, sender_id BIGINT NOT NULL REFERENCES players(telegram_id) ON DELETE CASCADE,
 text TEXT NOT NULL CHECK(char_length(text) BETWEEN 1 AND 500), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS dm_messages_pair_id_idx ON dm_messages(player_low,player_high,id DESC);
CREATE TABLE IF NOT EXISTS player_stats (
 telegram_id BIGINT PRIMARY KEY REFERENCES players(telegram_id) ON DELETE CASCADE, portal_seconds DOUBLE PRECISION NOT NULL DEFAULT 0, game_seconds DOUBLE PRECISION NOT NULL DEFAULT 0,
 chat_seconds DOUBLE PRECISION NOT NULL DEFAULT 0, game_launches INTEGER NOT NULL DEFAULT 0, messages_sent INTEGER NOT NULL DEFAULT 0,
 category_opens INTEGER NOT NULL DEFAULT 0, game_views INTEGER NOT NULL DEFAULT 0, active_days TEXT[] NOT NULL DEFAULT '{}', updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Legacy Telegram archive metadata removed by the new authentication architecture.
ALTER TABLE room_messages DROP COLUMN IF EXISTS telegram_message_id;
DROP TABLE IF EXISTS telegram_topics;
