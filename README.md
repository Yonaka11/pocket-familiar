# Pocket Familiar

A free, ad-free Android app that lets a small animated pet live on top of other apps as a floating overlay.

**Package:** `com.mikazuki.pocketfamiliar`  
**Min SDK:** 26 (Android 8.0 — required for `TYPE_APPLICATION_OVERLAY`)  
**Target SDK:** 35 (Android 15)

---

## Features (v0.1 MVP)

- Pet walks left and right, idles, and sleeps on top of any app
- Turns around at screen edges
- Can be dragged by touch; released pet falls with gravity
- Lands at the bottom of the usable screen (navigation-bar aware)
- Handles screen rotation via `DisplayManager.DisplayListener`
- Settings persist across launches (DataStore Preferences)
- Fully offline — no ads, analytics, accounts, or network permissions

---

## Build

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 11+, Android SDK with API 35.

---

## Install and run

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.mikazuki.pocketfamiliar/.MainActivity
```

1. Tap **Grant Overlay Permission** and enable it in Android Settings
2. Tap **Start Pet** — the familiar appears on screen
3. Open any app — the familiar walks over it
4. Drag the pet to move it; release to drop it
5. Tap **Stop Pet** or the notification action to remove the overlay

---

## Architecture

```
app/src/main/kotlin/.../
├── MainActivity.kt               Single Compose Activity
├── PocketFamiliarApplication.kt
├── ui/
│   ├── screens/HomeScreen.kt     Home screen (controls, settings, battery)
│   ├── screens/HomeViewModel.kt  AndroidViewModel; StateFlows for UI state
│   └── theme/Theme.kt            Material3 dynamic-color theme
├── service/
│   ├── PetOverlayService.kt      Foreground service; tick loop + DisplayListener
│   └── BootReceiver.kt           Optional auto-start after reboot
├── overlay/
│   ├── PetOverlayManager.kt      WindowManager window; VelocityTracker drag
│   └── PetView.kt                Custom View; frame-stepped sprite rendering
├── pet/
│   ├── behavior/PetState.kt      Sealed class: Idle/Walk/Sleep/Dragged/Falling
│   ├── behavior/PetStateMachine.kt   Coroutine-scheduled autonomous transitions
│   ├── physics/PetPhysicsEngine.kt   Gravity, boundaries, nav-bar inset
│   └── animation/PetAnimationSet.kt  Maps PetState → PetAnimation; horizontal flip
├── data/PetSettingsRepository.kt DataStore Preferences
├── model/PetSettings.kt
├── model/BatteryState.kt         Battery mood: happy / normal / tired / sleepy / charging
└── util/BatteryMonitor.kt        Sticky broadcast → StateFlow<BatteryState>
```

**Why a custom View for the overlay instead of ComposeView?**
A bare `WindowManager` window has no `ViewTreeLifecycleOwner`. Attaching Compose requires installing lifecycle owners manually. A plain `View` with a ticked frame loop is simpler and has no extra dependencies.

---

## Tech stack

| Component | Version |
|---|---|
| Kotlin | 2.0.21 |
| Android Gradle Plugin | 8.7.3 |
| Gradle | 8.9 |
| Jetpack Compose BOM | 2025.04.01 |
| Material3 | 1.x |
| DataStore Preferences | 1.1.1 |
| Coroutines | 1.9.0 |

---

## Permissions

| Permission | Why |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the pet overlay over other apps |
| `FOREGROUND_SERVICE` | Required for API 28+ |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Required for API 34+ (overlay pet has no matching typed category) |
| `RECEIVE_BOOT_COMPLETED` | Optional auto-start after reboot |

No internet, location, camera, microphone, contacts, or storage permissions are used or requested.

---

## Roadmap

- [ ] Screen-edge climbing (`ClimbLeft` / `ClimbRight` states pre-declared)
- [ ] Throwing (horizontal velocity on drag release)
- [ ] Custom sprite packs (swap drawables per `selectedPetId`)
- [ ] Battery-reactive behavior (mood system is architecturally ready)
- [ ] Multiple simultaneous pets
- [ ] Sound effects and haptics
- [ ] Time-of-day and weather reactions

---

## License

MIT
