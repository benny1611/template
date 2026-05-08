CREATE TABLE oauth_providers (
    id SMALLSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

INSERT INTO oauth_providers (id, name) VALUES
    (1, 'GOOGLE'),
    (2, 'MICROSOFT');