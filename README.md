# App Music Offline

- **App Music is an offline Android music player** that scans and plays audio files stored directly on the user’s device. It focuses on local music playback, so it does not require a backend server, online streaming service, or user account system.

- **The app provides a complete playback experience**, including play/pause, next song, previous song, seeking through the track, and real-time playback progress. It also includes a full player screen and a Mini Player at the bottom of the home screen for quick controls.

- **Users can search and organize their music library easily** by filtering songs based on title or artist. The app also includes a favorites feature, allowing users to mark songs they like and switch between the full music list and the favorite songs list.

- **Music data is loaded from Android MediaStore**, including song title, artist name, album artwork, and playable media URI. The app handles audio permissions for different Android versions, using `READ_MEDIA_AUDIO` on newer Android versions and `READ_EXTERNAL_STORAGE` on older ones.

- **Playback is powered by AndroidX Media3 and ExoPlayer**, with a dedicated `MediaSessionService` and `MediaController` to keep the player state synchronized with the UI. This provides a stronger foundation for media playback features such as background playback and system media controls.

- **The project is built with Kotlin, Jetpack Compose, and a clean MVVM-style structure**, separating UI components, ViewModel state, repository logic, data sources, and models. It uses Material 3 for the interface, Coil for loading album artwork, and SQLite for saving favorite songs locally.

**Technologies used:**  
- Android Jetpack
- SQLite
- Media3 ExoPlayer

