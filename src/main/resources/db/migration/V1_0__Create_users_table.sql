CREATE TYPE user_role AS ENUM (
    'USER',
    'ADMIN'
    );


CREATE TABLE users
(
    id                     VARCHAR PRIMARY KEY,
    email                  VARCHAR NOT NULL,
    pseudo                 VARCHAR NOT NULL,
    phone_number           VARCHAR,
    img_profile_bucket_key TEXT    NOT NULL,
    password               TEXT    NOT NULL,
    user_role              user_role                DEFAULT 'USER',
    is_activated           BOOLEAN NOT NULL         DEFAULT FALSE,
    created_at             TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at             TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);