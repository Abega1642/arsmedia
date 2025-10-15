CREATE TABLE auth_code
(
    id         VARCHAR PRIMARY KEY,
    user_id    VARCHAR                  NOT NULL,
    code       VARCHAR(5)               NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deadline   TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id),
    UNIQUE (code, created_at)
);