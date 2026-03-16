-- Переход на общий бот: убираем per-hotel bot token

DROP INDEX IF EXISTS idx_hotels_bot_token;
ALTER TABLE hotels DROP COLUMN IF EXISTS telegram_bot_token;
