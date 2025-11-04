CREATE TABLE api_client
(
    id           VARCHAR PRIMARY KEY,
    client_name  VARCHAR NOT NULL,
    email        VARCHAR NOT NULL,
    phone_number VARCHAR NOT NULL
);

CREATE TABLE api_client_secret
(
    id            VARCHAR PRIMARY KEY,
    api_client_id VARCHAR NOT NULL,
    secret        TEXT    NOT NULL,

    FOREIGN KEY (api_client_id) REFERENCES api_client (id)
);