CREATE TABLE media
(
    id         VARCHAR PRIMARY KEY,
    file_name  TEXT             NOT NULL,
    size       DOUBLE PRECISION NOT NULL,
    size_type  size_type        NOT NULL,
    file_type  file_type        NOT NULL,
    created_at TIMESTAMP        NOT NULL DEFAULT NOW(),
    owner_id   VARCHAR          NOT NULL,
    bucket_key TEXT             NOT NULL,

    FOREIGN KEY (owner_id) REFERENCES users (id)
);


CREATE TABLE video
(
    id                VARCHAR PRIMARY KEY REFERENCES media (id) ON DELETE CASCADE,
    duration          DOUBLE PRECISION,
    codec             video_codec,
    width             INT,
    height            INT,
    frame_rate        DOUBLE PRECISION,
    aspect_ratio      TEXT,
    container_format  container_format,
    bit_rate          DOUBLE PRECISION,
    audio_codec       audio_codec,
    audio_channels    INT,
    audio_sample_rate INT
);

CREATE TABLE image
(
    id          VARCHAR PRIMARY KEY REFERENCES media (id) ON DELETE CASCADE,
    width       INT,
    height      INT,
    format      image_format,
    bit_depth   INT,
    color_model TEXT,
    dpi         DOUBLE PRECISION,
    iso         INT
);

CREATE TABLE audio
(
    id          VARCHAR PRIMARY KEY REFERENCES media (id) ON DELETE CASCADE,
    duration    DOUBLE PRECISION,
    bit_rate    INT,
    sample_rate INT,
    channels    INT,
    codec       audio_codec,
    format      container_format
);
