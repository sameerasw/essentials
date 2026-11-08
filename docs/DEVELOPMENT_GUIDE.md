# Development Guide

Architecture conventions, state management patterns, UI component reuse and service decoupling practices for Essentials. For how to propose and submit changes, see [CONTRIBUTING.md](../CONTRIBUTING.md).

---

## Environment Setup

1. **Android Studio**: Use the latest stable release of [Android Studio](https://developer.android.com/studio).
2. **JDK**: Use JDK 17 or higher.
3. **Clone Repository**:
   ```bash
   git clone https://github.com/sameerasw/essentials.git
   ```
4. **Target Branch**: Ensure all branches and pull requests are based on and targeted to merge back into **`develop`**.
5. **Shizuku / Root**: Many privileged features require [Shizuku](https://shizuku.rikka.app/) or Root for testing on your device or emulator.

---

## Core Architectural & Development Principles

### 1. State Management & ViewModel Integration
- **Complete End-to-End Pipeline**: Ensure any new UI control or `FeatureRegistry.kt` entry is backed by a complete state flow:
  - Provide typed getter and setter methods in [`SettingsRepository`](../app/src/main/java/com/sameerasw/essentials/data/repository/SettingsRepository.kt).
  - Expose reactive state (`mutableStateOf`) and mutator functions in the corresponding ViewModel (e.g. `MainViewModel`, `NetworksViewModel`).
  - Connect the UI composable and `FeatureRegistry.onToggle` directly to these ViewModel methods.
- **Centralized Preference Keys**: Define all preference keys and helper accessors inside `SettingsRepository` to keep keys uniform and discoverable.
- **Database & Persistent Properties**: Maintain clean migrations for persistent properties to preserve built-in configuration export and import integrity.

---

### 2. Service Decoupling & Modularity
- **Preserve Shared Services**: Keep shared background services (such as `ScreenOffAccessibilityService`) lightweight and focused on their core responsibilities.
- **Use Dedicated Handlers & Controllers**:
  - Encapsulate feature-specific listeners (e.g. connectivity changes, sensor observers, audio events) in dedicated controllers or handlers under `domain/controller/` or `services/handlers/`.
  - Connect external events to shared services through clean adapters or listeners to maintain clear separation of concerns.

---

### 3. Privileged Execution & Error Feedback
- **Transparent Execution**: When executing shell commands (`ShellUtils.runCommand`), system APIs, or Shizuku binders:
  - Check command return codes and handle permission exceptions gracefully.
  - Provide clear UI feedback (such as guidance sheets, permission cards, or status indicators) if privileged execution cannot be completed.
- **Pre-Flight Permission Checks**: Verify required permissions (`WRITE_SECURE_SETTINGS`, Shizuku, Root, Accessibility) before initiating restricted actions.

---

### 4. Component Reuse & Design System
Leverage the rich design system components in `app/src/main/java/com/sameerasw/essentials/ui/core/` to maintain a consistent Material 3 Expressive interface:
- **Containers (`ui/core/containers/`)**:
  - `RoundedCardContainer`: Use for grouping related settings items.
  - `RoundedCardLazyContainer`: Use for scrolling list containers.
- **Cards & Settings Items (`ui/core/cards/`)**:
  - `IconToggleItem`: Use for standard toggle rows with an icon, title, description, and switch. Always supply `index` and `count` for seamless shape morphing.
  - `ConfigPickerItem`: Use for settings rows opening picker dialogs or bottom sheets.
  - `FeatureCard`: Use for highlighted feature banners using pastel background palettes (`ColorUtil.getPastelColorFor`) and vibrant icons (`ColorUtil.getVibrantColorFor`).
  - `PermissionCard`: Use for consistent permission status displays and action triggers.
- **Pickers (`ui/core/pickers/`)**:
  - `SegmentedPicker`, `MultiSegmentedPicker`: Use for connected button groups with built-in tactile feedback.
- **Bottom Sheets (`ui/core/sheets/`)**:
  - `EssentialsBottomSheet`, `PermissionsBottomSheet`, `FeatureHelpBottomSheet`: Use for modal sheets and feature guidance.

> [!TIP]
> Always check `ui/core/` before creating custom cards, list items, or containers to ensure visual harmony and maintainability.

---

### 5. Jetpack Compose & Material 3 Expressive Conventions
- **Top-Level Package Imports**: Place all class and symbol imports at the top of the file and reference items by their simple names.
- **Structured Grouping**: Group related settings into `RoundedCardContainer` blocks to maintain visual hierarchy.
- **Material 3 Expressive Theming**:
  - Use `surfaceContainer` for outer card containers and `surfaceContainerHigh` for modal bottom sheets.
  - Ensure compatibility with Dynamic Color and Pitch Black (pure `#000000` AMOLED) token palettes.
- **Supportive Disabled States**: When a feature is inactive or missing requirements, use `enabled = false` paired with `onDisabledClick` to present an explanatory guidance sheet.

---

### 6. Iconography & String Localization
- **String Resources**: Place all user-visible text in `app/src/main/res/values/strings.xml` and access them via `stringResource(R.string...)` or `context.getString(R.string...)`. Check existing entries first to avoid duplicates.
- **Drawable Resources**: Use rounded drawable resources (`R.drawable.rounded_*`) across UI elements and Quick Settings tiles.

---

### 7. Tactile Haptic Feedback (`HapticUtil`)
- **Interactive UI Feedback**: Integrate appropriate haptic responses on buttons, switches, sliders, segment pickers, and tiles using `HapticUtil` (`performUIHaptic`, `performVirtualKeyHaptic`, `performHeavyHaptic`, `performLightHaptic`).
- **Background & Tile Actions**: Use `HapticUtil.performHapticForService(context)` inside background services and QS tile interactions.
- **Feature Haptic Preferences**: Respect user-configured haptic profiles when available.

---

### 8. Quick Settings Tile Integration
- Follow the step-by-step developer guide in [ADD_QS_TILE.md](ADD_QS_TILE.md) when adding new tiles.
- Declare the service in `AndroidManifest.xml`, register in `QsTileRegistry.kt`, support headless execution in `QsTileActionRouter.kt`, list in `QuickSettingsTilesSettingsUI.kt`, and test on the **Favorite QS Tiles Glance Widget**.

---

### 9. Universal Search Integration (`FeatureRegistry.kt`)
- Register configurable settings in `FeatureRegistry.kt` using `SearchSetting(...)` entries.
- Attach `Modifier.highlight(highlightSetting == "key")` to composables so universal search can smoothly navigate to and highlight target items.

---

### 10. Code Style & Technical Documentation
- Write clean, concise, and idiomatic Kotlin.
- Use clear, technical comments where complex architecture, system settings, or low-level hardware interactions benefit from explanation.

---

## Best Practices Reference

| Area | Recommended Pattern | Context |
| :--- | :--- | :--- |
| **ViewModel State** | Expose reactive states via `SettingsRepository` and ViewModel methods | Ensures clean compilation, predictable state flow, and working search toggles. |
| **Background Logic** | Encapsulate features in modular handlers under `domain/controller/` | Keeps shared services (e.g. accessibility service) clean and isolated. |
| **Import Hygiene** | Place all package imports at the top of the file | Keeps code readable and conforms to project styling conventions. |
| **Preferences** | Store and access keys through constants in `SettingsRepository` | Prevents typos and centralizes data contracts. |
| **Privilege Feedback** | Validate permissions and provide clear UI feedback on failures | Keeps users informed when elevated permissions are needed. |
| **UI Components** | Use `RoundedCardContainer`, `IconToggleItem`, and `ui/core/` composables | Preserves design consistency and built-in shape morphing across all screens. |
