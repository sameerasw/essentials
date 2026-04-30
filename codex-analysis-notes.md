# Essentials Analysis Notes

## Project shape

- Android app module only: `:app`
- Stack:
  - Kotlin + Jetpack Compose
  - Material 3 / Material 3 Expressive APIs
  - SharedPreferences-heavy persistence
  - Services, receivers, tiles, widgets, IME, accessibility, WorkManager
  - Shizuku / root for privileged operations
  - GitHub API for updates
  - Jsoup for GSMArena scraping
  - Wearable Data Layer for calendar sync
- Min SDK 26, target/compile SDK 36
- App version in source: `13.1` (`versionCode = 40`)

## What the app is

This is not a single-feature utility. It is a platform-style Android toolbox for Pixel-oriented and power-user features:

- Quick Settings tiles
- notification-driven effects
- accessibility-driven behaviors
- app lock / freezing
- DIY automations
- a custom keyboard/IME
- widgets
- Wear OS calendar sync
- GitHub-based app update tracking
- EXIF/photo watermarking
- device info/spec scraping

The product model is "many independent utilities behind one shell" rather than one coherent workflow app.

## High-level architecture

### 1. App shell

- `EssentialsApp.kt`
  - stores global `context`
  - initializes Shizuku helpers
  - initializes logging
  - initializes DIY automation repository/manager
  - initializes calendar sync manager
  - manually initializes Sentry depending on user preference
  - registers a global screen/user-present security receiver

- `MainActivity.kt`
  - enormous top-level UI coordinator
  - handles splash animation, edge-to-edge setup, onboarding, tabs, update UI, GitHub auth UI, repo import/export, and multiple bottom sheets
  - uses a pager over `DIYTabs`

### 2. State + persistence

- `MainViewModel.kt`
  - central app state hub
  - ~2357 lines
  - owns permission state, feature toggles, update checks, search state, keyboard prefs, app lock/freeze state, lighting state, calendar sync state, export/import helpers, and many side effects
  - `check(context)` acts like a hydration/bootstrap routine:
    - recreates repositories
    - reloads permissions + system settings
    - registers content observers
    - registers power-save receiver
    - recalculates blur/theme/update/onboarding state
    - updates dependent services like app detection

- `SettingsRepository.kt`
  - main persistence layer
  - SharedPreferences-backed
  - stores nearly every feature setting
  - contains migration logic and some typed helpers

- `DIYRepository.kt`
  - separate SharedPreferences store for DIY automations
  - serializes sealed trigger/state/action models via Gson custom adapter

### 3. Feature metadata layer

- `FeatureRegistry.kt`
  - declarative catalog of app capabilities
  - each feature is an anonymous object extending `Feature`
  - defines:
    - feature id
    - strings/icons/categories
    - permissions required
    - searchable settings
    - visibility
    - toggle behavior
    - click behavior

- `PermissionRegistry.kt`
  - maps permission keys to dependent features

- `SearchRegistry.kt`
  - builds a search index at runtime from features + sub-settings + status bar icon registry

This metadata-driven layer is one of the most important implementation patterns in the app.

## Navigation / information architecture

### Main tabs

`DIYTabs.kt`

- `ESSENTIALS`
- `FREEZE`
- `DIY`
- `APPS`

The app does not appear to use Navigation Compose. Instead, tab state is owned directly in `MainActivity` and large screen sections are composed conditionally.

### Secondary activities

Manifest shows dedicated activities for:

- feature settings
- app settings
- app updates
- device info (`Your Android`)
- location alarm
- automation editor
- app freezing
- color picker
- flashlight intensity
- private DNS settings
- watermarking
- tile auth / preferences
- link picker
- app lock

## Runtime entry points outside the main UI

The app uses many Android system surfaces:

- Launcher activity
- share targets for text/image
- link interception activity
- input method service
- notification listener service
- accessibility service
- foreground services
- Quick Settings tiles
- app widgets
- condition provider service
- dream service
- broadcast receivers

This app is highly event-driven and service-driven, not just activity-driven.

## Feature implementation patterns

### Permission-sensitive / privileged features

Many capabilities branch between:

- ordinary Android API path
- `WRITE_SECURE_SETTINGS`
- accessibility service
- notification listener
- usage stats
- overlays
- Shizuku
- root

`ShellUtils.kt` abstracts the privileged execution source:

- if root mode enabled -> use `RootUtils`
- else -> use `ShizukuUtils`

This lets features like freezing, maps power saving, security, and some tile actions share a common privileged execution path.

Also notable:

- the app sometimes attempts to auto-enable its accessibility service when both:
  - the user opted into auto accessibility
  - `WRITE_SECURE_SETTINGS` is available

That behavior is coordinated inside `MainViewModel.check()`.

### Services and handlers

There is a recurring pattern:

- Android component receives the event
- a dedicated handler class owns the feature behavior

Examples:

- `ScreenOffAccessibilityService`
  - composes multiple handlers:
    - `FlashlightHandler`
    - `NotificationLightingHandler`
    - `ButtonRemapHandler`
    - `AppFlowHandler`
    - `SecurityHandler`
    - `AmbientGlanceHandler`
    - `AodForceTurnOffHandler`
    - `OmniGestureOverlayHandler`

- `NotificationListener`
  - reacts to posted/removed notifications
  - discovers channels for snoozing/maps
  - powers ambient glance / like-song flows

This is one of the cleaner parts of the codebase: behavior is often split into handler classes even though top-level activities/view models are very large.

## Key subsystems

### Accessibility service core

`ScreenOffAccessibilityService.kt`

- central runtime orchestrator for several features
- listens for:
  - screen on/off
  - user present
  - package/window changes
  - external volume long-press broadcasts
  - ambient glance intents
  - force-AOD-off intents
- also registers proximity sensor
- drives:
  - screen-off widget behavior
  - app lock flow
  - app automations
  - flashlight enhancements
  - notification lighting overlays
  - gesture overlay / circle-to-search behavior
  - freeze-on-lock scheduling

### App lock / app-aware automations / dynamic night light

`AppFlowHandler.kt`

- tracks current foreground package
- supports two upstream sources:
  - accessibility window changes
  - usage-stats polling via `AppDetectionService`
- responsibilities:
  - app lock launch/auth state
  - dynamic night light toggling for selected apps
  - app-based DIY automations (entry/exit actions)
  - gesture-bar related automation behavior

### App detection fallback service

`AppDetectionService.kt`

- foreground service
- polls `UsageStatsManager` every 500ms
- used when usage-access mode is enabled for app lock / related features

This is a pragmatic but potentially battery-expensive design choice.

### Volume button remap

Two paths exist:

- accessibility key filtering in `ButtonRemapHandler`
- shell/input-event path via `InputEventListenerService`

`InputEventListenerService.kt`

- foreground special-use service
- scans Linux input devices for volume event sources
- listens for long presses via low-level input stream
- only acts when screen is fully off and not in AOD
- can also adjust flashlight intensity on short presses

This is one of the more advanced/power-user parts of the app.

### Notification lighting

`NotificationLightingHandler.kt`

- queue-based overlay display manager
- reads lighting config from intents
- supports multiple modes:
  - stroke
  - glow
  - indicator
  - sweep
  - system mode
- can show ambient background behavior when screen is off
- uses `OverlayHelper`

### DIY automations

Core types:

- `Automation`
- `Trigger`
- `State`
- `Action`

Supported trigger/state model observed:

- triggers:
  - screen off
  - screen on
  - unlock
  - charger connected/disconnected
  - scheduled time
- states:
  - charging
  - screen on
  - time period
- app automations:
  - enter/exit actions for selected packages
- actions:
  - flashlight on/off/toggle
  - haptic vibration
  - dim wallpaper
  - sound mode
  - low power on/off
  - device effects
  - notification placeholders

Execution:

- `DIYRepository` persists automations
- `AutomationManager` watches repository flow
- it enables module instances depending on active automation types:
  - `PowerModule`
  - `DisplayModule`
  - `TimeModule`
- actions execute through `CombinedActionExecutor`

Current limitation observed:

- some action types like notification show/remove are still placeholders in `CombinedActionExecutor`

Important observation:

- DIY is modular at runtime, but the persistence model is still simple SharedPreferences JSON rather than a richer rules engine/data store.

### Keyboard / IME

`EssentialsInputMethodService.kt`

- Compose-based IME
- owns lifecycle/viewmodel store plumbing manually for Compose inside `InputMethodService`
- loads prefs live for keyboard appearance and behavior
- tracks clipboard history
- uses `SuggestionEngine`
- uses `UndoRedoManager`

`SuggestionEngine.kt`

- combines Android spell checker session + SymSpell
- copies a frequency dictionary asset on first use
- supports learned words from a local text file

`KeyboardInputView.kt`

- giant Compose file (~106 KB)
- contains actual keyboard layout and interactions
- custom key shapes, popup accents, haptics, clipboard UI, emoji and kaomoji switching

This IME is substantial and not a toy add-on.

### Watermarking

`WatermarkEngine.kt`

- decodes bitmap
- optionally rotates
- extracts EXIF via `MetadataProvider`
- draws either:
  - overlay watermark
  - frame watermark
- supports:
  - brand text
  - EXIF rows
  - custom text
  - OEM logo
  - border stroke/corners
  - accent-based color modes
- writes output JPG
- copies selected EXIF tags to result

`WatermarkViewModel.kt`

- manages preview generation separately from final save
- derives accent color from image palette
- auto-detects OEM logo from EXIF make/model

### App updates

There are two distinct update concerns:

1. Self-update check for Essentials
   - `UpdateRepository.kt`
   - checks GitHub releases for this app
   - compares semantic versions

2. Generic GitHub APK tracking for sideloaded apps
   - `GitHubRepository.kt`
   - `AppUpdatesViewModel.kt`
   - stores `TrackedRepo` list
   - maps repos to installed packages/apps
   - supports README display, release notes, export/import, APK download/install

The "Apps" tab in `MainActivity` is effectively a mini package/update manager UI for GitHub-hosted APKs rather than a standard app list.

### Device info / "Your Android"

`YourAndroidActivity.kt`

- fetches local device info via `DeviceUtils`
- fetches online marketing specs via `GSMArenaService`
- caches specs and downloaded images via `DeviceSpecsCache`
- supports pull-to-refresh

This is a hybrid local+scraped feature.

### Wear OS calendar sync

`CalendarSyncManager.kt`

- observes calendar provider changes
- queries next 7 days, up to 10 events
- sends event payloads to wearable via Google Play Services Data Layer
- also ships dynamic theme colors

`CalendarSyncWorker.kt`

- periodic backup sync path using WorkManager

### Widgets

- `ScreenOffWidgetProvider`
  - minimal widget trigger surface
- `BatteriesWidget`
  - Glance widget
  - combines phone battery, optional Mac battery via AirSync bridge, and Bluetooth battery readings

### Dream / ambient media glance

- `AmbientDreamService`
- `AmbientGlanceHandler`

The codebase suggests a strong focus on ambient, lockscreen, and screen-off experiences.

## UI / design system observations

### Theme approach

`EssentialsTheme`

- Material 3
- dynamic color on Android 12+
- optional "pitch black" override for dark mode by forcing several surfaces/backgrounds to black
- fallback static palette still uses default-ish purple material seed values

### Typography

- uses bundled `google_sans_flex.ttf`
- typography is customized globally
- also defines a rounded variation using font variation settings (`ROND`)

### Shapes

- global rounded corners
- many local components push corner radii further (24dp, 32dp)

### Visual language

Observed patterns:

- big rounded containers/cards
- strong use of `surfaceBright`, `surfaceContainer`, `primaryContainer`
- bottom floating toolbar used as a signature shell component
- blurred/frosted top/bottom overlays via custom AGSL progressive blur shader
- colorful deterministic pastel/vibrant icon badges based on feature title hash
- large onboarding with animation, gestures, and custom content
- strong emphasis on expressive but still Material-based UI
- occasional playful personality touches, including Easter eggs in onboarding

### Core reusable UI primitives

- `EssentialsFloatingToolbar`
  - signature bottom toolbar
  - used for tab mode and back/title mode
  - adaptive label hiding on compact/large-font situations

- `RoundedCardContainer`
  - stacked card grouping primitive

- `FeatureCard`
  - primary feature list item
  - long-press menu, blur/de-emphasis when menus are active, inline toggle, help/pin actions

- `FavoriteCarousel`
  - pinned features rendered in a Material 3 carousel

- `progressiveBlur`
  - custom shader-based blur edge effect for top/bottom chrome

### Design conclusion

The design is not a custom design system from scratch; it is a heavily customized Material 3 system with:

- Google Sans branding
- stronger rounding
- expressive floating-bottom navigation/action chrome
- blur/frost effects
- personalized color chips
- onboarding/playful personality touches

It feels handcrafted and brand-driven, but still anchored in Material semantics and Compose defaults.

## Build / dependency observations

Notable dependencies:

- Compose BOM + Material 3 alpha
- Shizuku
- HiddenApiBypass
- Google Play Services Location/Wearable
- WorkManager
- Gson
- Jsoup
- Palette
- Coil + GIF support
- SymSpell
- Sentry
- Glance widgets

Potential concern:

- some dependency declarations are duplicated or mixed across BOM-managed and explicit versions
- Material 3 alpha is intentionally forced

## Codebase scale indicators

- feature definitions in `FeatureRegistry`: 43
- files under `services/tiles`: 28
- config UI composables: 25
- top large files:
  - `MainViewModel.kt`
  - `KeyboardInputView.kt`
  - `MainActivity.kt`
  - `SetupFeatures.kt`
  - `AutomationEditorActivity.kt`

## Testing state

- only placeholder example unit/instrumentation tests are present
- no meaningful automated coverage found yet

## Architectural strengths

- broad feature ambition with many Android surface integrations
- metadata-driven feature catalog is useful
- handler pattern helps isolate runtime behaviors
- Compose UI is fairly reusable in many places
- advanced Android integration work is real and non-trivial
- strong product personality in UX

## Architectural weaknesses / likely maintenance pressure

- `MainViewModel` is overloaded
- `MainActivity` is overloaded
- several feature screens are very large
- SharedPreferences is doing the job of a wider application state/data layer
- behavior can be hard to reason about because features interact across services, listeners, handlers, and settings keys
- low automated test coverage
- some features depend on hidden APIs / shell / OEM behavior / special permissions, so long-term reliability may vary by device and Android version
- some content observers / listeners are registered from central hydration code, which increases the chance of duplicated mental ownership when debugging lifecycle issues

## Likely mental model for the app

Think of Essentials as:

- a feature registry
- backed by a giant settings repository
- rendered through Compose screens
- with background behavior carried out by Android services/receivers
- and privileged actions routed through Shizuku/root when needed

It behaves more like a bundled suite of device mods than a traditional app.
