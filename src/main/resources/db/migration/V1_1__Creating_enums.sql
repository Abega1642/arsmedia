CREATE TYPE size_type AS ENUM (
    'BYTES',
    'KB',
    'MB',
    'GB',
    'TB',
    'PB',
    'EB'
    );

CREATE TYPE file_type AS ENUM (
    'VIDEO',
    'IMAGE',
    'AUDIO'
    );

CREATE TYPE video_codec AS ENUM (
    'H264',
    'H265',
    'VP8',
    'VP9',
    'AV1',
    'MPEG2',
    'MPEG4',
    'THEORA',
    'WMV',
    'DV',
    'PRORES',
    'CINEFORM',
    'DNXHD',
    'UNKNOWN'
    );

CREATE TYPE audio_codec AS ENUM (
    'NONE',
    'AAC',
    'MP3',
    'AC3',
    'EAC3',
    'DTS',
    'FLAC',
    'PCM',
    'OPUS',
    'VORBIS',
    'WMA',
    'ALAC',
    'AMR',
    'G711',
    'G722',
    'UNKNOWN'
    );

CREATE TYPE container_format AS ENUM (
    'MP4',
    'MKV',
    'MOV',
    'AVI',
    'FLV',
    'WMV',
    'WEBM',
    'MPEG_TS',
    'MPEG_PS',
    'THREEGP',
    'OGG',
    'M4A',
    'WAV',
    'FLAC',
    'UNKNOWN'
    );

CREATE TYPE image_format AS ENUM (
    'JPEG',
    'PNG',
    'GIF',
    'BMP',
    'TIFF',
    'WEBP',
    'HEIF',
    'HEIC',
    'RAW',
    'SVG',
    'ICO',
    'UNKNOWN'
    );