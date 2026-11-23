# Flixvora - Modern Android Streaming Application

A high-performance Android media streaming player built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **AndroidX Media3 (ExoPlayer)**.

---

## ✨ Features

- **Modern UI / UX**: Clean, immersive streaming interface designed with Jetpack Compose & Material 3.
- **Robust Media Player**: Powered by AndroidX Media3 ExoPlayer with support for DASH, HLS, adaptive streaming, and automatic quality switching.
- **Content Filtering**: Comprehensive privacy and content controls with instant cache purging.
- **Search & Discovery**: High-speed debounced search with cancelable background jobs and instant auto-complete suggestions.
- **Local Proxy Engine**: Built-in on-device request processing engine for optimized resource resolution.
- **Offline Download Manager**: Multi-threaded download service with foreground notification support.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose (100% Kotlin)
- **Design System**: Material Design 3
- **Network**: Retrofit 2 + OkHttp 4 + Moshi
- **Local Storage**: Room Database + Jetpack DataStore Preferences
- **Asynchronous**: Kotlin Coroutines & StateFlow
- **Image Loading**: Coil Compose

---

## 🚀 Building & Releases

This repository includes automated **GitHub Actions CI/CD workflows**:
- Automated build runs on every push.
- APK artifacts are compiled and published directly under the **Releases** section.

---

## 📄 License

This project is licensed under the MIT License.
