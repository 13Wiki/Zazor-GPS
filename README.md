# Zazor GPS

Android camera app that stamps photos with GPS coordinates, address, date/time and accuracy,
plus freehand drawing, text overlays, 2- and 4-cell collages and a panorama mode.

## Build

```bash
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease   # minified + shrunk, debug-signed by default
```

Requirements: JDK 17+, Android SDK with platform 35 and build-tools 35.
Put the SDK location in `local.properties` (`sdk.dir=/path/to/Android/sdk`) or set `ANDROID_HOME`.

| | |
|---|---|
| Gradle / AGP / Kotlin | 8.11.1 / 8.7.3 / 2.0.21 |
| min / target / compile SDK | 24 / 35 / 35 |
| DI | Koin 3.5 |
| Camera | CameraX 1.4 |
| Storage | Room 2.6 (KSP), app-private external files |

### Signing a real release

`app/build.gradle` points the release `signingConfig` at the local debug keystore so
`assembleRelease` produces an installable APK out of the box. Before publishing, replace it with
your own keystore and keep the credentials out of version control:

```groovy
signingConfigs {
    release {
        storeFile file(System.getenv("ZAZOR_KEYSTORE"))
        storePassword System.getenv("ZAZOR_KEYSTORE_PASSWORD")
        keyAlias System.getenv("ZAZOR_KEY_ALIAS")
        keyPassword System.getenv("ZAZOR_KEY_PASSWORD")
    }
}
```

Then build an App Bundle for Play: `./gradlew bundleRelease`.

## Architecture

MVI-ish: each screen has a `Contract` (State + Event), a `ViewModel` exposing a `StateFlow<State>`
and consuming a `SharedFlow<Event>`, and a `Fragment`/`Activity` that renders state and sends
events. Cross-screen coordination (the edit sheet driving the photo preview, a collage cell coming
back from its own activity) goes through shared flows registered in Koin.

```
ui/
  auth/       PIN + biometric entry, and the "clear code" that wipes the gallery
  photo/      camera tab, collage tabs, panorama tab, edit-photo bottom sheet
  media/      gallery list, share, per-photo re-edit
  settings/   PIN / clear code / which stamp fields to show / trial code
data/         Room entity + DAO + repository, SharedPreferences
utils/        CameraX wrapper, location + geocoding, photo storage, view binding
views/        DrawView, EditorView (text overlay), NotesDragView, tab and pager views
```

## Known limitations

- **Panorama** relies on `libs/dmd_pano_library_2.jar`, a proprietary Camera1 SDK with no source
  and no upstream. It is wrapped in error handling and degrades to a message when the device
  rejects it, but it cannot be maintained or ported. Replacing it is the next larger piece of work.
- **The PIN and clear code are stored in plain `SharedPreferences`.** They gate the UI, not the
  files on disk. Anything stronger needs a KeyStore-backed hash.
- Photos live in the app-private external directory, so uninstalling the app deletes them.
