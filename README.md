# CookingAssistant (Android, Java)

Smart cooking assistant with pantry management, recipe search (TheMealDB), and an Open Kitchen Mode using multiple sensors (light, proximity, accelerometer).

## Features
- The pantry with add/edit/delete, quick-add dialog, and clear-all with Undo.
- Recipe search, rich detail screen (image header, chips, ingredients, TTS).
- **Open Kitchen Mode**:
  - Light sensor → auto high-contrast “night kitchen”.
  - Proximity → hands-free: wave to advance steps.
  - Accelerometer → shake to start/pause timer + orientation readout.

## Tech
- Android Studio Ladybug (2024.2.2), Java 17, SDK 35  
- Room, Retrofit + Gson, Glide, Material Components  
- MVVM-ish, ViewBinding

## Build
- Open in Android Studio → Sync Gradle → Run on device (sensors work best on a real device).
- Use Pixel 6 in the emulator (best choice for this project)

## Repo hygiene
- Secrets not committed (`local.properties`, keystores ignored).
- Use branches + PRs. Protected `main`.

## Team
- @Mehul Modha (owner), 
- @Yash Ghildiyal (owner), 
- @Swarnim Tiwari (owner), 
