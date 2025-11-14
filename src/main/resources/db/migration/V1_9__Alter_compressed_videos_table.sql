ALTER TABLE compressed_videos
    ADD COLUMN process_status      process_status DEFAULT 'PENDING',
    ADD COLUMN compressed_video_id VARCHAR(255),
    ADD COLUMN completed_at        TIMESTAMP,
    ADD COLUMN error_message       VARCHAR(1000),
    ADD COLUMN attempt_count       INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_compressed_video FOREIGN KEY (compressed_video_id) REFERENCES video (id);