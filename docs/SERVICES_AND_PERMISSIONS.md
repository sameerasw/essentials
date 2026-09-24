# Services, QS Tiles & Permissions Matrix

This document outlines background services, Quick Settings tiles, and system permission requirements across the Essentials app.

---

## Quick Settings Tile Services (`services/tiles/`)

| Tile Service | Action & Description | Permission Requirements |
| :--- | :--- | :--- |
| **`CaffeinateTileService`** | Toggles screen awake wake lock timers. | `POST_NOTIFICATIONS` (Foreground Service) |
| **`FlashlightTileService`** | Toggles camera torch intensity. | `CAMERA` |
| **`PrivateDnsTileService`** | Cycles Private DNS provider mode (Off / Automatic / Custom Hostname). | `WRITE_SECURE_SETTINGS` |
| **`RefreshRateTileService`** | Cycles display refresh rate profiles (Fixed / Auto / Peak). | `WRITE_SECURE_SETTINGS` |
| **`SoundModeTileService`** | Cycles sound profiles (Normal / Vibrate / Silent). | `ACCESS_NOTIFICATION_POLICY` |
| **`HapticsTileService`** | Toggles system vibration and touch haptics together. | `WRITE_SETTINGS` |
| **`DataSimTileService`** | Switches to the next active data SIM and enables data on it. | `Shizuku` / `Root`, `READ_PHONE_STATE` |
| **`AppFreezingTileService`** | Freezes background apps using package state policies. | `Shizuku` / `Root` |
| **`ChargeQuickTileService`** | Toggles fast charging policy sysfs node. | `Root` |

---

## Privileged Permission Matrix

- **`WRITE_SECURE_SETTINGS`**: Granted via ADB (`adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS`). Allows modifying system secure settings.
- **`Shizuku` Binder Interface**: Enables executing privileged system API calls without full root access.
- **`Root` (`su`)**: Used for direct kernel sysfs writes (e.g. charging current control, SurfaceFlinger adjustments).

---

## Developer Guide

For instructions on adding and registering new Quick Settings tiles, refer to [ADD_QS_TILE.md](ADD_QS_TILE.md).

