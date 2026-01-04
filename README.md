# Memos Client

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.0.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Android](https://img.shields.io/badge/platform-Android-green.svg?logo=android)](https://www.android.com/)

A native Android client for [Memos](https://github.com/usememos/memos) - an open-source, self-hosted memo hub.

> **⚠ Work in Progress**  
> This project is in early development stage. Features are being actively developed and the API may change.

- **Offline-First Architecture** - Create and view memos without internet connection
- **Background Sync** - Automatic synchronization with Memos server using WorkManager
- **Modern UI** - Built with Jetpack Compose and Material 3 design
- **Theme Support** - Light, dark, and system theme modes
- **Multi-Language** - Localization support
- **Material You** - Dynamic color theming on Android 12+

## Installation

### From Source

1. Clone the repository:

```bash
git clone https://github.com/nikita-popov/memos-client.git
cd memos-client
```

2. Open the project in Android Studio Ladybug or later
3. Build and run the project on your device or emulator

### From Releases

Binary releases will be available soon

## Configuration

On first launch, you'll need to configure your Memos server connection:

1. Open Settings from the main screen
2. Enter your Memos server URL (e.g., https://memos.example.com)
3. Provide your authentication credentials
4. Save settings and sync will start automatically

## Building

### Debug Build

```bash
./gradlew assembleDebug
```

### Release Build

```bash
./gradlew assembleRelease
```

## Roadmap

- Markdown support
- Search and filtering memos
- Tags management
- Resources (images, files) support
- Widget support
- Export/Import memos
- Conflict resolution for sync
- Multiple account support

## Known Issues

- Background sync may not work reliably on some devices with aggressive battery optimization
- Large memo synchronization not yet optimized

## Contributing

Contributions are welcome!  
Since I'm relatively new to Kotlin and Android development, any help, suggestions, or code reviews are greatly appreciated.

## License

Memos Client is open-source software licensed under the [MIT License](LICENSE).

## Acknowledgments

- [Memos](https://github.com/usememos/memos) - The amazing memo service this client is built for
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit

## Disclaimer

This is an unofficial client for Memos.  
It is not affiliated with or endorsed by the official Memos project.
