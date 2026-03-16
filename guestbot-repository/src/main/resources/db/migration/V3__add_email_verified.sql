-- Подтверждение email при регистрации
ALTER TABLE owners ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
