# 🎬 Arsmedia Backend API

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?style=for-the-badge)
![Build](https://img.shields.io/badge/Build-Gradle-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-purple?style=for-the-badge)
![Status](https://img.shields.io/badge/Status-Active-success?style=for-the-badge)

A **high-performance multimedia processing backend** built with **Spring Boot**, supporting advanced operations on *
*videos**, **audios**, and **images**. Designed for speed, extensibility, and secure client integration.

---

## 🚀 Features

### 🎥 **Video Capabilities**

* Format conversion (.avi → .mp4, .webm → .mov, ...)
* Codec switching (H.264, H.265, VP9, AV1)
* Extract/replace audio tracks
* Cut/trim/merge videos **without re-encoding**
* Resize, crop, rotate, mirror
* Burn subtitles or attach them
* Filters: blur, sharpen, grayscale, watermark
* **Video compression** with configurable quality

### 🎵 **Audio Capabilities**

* Convert .mp3/.wav/.aac/.flac, etc.
* Change bitrate, sample rate, channels
* Extract audio from video
* Normalize, adjust volume
* Effects: echo, fade in/out, equalizer
* Mix multiple audio tracks

### 🖼️ **Image Capabilities**

* Convert between image formats
* Extract frames from videos
* Create videos/GIFs from images
* Resize, crop, apply filters
* Generate thumbnails

---

## 🔐 Authentication & Security

Arsmedia implements **three-layer security**:

### 1️⃣ **Client Authentication**

Required for all non-public endpoints.

```
X-CLIENT-ID: <client_id>
X-CLIENT-SECRET: <client_secret>
```

Local requests (`localhost`, `127.0.0.1`) bypass the secret validation.

### 2️⃣ **User Authentication (JWT)**

Obtain JWT from:

```
POST /auth/token/token-pairs
```

Used for user-protected endpoints.

### 3️⃣ **Admin Endpoints**

Require:

* API Key
* Client credentials
* JWT Bearer token

---

## 🏗️ Tech Stack

| Component    | Technology                        |
|--------------|-----------------------------------|
| Language     | Java 21                           |
| Framework    | Spring Boot 3.5                   |
| Build        | Gradle                            |
| Auth         | JWT, API Keys, Client Credentials |
| Media Engine | FFmpeg / custom wrappers          |
| Deployment   | Koyeb (Production)                |

---

## 🌍 API Servers

| Environment    | URL                                                        |
|----------------|------------------------------------------------------------|
| **Production** | `https://wooden-cristie-razafindratelo-9e0e4bcf.koyeb.app` |

OpenAPI specification is included in the repository.

---

## 📦 Project Structure

```
arsmedia/
 ├── build/
 ├── build.gradle
 ├── Dockerfile
 ├── .env.template
 ├── doc/
 ├── format.sh
 ├── google-java-format-1.28.0-all-deps.jar
 ├── qodana.yaml
 ├── settings.gradle
 ├── src/
 │   └── main/java/dev/razafindratelo/arsmedia/
 │        ├── ArsmediaApplication.java
 │        ├── config/
 │        ├── datastructure/
 │        ├── endpoint/
 │        ├── event/
 │        ├── exception/
 │        ├── file/
 │        ├── mail/
 │        ├── mapper/
 │        ├── model/
 │        ├── repository/
 │        └── service/
```

---

## ▶️ Running the Application

### **1. Clone the repository**

```

git clone [https://github.com/Abega1642/arsmedia.git](https://github.com/Abega1642/arsmedia.git)
cd arsmedia

```

---

## 📜 API Documentation

The full API definition is available in [api.yaml](/doc/api.yaml) and can be viewed using:

- **Swagger Editor**
- **Postman**
- **Stoplight Studio**

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first.

---

## 📧 Contact

**Abegà Razafindratelo**  
📩 a.razafindratelo@gmail.com  
🔗 GitHub: https://github.com/Abega1642

---

## ⭐ If you like this project

Consider starring the repo to support the development!

![Stars](https://img.shields.io/github/stars/Abega1642/arsmedia-api?style=social)
