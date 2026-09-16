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
| min / target / compile SDK | 24 / 36 / 36 |
| DI | Koin 3.5 |
| Camera | CameraX 1.4 |
| Storage | Room 2.6 (KSP), app-private external files |
| Dates | `java.time` via core library desugaring |
| Paging | ViewPager2 + Material TabLayout |

`./gradlew test` runs the unit suite.

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
  auth/       PIN + biometric entry, and the "clear code" that wipes everything
  privacy/    first run: what never leaves the phone, and the analytics switch
  photo/      camera, collage, wide-angle, approach series, edit sheet
  media/      gallery, per-photo re-edit, voice notes
  outings/    days walked, each day's track drawn from its photos
  share/      what travels and how: messenger, archive, plain text
  settings/   PIN, wipe code, stamp fields, launcher appearance, Pro
data/         Room entity + DAO + repository, SharedPreferences
analytics/    anonymous counters, behind an interface that cannot carry personal data
billing/      Play Billing behind a ProStatus interface
ads/          one slot, in the photo feed and nowhere else
utils/        CameraX, location, geocoding, storage, GPX/KML, zip bundles, launcher aliases
views/        DrawView, EditorView, NotesDragView, RouteView
```

## Metadata

The position is drawn onto the picture and never travels inside it. The camera, collage and
re-edit paths encode with `Bitmap.compress`, which writes no EXIF segment at all - no make, model,
software tag, timestamp or coordinates. The panorama SDK does copy EXIF from its source frames, so
its output is scrubbed by `MetadataStripper` before it reaches the gallery.

## Maps and panorama, and why neither uses an SDK

**The route is drawn, not a tile map.** Google Maps would mean an API key with a billing account
behind it, a network connection, and about 3 MB of library — for a screen whose job is "which point
is which, and how far apart". `RouteView` draws it: no key, no cost, and it works with no signal in
the middle of a field. Whoever wants real streets taps through to their own map app via a `geo:`
intent.

**Panorama is the phone's own ultra-wide lens**, found by inspecting focal lengths through the
Camera2 interop. A modern phone covers ~120 degrees in one frame: instant, nothing to stitch,
nothing added to the download. It goes through the same pipeline as an ordinary photo, so it gets
the stamp, the marks and the metadata scrub for free — the proprietary Camera1 SDK it replaced
bypassed all three.

## Before publishing

- Replace the debug signing config with a real keystore (see above).
- There is no telemetry in the build at all: Crashlytics and Analytics are not in the dependency
  list, so they are not in the APK, and `NoAnalytics` is what the app gets. An inert SDK would have
  kept the app just as silent, but not the claim just as checkable - a listing that says "collects
  nothing" should survive someone opening the file. Diagnostics come from Play Console, which
  collects crashes without any library of ours, and from the feedback row in settings. Restoring
  telemetry is the three steps listed in `app/build.gradle`, and then saying so in Data safety.
- Ads are off the same way: `zazor.admob.bannerUnitId` in `gradle.properties` is empty, so nothing
  is ever requested and the Pro row stays out of settings. The in-app product is only worth creating
  in Play Console when that id is filled in.
- Fill in the Data safety form: with this configuration the app collects nothing at all, and the
  advertising ID is removed from the merged manifest either way.

## Known limitations

- **The PIN and clear code are stored in plain `SharedPreferences`.** They gate the UI, not the
  files on disk. Anything stronger needs a KeyStore-backed hash.
- Photos live in the app-private external directory, so uninstalling the app deletes them.
- A photo is saved at the resolution the camera gave it, with the stamp and the marks composed onto
  the frame. The size is capped by a share of the app's heap rather than a fixed number, so a
  flagship's 200-megapixel frame is scaled to what the phone can actually hold instead of failing.
- The first release ships with the ads switched off (an empty banner unit id in
  `gradle.properties`) and with no Pro row in settings; both come back with one line and a version
  bump.
- Run on an Android 14 emulator: capture, the stamp, the metadata scrub, the weak-fix warning, the
  gallery, the collage, the exports, the codes and the wipe behave. The ultra-wide lens, a real GPS
  fix and the microphone still need a phone - an emulator has no second camera, fakes the fix
  perfectly and records silence.
