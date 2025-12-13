# VideoScan PDF

An Android app that records video of pages, suggests sharp/stable frames, and exports to PDF.

## Features

- 📹 **Video Recording** - Record documents with CameraX
- 🎞️ **Frame Picker** - Scrub through video and select pages
- ✏️ **Page Editor** - Rotate, delete, and reorder pages
- 📄 **PDF Export** - Generate and share PDFs
- 💾 **Offline-first** - All data stored locally

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Repository pattern
- **DI**: Hilt
- **Database**: Room
- **Camera**: CameraX
- **Video**: Media3 / ExoPlayer
- **PDF**: Android PdfDocument

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 35
- Min SDK: 26 (Android 8.0)
- Kotlin 2.0+

## Building

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle
4. Run on device or emulator

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/vs/videoscanpdf/
├── data/
│   ├── dao/          # Room DAOs
│   ├── database/     # Room Database
│   ├── entities/     # Room Entities
│   └── repository/   # Repository pattern
├── di/               # Hilt modules
├── ui/
│   ├── home/         # Home screen
│   ├── recorder/     # Video recorder
│   ├── picker/       # Frame picker
│   ├── editor/       # Page editor
│   ├── export/       # PDF export
│   ├── settings/     # Settings
│   ├── navigation/   # Compose Navigation
│   └── theme/        # Material theme
├── MainActivity.kt
└── VideoScanPdfApplication.kt
```

## Roadmap

- [x] M1: Basic record → pick → edit → export flow
- [ ] M2: Smart frame suggestions (sharpness + document detection)
- [ ] M3: Motion stillness tracking
- [ ] M4: Advanced editing (crop, filters, perspective)
- [ ] M5: Diagnostics and performance optimization

## License

MIT