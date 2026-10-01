DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled boolean;
        UPDATE users SET enabled = true WHERE enabled IS NULL;
        ALTER TABLE users ALTER COLUMN enabled SET DEFAULT true;
        ALTER TABLE users ALTER COLUMN enabled SET NOT NULL;

        ALTER TABLE users ADD COLUMN IF NOT EXISTS phone varchar(30);

        ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at timestamp with time zone;
        UPDATE users SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
        ALTER TABLE users ALTER COLUMN created_at SET NOT NULL;

        ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at timestamp with time zone;
        UPDATE users SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;
        ALTER TABLE users ALTER COLUMN updated_at SET NOT NULL;

        ALTER TABLE users ADD COLUMN IF NOT EXISTS version bigint;
        UPDATE users SET version = 0 WHERE version IS NULL;
        ALTER TABLE users ALTER COLUMN version SET DEFAULT 0;
        ALTER TABLE users ALTER COLUMN version SET NOT NULL;
    END IF;
END
$$;
