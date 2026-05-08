-- Ban Logs
CREATE TABLE user_ban_log (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    action_type TEXT NOT NULL CHECK (action_type IN ('BAN', 'UNBAN')),
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES users(id)
);

-- Deletion Logs
CREATE TABLE user_deletion_log (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    deletion_type TEXT NOT NULL CHECK (deletion_type IN ('SELF', 'ADMIN')),
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Recovery Logs
CREATE TABLE user_recovery_log (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT NOT NULL,
    recovered_by_id BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Automated logic for Bans
CREATE OR REPLACE FUNCTION fn_sync_user_ban_state()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.action_type = 'BAN' THEN
        UPDATE users SET state_id = 3 WHERE id = NEW.target_user_id;
    ELSIF NEW.action_type = 'UNBAN' THEN
        UPDATE users SET state_id = 1 WHERE id = NEW.target_user_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_after_ban_log_insert
AFTER INSERT ON user_ban_log
FOR EACH ROW EXECUTE FUNCTION fn_sync_user_ban_state();