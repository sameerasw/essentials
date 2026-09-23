# Quick Settings (QS) Tile Implementation Guide

This guide details the end-to-end process of implementing, registering, and maintaining Quick Settings (QS) tiles within the **Essentials** architecture.

For system architecture context, refer to [ARCHITECTURE.md](ARCHITECTURE.md), [STRUCTURE.md](STRUCTURE.md), and [SERVICES_AND_PERMISSIONS.md](SERVICES_AND_PERMISSIONS.md).

---

## Architectural Overview

Quick Settings tile integration operates across three core layers:

```
┌─────────────────────────────────────────────────────────────┐
│                       System & Shade                        │
│             Android Quick Settings Shade (SystemUI)         │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    BaseTileService Layer                    │
│   - Standardized lifecycle & background coroutine scope     │
│   - Secure / Global settings cache & fallback bridge        │
│   - Permission & device support validation                  │
│   - Built-in haptic feedback                                │
└──────────────────────────────┬──────────────────────────────┘
                               │
         ┌─────────────────────┴─────────────────────┐
         ▼                                           ▼
┌─────────────────────────────────┐   ┌──────────────────────────────┐
│  Discovery & Headless Execution │   │    In-App Tile Manager UI    │
│  - QsTileRegistry               │   │  - QuickSettingsTilesSettingsUI
│  - QsTileActionRouter           │   │  - StatusBarManager tile add │
│  - QsTilesWidget (Glance)       │   │  - PermissionsBottomSheet    │
└─────────────────────────────────┘   └──────────────────────────────┘
```

---

## Step-by-Step Implementation Workflow

### 1. Create Tile Service Class

All QS tile services are located in `app/src/main/java/com/sameerasw/essentials/services/tiles/` and must extend [`BaseTileService`](../app/src/main/java/com/sameerasw/essentials/services/tiles/BaseTileService.kt).

#### Implementation Example:

```kotlin
package com.sameerasw.essentials.services.tiles

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.DeviceUtils

class FeatureTileService : BaseTileService() {

    override fun getTileLabel(): String = getString(R.string.tile_feature_label)

    override fun getTileSubtitle(): String {
        return if (isFeatureActive()) getString(R.string.on) else getString(R.string.off)
    }

    override fun hasFeaturePermission(): Boolean {
        return checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
    }

    override fun isDeviceSupported(): Boolean {
        // Optional override: Return false if feature is restricted to specific hardware/OEMs
        return true
    }

    override fun getTileIcon(): Icon? {
        val iconRes = if (isFeatureActive()) {
            R.drawable.rounded_feature_active_24
        } else {
            R.drawable.rounded_feature_inactive_24
        }
        return Icon.createWithResource(this, iconRes)
    }

    override fun getTileState(): Int {
        return if (isFeatureActive()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        // Asynchronous execution within service coroutine scope
        val newState = if (isFeatureActive()) 0 else 1
        putSecureInt("secure_feature_setting_key", newState)
    }

    private fun isFeatureActive(): Boolean {
        return getSecureInt("secure_feature_setting_key", 0) == 1
    }
}
```

#### `BaseTileService` Methods Reference:

| Method | Return Type | Description |
| :--- | :--- | :--- |
| `onTileClick()` | `Unit` | Executed asynchronously when the tile is tapped. |
| `getTileLabel()` | `String` | Primary title string rendered on the tile. |
| `getTileSubtitle()` | `String` | Secondary status text (e.g. "On", "Off", timer remaining). |
| `getTileState()` | `Int` | `Tile.STATE_ACTIVE` or `Tile.STATE_INACTIVE`. |
| `hasFeaturePermission()` | `Boolean` | Permission validation. Returns `Tile.STATE_UNAVAILABLE` when `false`. |
| `isDeviceSupported()` | `Boolean` | Compatibility check. Controlled by "Enable unsupported features" setting. |
| `getTileIcon()` | `Icon?` | Optional dynamic icon resolution based on feature state. |
| `getSecureInt()` / `putSecureInt()` | `Int` / `Unit` | Read/write secure system settings with cached fallback to Shizuku/root shell. |
| `getGlobalInt()` / `putGlobalInt()` | `Int` / `Unit` | Read/write global system settings with cached fallback to Shizuku/root shell. |

---

### 2. Android Manifest Registration

Declare the service inside `<application>` in [`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) with `BIND_QUICK_SETTINGS_TILE` permission:

```xml
<service
    android:name=".services.tiles.FeatureTileService"
    android:exported="true"
    android:icon="@drawable/rounded_feature_24"
    android:label="@string/tile_feature_label"
    android:permission="android.permission.BIND_QUICK_SETTINGS_TILE">
    <intent-filter>
        <action android:name="android.service.quicksettings.action.QS_TILE" />
    </intent-filter>
    <meta-data
        android:name="android.service.quicksettings.TILE_CATEGORY"
        android:value="android.service.quicksettings.CATEGORY_UTILITIES" />
</service>
```

---

### 3. String Localization

Declare user-facing tile labels and documentation strings in [`strings.xml`](../app/src/main/res/values/strings.xml):

```xml
<!-- Feature QS Tile -->
<string name="tile_feature_label">Feature Name</string>
<string name="about_desc_feature_tile">Enables or disables Feature directly from your Quick Settings shade or widget.</string>
```

---

### 4. Tile Registry Registration (`QsTileRegistry.kt`)

Register the tile in [`QsTileRegistry.ALL_TILES`](../app/src/main/java/com/sameerasw/essentials/services/tiles/QsTileRegistry.kt):

```kotlin
QsTileEntry(
    titleRes = R.string.tile_feature_label,
    iconRes = R.drawable.rounded_feature_24,
    serviceClass = FeatureTileService::class.java
),
```

#### Glance Widget Integration:
- `QsTileRegistry` provides state resolution, label translation, dynamic icon rendering, and active status for the **Favorite QS Tiles Glance Widget** ([`QsTilesWidget.kt`](../app/src/main/java/com/sameerasw/essentials/services/widgets/QsTilesWidget.kt)).
- Standard `BaseTileService` subclasses are automatically queried via reflection (`isTileActive`, `getTileSubtitle`, `getTileIcon`).
- If state queries require an external controller (e.g. `CaffeinateController`), add a custom condition in `isTileActive()` / `getTileSubtitle()`.

---

### 5. Headless Action Routing (`QsTileActionRouter.kt`)

Tapping a tile inside the **Favorite QS Tiles Glance Widget** triggers [`QsTileClickActionCallback`](../app/src/main/java/com/sameerasw/essentials/services/widgets/QsTileClickActionCallback.kt), which dispatches to [`QsTileActionRouter`](../app/src/main/java/com/sameerasw/essentials/services/receivers/QsTileActionRouter.kt):

- Standard `BaseTileService` subclasses are automatically initialized headlessly by `QsTileActionRouter` to invoke `onTileClick()`.
- If the feature requires broadcast routing or dedicated service intents, define an explicit dispatch branch in `QsTileActionRouter.kt`.

---

### 6. In-App Tile Manager UI (`QuickSettingsTilesSettingsUI.kt`)

All QS tiles must be added to the in-app Quick Settings Tiles settings screen so users can view permissions and add tiles directly to their system QS panel via `StatusBarManager.requestAddTileService()`.

In [`QuickSettingsTilesSettingsUI.kt`](../app/src/main/java/com/sameerasw/essentials/ui/features/tiles/QuickSettingsTilesSettingsUI.kt), register the tile in `allTiles`:

```kotlin
QSTileInfo(
    titleRes = R.string.tile_feature_label,
    iconRes = R.drawable.rounded_feature_24,
    serviceClass = FeatureTileService::class.java,
    permissionKeys = listOf("WRITE_SECURE_SETTINGS"),
    aboutDescription = R.string.about_desc_feature_tile,
    categoryRes = R.string.cat_utils // e.g. R.string.cat_visuals, R.string.cat_privacy, R.string.cat_accessibility
)
```

---

## Contributor Checklist

- [ ] **Import Standards**: All package imports declared at the top of the file (no inline package references).
- [ ] **Localization**: User-facing strings added to `strings.xml` with no duplicates.
- [ ] **Iconography**: Rounded drawable resources (`R.drawable.rounded_*`) used.
- [ ] **Permission Handling**: Required permissions correctly mapped in `QSTileInfo` and validated in `hasFeaturePermission()`.
- [ ] **Glance Widget Support**: Tile verified in `QsTilesWidget` (state toggling, label/subtitle display, haptics).
- [ ] **In-App Management**: Tile visible under correct category in `QuickSettingsTilesSettingsUI` with functional "Add" button and "About" dialog.
