CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL, -- Uniqueness handled by index below
    password TEXT,       -- Nullable for OAuth-only users
    profile_picture_url TEXT,
    language VARCHAR(5) DEFAULT 'en',

    -- Security & State
    state_id SMALLINT NOT NULL DEFAULT 1,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMPTZ,

    -- Activation
    activation_token UUID UNIQUE,
    activation_sent_at TIMESTAMPTZ,

    -- Soft Delete
    deleted_at TIMESTAMPTZ DEFAULT NULL,

    CONSTRAINT fk_users_state FOREIGN KEY (state_id) REFERENCES user_states(id)
);

-- Index for soft-delete aware uniqueness
CREATE UNIQUE INDEX idx_users_email_active ON users(email) WHERE (deleted_at IS NULL);
-- Index for login/lookup performance
CREATE INDEX idx_users_state_id ON users(state_id);
CREATE INDEX idx_users_deleted_at ON users(deleted_at) WHERE deleted_at IS NOT NULL;