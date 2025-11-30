CREATE TABLE video_format_conversion_job
(
    id                 VARCHAR(255) PRIMARY KEY,
    video_id           VARCHAR(255) NOT NULL,
    converted_video_id VARCHAR(255),
    FOREIGN KEY (id) REFERENCES job (id) ON DELETE CASCADE,
    FOREIGN KEY (video_id) REFERENCES video (id),
    FOREIGN KEY (converted_video_id) REFERENCES video (id)
);


CREATE INDEX idx_video_format_conversion_job_parent_id ON video_format_conversion_job (video_id);
CREATE INDEX idx_video_format_conversion_job_converted_id ON video_format_conversion_job (converted_video_id);