CREATE TABLE api_key
(
    id         VARCHAR PRIMARY KEY,
    user_id    VARCHAR                  NOT NULL,
    api_key    TEXT                     NOT NULL,
    creation   TIMESTAMP WITH TIME ZONE NOT NULL,
    expiration TIMESTAMP WITH TIME ZONE NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);