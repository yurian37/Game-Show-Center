# 🎮 Game Show Center

> **Interactive game show platform for education, live events, and classrooms.**  
> Features a 100% offline JavaFX desktop runtime with local RAG AI (PDF context), a React web showcase, and a modular mini-game architecture.

---

## 🌟 Overview

**Game Show Center** transforms any classroom, auditorium, corporate event, or family gathering into an authentic TV-style game show. It combines broadcast-quality aesthetics with pedagogical tools, allowing educators, hosts, and developers to run trivia, word challenges, and competitive rounds seamlessly.

### Key Highlights
- **100% Offline Desktop Runtime (`template-offline` v1.0.0)**: Zero-latency JavaFX 22 application that runs entirely without an internet connection, featuring native Windows `.exe` packaging and per-game Battle Royale mode.
- **Private Local AI with RAG**: Offline LLM inference using quantized GGUF models. Hosts and teachers can upload custom PDF textbooks/manuals (up to 10 files) to automatically generate curriculum-aligned questions and challenges with zero API costs and full student data privacy.
- **Modern Web Showcase & Host Portal (`frontend-react`)**: Responsive React 18 + Vite platform for online demonstrations, minigame catalog, and host management.
- **Spring Boot Backend (`backend-java`)**: Robust REST API for online licensing, game distribution, and cloud integration.
- **Modular Game SDK**: Extensible architecture supporting hot-swappable minigames (*Trivia Quiz*, *Hangman*, *Topic Takedown*, *GeoLocation*, *Snap Solve*, *Zero Margin*, *TimeLine*, *Rapid Rhythm*, *Guess Character*, etc.) loaded dynamically via custom classloaders.

---

## 📁 Repository Structure

```text
Game-Show-Center/
├── frontend-react/        # Web Portal & Online Host (React 18 + Vite)
├── backend-java/          # Cloud Backend API (Spring Boot 3)
├── template-offline/      # Desktop Engine (JavaFX 22 + Local Python AI Worker)
│   ├── games/             # Dynamic Minigames & JSON manifests
│   ├── scripts/           # Local AI inference worker (GGUF & PDF RAG)
│   └── src/               # Core JavaFX UI and Game Engine
└── docs/                  # Architecture, Developer & User Manuals, Business Plan
```

---

## 🚀 Quick Start

### 1. Offline Desktop Runtime (`template-offline`)
Requirements: **Java 21+**, **Python 3.10+** (optional, for local AI generation).

```bash
cd template-offline
# Windows
.\mvnw.cmd javafx:run
# Linux / macOS
./mvnw javafx:run
```

*For developer testing, use the master activation key: `YUYI-STUDIO-PRO-2026`.*

### 2. Web Portal (`frontend-react`)
Requirements: **Node.js 18+**, **npm**.

```bash
cd frontend-react
npm install
npm run dev
```

### 3. Backend Service (`backend-java`)
Requirements: **Java 21+**, **Maven**.

```bash
cd backend-java
# Windows
.\mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run
```

---

## 📚 Documentation

Detailed technical and operational guides can be found in the [`docs/`](./docs) folder:
- **`Manual de Programador.txt`**: Guide for developing and compiling custom minigames and plugins.
- **`Manual de Usuario Offline.txt`**: Complete host guide for operating the desktop game show.
- **`Business Plan.txt`**: Commercial strategy, licensing tiers, K-12 education, and university packages.

---

## 🛡️ License

Copyright © 2026 Yuyi Studios. All rights reserved.
