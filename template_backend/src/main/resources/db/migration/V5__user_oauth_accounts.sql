CREATE TABLE user_oauth_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider_id SMALLINT NOT NULL,
    provider_user_id TEXT NOT NULL, -- The 'sub' claim
    email TEXT,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ DEFAULT NULL,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (provider_id) REFERENCES oauth_providers(id) ON DELETE CASCADE,

    UNIQUE (provider_id, provider_user_id),
    UNIQUE (user_id, provider_id)
);

CREATE INDEX idx_user_oauth_accounts_user_id ON user_oauth_accounts(user_id);