CREATE TABLE compressed_videos
(
    id         VARCHAR PRIMARY KEY,
    video_id   VARCHAR                  NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (video_id) REFERENCES video (id)
);