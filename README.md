# 🎬 Arsmedia Backend API

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?style=for-the-badge)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?style=for-the-badge)
![AWS S3](https://img.shields.io/badge/AWS-S3-orange?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-purple?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Production-success?style=for-the-badge)

A **high-performance, enterprise-grade multimedia processing backend** built with Spring Boot, providing comprehensive media transformation capabilities for video, audio, and image content. Designed for scalability, security, and seamless integration.

---

## 🚀 Core Capabilities

### 🎥 **Video Processing**
- **Format Conversion**: AVI → MP4, WebM → MOV, and 50+ additional formats
- **Codec Transcoding**: H.264, H.265, VP9, AV1 with quality optimization
- **Stream Manipulation**: Extract, replace, or merge audio tracks
- **Non-destructive Editing**: Cut, trim, merge videos without re-encoding
- **Visual Transformations**: Resize, crop, rotate, mirror, and flip
- **Subtitles & Overlays**: Burn subtitles or attach as separate streams
- **Advanced Filters**: Blur, sharpen, grayscale, watermark, and custom effects
- **Intelligent Compression**: Quality-aware compression with bitrate control

### 🎵 **Audio Processing**
- **Multi-format Support**: MP3, WAV, AAC, FLAC, OGG, and more
- **Audio Enhancement**: Bitrate adjustment, sample rate conversion, channel mixing
- **Extraction Tools**: Isolate audio from video files
- **Audio Effects**: Normalization, volume adjustment, echo, fade, equalizer
- **Mixing & Composition**: Combine multiple audio tracks with precision

### 🖼️ **Image Processing**
- **Format Conversion**: JPEG, PNG, WebP, AVIF, and 20+ formats
- **Frame Extraction**: Capture frames from videos at customizable intervals
- **Media Creation**: Generate videos and GIFs from image sequences
- **Image Optimization**: Resize, crop, filter, and enhance images
- **Thumbnail Generation**: Automatic thumbnail creation with smart cropping

---

## 🏗️ Architecture & Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.6 |
| **Build System** | Gradle |
| **Database** | PostgreSQL with Flyway migrations |
| **Authentication** | JWT, API Keys, Client Credentials |
| **Media Engine** | FFmpeg, Jaffree |
| **Storage** | AWS S3 with Transfer Manager |
| **Email** | Resend Java SDK |
| **File Analysis** | Apache Tika |
| **Messaging** | RabbitMQ |
| **Testing** | TestContainers, JUnit 5, GreenMail |

---

## 🔐 Security Architecture

Arsmedia implements a **multi-layered security model**:

### 🔑 **Client Authentication**
Required for all non-public endpoints:
```http
X-CLIENT-ID: <your_client_id>
X-CLIENT-SECRET: <your_client_secret>
```
*Local development requests bypass secret validation for convenience*

### 🔒 **User Authentication (JWT)**
Obtain access tokens via:
```http
POST /auth/token/token-pairs
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "secure_password"
}
```

### 🛡️ **Admin Endpoints**
Require triple authentication:
- Valid API Key
- Client credentials
- JWT Bearer token with admin privileges

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- PostgreSQL 16+
- FFmpeg 6.0+
- Gradle 8.0+

### Installation & Deployment

1. **Clone the repository**
```bash
git clone https://github.com/Abega1642/arsmedia.git
cd arsmedia
```

2. **Configure environment variables**
```bash
cp .env.template .env
# Edit .env with your configuration
```

3. **Build the application**
```bash
./gradlew clean build
```

4. **Run with Docker**
```bash
docker build -t arsmedia .
docker run -p 8080:8080 --env-file .env arsmedia
```

### Development Setup

1. **Run tests with coverage**
```bash
./gradlew testWithCoverage
```

2. **Format code**
```bash
./format.sh
```

3. **Run locally**
```bash
./gradlew bootRun
```

---

## 📊 Quality Assurance

### Testing & Coverage
- **Comprehensive Test Suite**: Unit, integration, and end-to-end tests
- **TestContainers**: Real database and service testing
- **Code Coverage**: 50% minimum line coverage enforced
- **Quality Gates**: JaCoCo verification with detailed reporting

### Code Quality
- **Static Analysis**: Qodana configuration for code quality
- **Code Formatting**: Google Java Format enforced
- **Security Scanning**: Dependency vulnerability checks

---

## 🌍 Production Deployment

| Environment | URL | Status |
|-------------|-----|---------|
| **Production** | `https://wooden-cristie-razafindratelo-9e0e4bcf.koyeb.app` | ✅ Active |

### Monitoring & Health
- Spring Boot Actuator endpoints enabled
- Health checks, metrics, and environment info
- Custom media processing metrics

---

## 📚 API Documentation

### Interactive Documentation
- **Swagger UI**: Available at `/swagger-ui.html`
- **OpenAPI 3.0**: Full specification in `doc/api.yaml`
- **Postman Collection**: Import from OpenAPI spec

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

## 🏗️ Project Structure

```
arsmedia/
├── src/main/java/dev/razafindratelo/arsmedia/
│   ├── config/           # Spring configuration classes
│   ├── endpoint/         # REST API controllers
│   ├── service/          # Business logic layer
│   ├── repository/       # Data access layer
│   ├── model/           # Domain entities and DTOs
│   ├── mapper/          # Object mapping utilities
│   ├── file/            # File processing services
│   ├── mail/            # Email service integration
│   ├── event/           # Application events
│   ├── exception/       # Custom exception handling
│   └── datastructure/   # Custom data structures
├── doc/                 # API documentation
├── build.gradle         # Build configuration
├── Dockerfile          # Container definition
└── qodana.yaml         # Code quality configuration
```

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit your changes** (`git commit -m 'Add amazing feature'`)
4. **Push to the branch** (`git push origin feature/amazing-feature`)
5. **Open a Pull Request**

### Development Guidelines
- Write comprehensive tests for new features
- Maintain minimum 50% code coverage
- Follow Google Java Format style
- Update documentation accordingly

---

## 📞 Support & Contact

**Technical Lead**: Abegà Razafindratelo  
**Email**: a.razafindratelo@gmail.com  
**GitHub**: [https://github.com/Abega1642](https://github.com/Abega1642)

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## ⭐ Support the Project

If you find this project useful, please consider giving it a star on GitHub!

[![GitHub Stars](https://img.shields.io/github/stars/Abega1642/arsmedia?style=social)](https://github.com/Abega1642/arsmedia)

---

*Arsmedia - Professional Media Processing Backend*
