CREATE TABLE job
(
    id             VARCHAR(255) PRIMARY KEY,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at   TIMESTAMP WITH TIME ZONE,
    process_status process_status           NOT NULL DEFAULT 'PENDING',
    error_message  VARCHAR(1000),
    attempt_count  INTEGER                  NOT NULL DEFAULT 0
);

CREATE TABLE audio_extraction_job
(
    id                 VARCHAR(255) PRIMARY KEY,
    video_id           VARCHAR(255) NOT NULL,
    extracted_audio_id VARCHAR(255),
    FOREIGN KEY (id) REFERENCES job (id) ON DELETE CASCADE,
    FOREIGN KEY (video_id) REFERENCES video (id),
    FOREIGN KEY (extracted_audio_id) REFERENCES audio (id)
);

ALTER TABLE compressed_videos
    RENAME TO video_compression_job;

INSERT INTO job (id, created_at, completed_at, process_status, error_message, attempt_count)
SELECT vcj.id,
       vcj.created_at,
       vcj.completed_at,
       COALESCE(vcj.process_status, 'PENDING'),
       vcj.error_message,
       COALESCE(vcj.attempt_count, 0)
FROM video_compression_job vcj;

ALTER TABLE video_compression_job
    DROP COLUMN created_at,
    DROP COLUMN completed_at,
    DROP COLUMN process_status,
    DROP COLUMN error_message,
    DROP COLUMN attempt_count;

ALTER TABLE video_compression_job
    ADD CONSTRAINT fk_video_compression_job_job
        FOREIGN KEY (id) REFERENCES job (id) ON DELETE CASCADE;

CREATE INDEX idx_job_status ON job (process_status);
CREATE INDEX idx_job_created_at ON job (created_at);
CREATE INDEX idx_audio_extraction_video_id ON audio_extraction_job (video_id);
CREATE INDEX idx_audio_extraction_audio_id ON audio_extraction_job (extracted_audio_id);
CREATE INDEX idx_video_compression_job_parent_id ON video_compression_job (video_id);
CREATE INDEX idx_video_compression_job_compressed_id ON video_compression_job (compressed_video_id);