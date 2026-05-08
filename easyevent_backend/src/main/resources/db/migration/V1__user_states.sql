CREATE TABLE user_states (
    id SMALLSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

INSERT INTO user_states (id, name) VALUES
    (1, 'ACTIVE'),
    (2, 'INACTIVE'),
    (3, 'BLOCKED'),
    (4, 'BANNED');