CREATE TYPE token_type as ENUM (
    'ACCESS_TOKEN',
    'REFRESH_TOKEN'
    );

CREATE TABLE token
(
    id         VARCHAR PRIMARY KEY,
    value      TEXT                     NOT NULL,
    creation   TIMESTAMP WITH TIME ZONE DEFAULT now(),
    expiration TIMESTAMP WITH TIME ZONE NOT NULL,
    is_valid   BOOLEAN                  DEFAULT false,
    user_id    VARCHAR                  NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);