DROP TABLE public.users_new CASCADE;
DROP TABLE public.users CASCADE;

CREATE TABLE public.users (
    id SERIAL PRIMARY KEY, -- Automatically incrementing ID
    create_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    discord_id BIGINT UNIQUE, -- Using discord_id with a unique constraint
    exp NUMERIC DEFAULT 0 NOT NULL,
    faction_name VARCHAR(255) DEFAULT NULL,
    level INTEGER DEFAULT 1 NOT NULL,
    minecraft_id VARCHAR(255), -- New field added
    patreon_about TEXT,
    patreon_amount_cents INTEGER,
    patreon_email VARCHAR(255),
    patreon_id BIGINT,
    patreon_name VARCHAR(255), -- New field added
    patreon_status VARCHAR(50),
    patreon_tier VARCHAR(255),
    patreon_vanity VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS public.moderation_counts (
    user_id BIGINT PRIMARY KEY REFERENCES public.users(discord_id) ON DELETE CASCADE,
    flagged_count INTEGER DEFAULT 0 NOT NULL
);

-- Alter users table to add moderation columns
ALTER TABLE public.users
ADD COLUMN IF NOT EXISTS is_muted BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS mute_until TIMESTAMPTZ DEFAULT NULL,
ADD COLUMN IF NOT EXISTS warning_count INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS is_banned BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS ban_until TIMESTAMPTZ DEFAULT NULL;

ALWAYS ADD COLUMN IF NOT EXISTS
