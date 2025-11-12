CREATE TYPE process_status AS ENUM (
    'FAILED',
    'PROGRESSING',
    'COMPLETED'
    );

ALTER TABLE compressed_videos
    ADD COLUMN process_status process_status DEFAULT 'PROGRESSING';