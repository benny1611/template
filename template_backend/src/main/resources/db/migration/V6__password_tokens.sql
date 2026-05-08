CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY,
    token_hash TEXT NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_reset_tokens_valid ON password_reset_tokens (id) WHERE used = false;
CREATE INDEX idx_password_reset_tokens_expires_at ON password_reset_tokens (expires_at);