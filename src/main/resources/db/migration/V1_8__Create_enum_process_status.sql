CREATE TYPE process_status AS ENUM (
    'PENDING',
    'FAILED',
    'PROGRESSING',
    'COMPLETED'
    );