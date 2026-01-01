# Arsmedia API

<p align="center">
  <strong>Arsmedia API</strong><br/>
</p>

<p align="center">
  <a href="https://github.com/Abega1642/arsmedia.git/actions/workflows/ci-test.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/arsmedia/ci-test.yml?label=tests&style=for-the-badge" />
  </a>
  <a href="https://github.com/Abega1642/arsmedia/actions/workflows/ci-build.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/Abega1642/arsmedia/ci-build.yml?label=build&style=for-the-badge" />
  </a>
  <a href="https://github.com/Abega1642/arsmedia/blob/preprod/LICENSE">
    <img src="https://img.shields.io/github/license/Abega1642/arsmedia?style=for-the-badge" />
  </a>
  <img src="https://img.shields.io/badge/java-21-007396?style=for-the-badge" />
  <img src="https://img.shields.io/badge/spring%20boot-3.6.9-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
</p>

<p align="center">
  <img src="https://skillicons.dev/icons?i=spring,docker,gradle,java,postgres,rabbitmq,gmail,bitbucket&theme=light" />
</p>

---

## Introduction

**Arsmedia** is a high-performance, enterprise-grade multimedia processing backend built with Spring Boot.  
It provides a comprehensive set of media transformation capabilities for **video, audio, and image** content, with a strong focus on scalability, security, and clean integration.

The platform is designed to serve as a reliable backend for media-intensive applications, supporting complex processing pipelines while maintaining predictable performance and operational stability.

---

## Core Capabilities

### Video Processing

- Format conversion (AVI → MP4, WebM → MOV, and 50+ additional formats)
- Codec transcoding (H.264, H.265, VP9, AV1) with quality optimization
- Audio stream manipulation (extract, replace, merge)
- Non-destructive editing (cut, trim, merge without re-encoding)
- Visual transformations (resize, crop, rotate, mirror, flip)
- Subtitle and overlay handling (burn-in or separate streams)
- Advanced filtering (blur, sharpen, grayscale, watermark, custom effects)
- Intelligent compression with bitrate and quality control

### Audio Processing

- Multi-format support (MP3, WAV, AAC, FLAC, OGG, and more)
- Bitrate adjustment and sample-rate conversion
- Channel mixing and audio enhancement
- Audio extraction from video sources
- Effects processing (normalization, volume control, echo, fade, equalizer)
- Precise mixing and composition of multiple tracks

### Image Processing

- Format conversion (JPEG, PNG, WebP, AVIF, and 20+ formats)
- Frame extraction from videos at configurable intervals
- Media generation (videos and GIFs from image sequences)
- Image resizing, cropping, filtering, and enhancement
- Automatic thumbnail generation with smart cropping

---

## Architecture & Technology Stack

| Component        | Technology                          |
|------------------|-------------------------------------|
| Language         | Java 21                              |
| Framework        | Spring Boot 3.5.9                    |
| Build System     | Gradle                               |
| Database         | PostgreSQL with Flyway migrations    |
| Authentication  | JWT, API Keys, Client Credentials   |
| Media Engine    | FFmpeg, Jaffree                      |
| Storage          | AWS S3 (Transfer Manager)            |
| Email            | Resend Java SDK                      |
| File Analysis    | Apache Tika                          |
| Messaging        | RabbitMQ                             |
| Testing          | Testcontainers, JUnit 5, GreenMail   |

---

## Security Model

Arsmedia implements a layered security approach to protect both infrastructure and media operations.

### Client Authentication

All non-public endpoints require client authentication via headers:

```http
X-CLIENT-ID: <client_id>
X-CLIENT-SECRET: <client_secret>
````

For local development, secret validation may be relaxed to simplify testing.

### User Authentication (JWT)

JWT access tokens are issued via the authentication endpoint:

```http
POST /auth/token/token-pairs
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "secure_password"
}
```

### Administrative Endpoints

Administrative operations require **triple authentication**:

* Valid API key
* Client credentials
* JWT bearer token with administrative privileges

---

## Getting Started

### Prerequisites

* Java 21 or newer
* PostgreSQL 16 or newer
* FFmpeg 6.0 or newer
* Gradle 8.0 or newer

### Installation & Deployment

Clone the repository:

```bash
git clone https://github.com/Abega1642/arsmedia.git
cd arsmedia
```

Configure environment variables:

```bash
cp .env.template .env
# Update values as needed
```

Build the application:

```bash
./gradlew clean build
```

Run using Docker:

```bash
docker build -t arsmedia .
docker run -p 8080:8080 --env-file .env arsmedia
```

### Development Workflow

Run tests with coverage:

```bash
./gradlew testWithCoverage
```

Apply code formatting:

```bash
./format.sh
```

Run locally:

```bash
./gradlew bootRun
```

---

## Quality & Reliability

### Testing Strategy

* Unit, integration, and end-to-end tests
* Real infrastructure testing using Testcontainers
* Minimum line coverage threshold enforced (50%)
* JaCoCo reporting and verification

### Code Quality

* Static analysis via Qodana
* Google Java Format enforcement
* Dependency and security vulnerability scanning

---

## Production Deployment

| Environment | URL                                                            | Status |
| ----------- | -------------------------------------------------------------- | ------ |
| Production  | [https://arsmedia.onrender.com](https://arsmedia.onrender.com) | Active |

### Monitoring

* Spring Boot Actuator enabled
* Health checks and metrics endpoints
* Custom metrics for media processing operations

---

## API Documentation

### Interactive Documentation

* Swagger UI available at `/swagger-ui.html`
* OpenAPI 3.0 specification located at `doc/api.yaml`
* Postman collections can be generated from the OpenAPI definition

### Example Request

```http
POST /api/v1/video/convert
Content-Type: multipart/form-data
X-CLIENT-ID: your-client-id
X-CLIENT-SECRET: your-client-secret

{
  "file": [binary data],
  "targetFormat": "mp4",
  "quality": "high",
  "outputResolution": "1080p"
}
```

---

## Project Foundation

Arsmedia is built on top of the **ar-infra-template**, a production-ready Spring Boot infrastructure and architecture template.

For detailed architectural conventions and project structure, refer to:
[https://github.com/Abega1642/ar-infra-template.git](https://github.com/Abega1642/ar-infra-template.git)

---

## Maintainer & Contact

**BackOps Engineer:** Abegà Razafindratelo
**Email:** [a.razafindratelo@gmail.com](mailto:a.razafindratelo@gmail.com)
**GitHub:** [https://github.com/Abega1642](https://github.com/Abega1642)

---

## License

This project is licensed under the MIT License.
See the `LICENSE` file for details.

---

## Support

If this project is useful to you, consider giving it a star on GitHub.

<p>
  <a href="https://github.com/Abega1642/arsmedia">
    <img src="https://img.shields.io/github/stars/Abega1642/arsmedia?style=social" />
  </a>
</p>
