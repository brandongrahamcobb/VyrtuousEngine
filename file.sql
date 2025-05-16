DROP TABLE public.users_new CASCADE;
DROP TABLE public.users CASCADE;

CREATE TABLE public.users (
    id SERIAL PRIMARY KEY, -- Automatically incrementing ID
    create_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    discord_id BIGINT UNIQUE, -- Using discord_id with a unique constraint
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
