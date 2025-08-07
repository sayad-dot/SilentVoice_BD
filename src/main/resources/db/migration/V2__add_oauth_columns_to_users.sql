-- V2__add_oauth_columns_to_users.sql

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS oauth_provider VARCHAR(50)   DEFAULT 'local',
  ADD COLUMN IF NOT EXISTS oauth_id VARCHAR(255),
  ADD COLUMN IF NOT EXISTS profile_picture_url VARCHAR(500),
  ADD COLUMN IF NOT EXISTS email_verified BOOLEAN       DEFAULT FALSE;

-- Backfill only if null
UPDATE users
  SET oauth_provider = 'local'
  WHERE oauth_provider IS NULL;
