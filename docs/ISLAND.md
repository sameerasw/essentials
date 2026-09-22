# Island

The Island is the camera-cutout overlay that shows live content (notifications, media, calls, timers, calendar, and more) inside a single black surface around the front camera. It is built as a small host plus a set of plugins: the host owns the window, the surface, every animation and every gesture; plugins only describe what to show.

For general context, see [ARCHITECTURE.md](ARCHITECTURE.md) and [SERVICES_AND_PERMISSIONS.md](SERVICES_AND_PERMISSIONS.md). Design history and decisions are in `docs/working/island/`.

---

## How it fits together

```
 system sources            plugins                 host
 ──────────────            ───────                 ────
 NotificationListener ─┐
 CallReceiver ─────────┼─► IslandPlugin ─► IslandItem ─► IslandController ─► IslandUiState
 MediaSession, Torch,  │   (one per feature)             (stage machine)          │
 Calendar, ... ────────┘                                                          ▼
                                                        IslandWindowHost ─► IslandRoot (Compose)
                                                        (overlay windows)    surface + templates + gestures
```

- **Plugins** turn a data source into an `IslandItem` and publish it.
- **`IslandController`** merges all items, decides the current stage and which item is focused, and runs timers (peek, auto-collapse).
- **`IslandRoot`** renders the state: one black surface whose size, corners and content animate between stages.
- **`IslandCoordinator`** wires it all to the accessibility service, settings and suppression (landscape, fullscreen, screen off).

The accessibility service holds the coordinator as `ScreenOffAccessibilityService.islandOverlayHandler` and calls `updateState()`, `setFullscreen()`, `onConfigurationChanged()` and `onDestroy()` on it.

---

## File structure

All paths are under `app/src/main/java/com/sameerasw/essentials/`.

```
island/
├── model/
│   ├── IslandModels.kt        IslandStage, IslandItem, CompactCell, LineContent, ExpandedContent,
│   │                          IslandExpandedScope, QueueInfo, PluginRequest, IslandPriority
│   └── IslandPlugin.kt        IslandPlugin interface and IslandPluginContext
├── state/
│   ├── IslandController.kt    Stage machine: focus, peek, expand/collapse, dismiss, queue, timers
│   ├── CompactLayoutEngine.kt Pure function placing compact cells around the camera
│   └── CameraGeometry.kt      Camera position/size and CameraAnchor (Center / Start / End)
├── service/
│   ├── IslandCoordinator.kt   Entry point from the accessibility service; owns plugins and config
│   ├── IslandWindowHost.kt    Overlay windows (fixed drawing window + touch window, text-input mode)
│   ├── CameraGeometryResolver.kt  Cutout auto-detect and manual offsets -> CameraGeometry
│   ├── IslandStatusBarHider.kt    Optional status bar hiding while large
│   └── OverlayLifecycleOwner.kt   Lifecycle for the overlay ComposeView
├── ui/
│   ├── IslandRoot.kt          The surface, stage transitions, gestures, dismiss/queue feedback
│   ├── CompactTemplate.kt     Compact stage layout
│   ├── LineTemplate.kt        Line stage layout, fonts and text styles
│   ├── ExpandedHost.kt        Hosts a plugin's expanded composable
│   ├── IslandLayoutSpec.kt    Sizes derived from camera geometry and settings
│   ├── IslandMotion.kt        Every spring, curve and duration
│   ├── IslandHaptics.kt       Every haptic used by the island
│   ├── IslandModifiers.kt     animatePlacement()
│   └── components/            Shared building blocks (see below)
└── plugins/
    ├── BaseIslandPlugin.kt    Base class most plugins extend
    ├── PluginUtils.kt         sendPendingIntent, launchPackage, accentFrom, soften
    └── <feature>/             One folder per plugin

utils/call/                    Shared call module (also used by watch call sync)
utils/chronometer/             Timer / stopwatch notification tracking
utils/notification/            NotificationRepostFilter (group repost de-duplication)
```

Tests live in `app/src/test/java/com/sameerasw/essentials/island/` and cover the controller and the compact layout engine.

---

## Stages

The island is always in exactly one stage. Plugins never choose the stage directly; they provide content for each stage they support and the controller decides.

| Stage | Shows | Layout | Entered by |
|---|---|---|---|
| `Hidden` | Nothing | Camera-width sliver, then invisible | No visible items, or content suppressed |
| `Compact` | Up to 4 cells from all items | `CompactTemplate` (fixed) | Default resting stage |
| `Line` | The focused item only | `LineTemplate` (fixed) | Automatic only, via `PluginRequest.Peek` |
| `Expanded` | The focused item only | The plugin's own composable | Tap, or `PluginRequest.Expand` |

Rules worth knowing:

- **Line is never user-initiated.** A tap on a line peek expands it; swiping toward the camera collapses it. Line peeks can be disabled globally ("Line peek").
- **Expanded is sticky.** Other items updating never steal focus from an expanded item.
- **Every collapse looks the same.** Tap, timeout, button and plugin collapses all replay the swipe-to-collapse path through `IslandController.collapseAnimator`, so there is one collapse animation.

### Compact

`CompactLayoutEngine` receives every item's compact cells and returns what goes before and after the camera:

- Items are selected by priority until 4 cells are used; anything that does not fit is hidden, lowest priority first.
- Pinned items (time, battery) sit next to the camera. A two-cell dynamic item takes one side and pushes the pinned cells to the other: `battery time | cam | eq art`.
- The surface stays centred on the camera; cells hug the outer ends with equal edge padding.
- With an edge camera (`CameraAnchor.Start` / `End`) all cells go on the open side.

Priorities are in `IslandPriority` (lower wins). Current order: Call, Time, Battery, Notification, Flashlight, Timer, Media, Conscious Gate, Calendar.

### Line

A single row: icon, left text, camera gap, right text, optional end slot. Text is aligned away from the camera and scrolls with edge fades when it overflows. Plugins only provide the data (`LineContent`); they cannot change the layout.

### Expanded

A free-form composable supplied by the plugin through `ExpandedContent`. `ExpandedHost` fixes the width and a minimum height (so corners never clamp) and passes an `IslandExpandedScope`:

| Member | Purpose |
|---|---|
| `spec` | Current `IslandLayoutSpec` (paddings, camera sizes, outset, anchor) |
| `accent` | The item's accent colour |
| `collapse()`, `dismiss()`, `openApp()` | Host actions |
| `setTextInput(Boolean)` | Makes the window focusable for a text field (inline reply) |
| `keepAlive()` | Holds off auto-collapse for non-touch activity such as typing |

Expanded content owns its own padding so backgrounds can go full-bleed. It must pad its content by `spec.expandedOutset` and keep the camera row clear. Use the shared components rather than hand-rolling this.

---

## Adding a plugin

A plugin is a class that publishes zero or more `IslandItem`s. The steps below use a hypothetical "Rideshare" plugin.

### 1. Create the plugin

Create `island/plugins/rideshare/RidesharePlugin.kt`:

```kotlin
class RidesharePlugin : BaseIslandPlugin() {
    override val id = "rideshare"

    // Settings whose change should call refresh().
    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_RIDESHARE)

    override fun onStart() {
        // Register listeners / start collecting a repository. ctx!!.scope is cancelled on stop.
    }

    override fun onStop() {
        // Unregister anything registered in onStart.
    }

    override fun refresh() {
        val ride = currentRide()
        if (ctx == null || ride == null || !settings.isIslandShowRideshareEnabled()) {
            publish(null)
            return
        }
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.DEFAULT,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("ride.icon") { IslandIcon(R.drawable.rounded_car_24, size = 18.dp) },
                    CompactCell("ride.eta") { RollingText(ride.eta) },
                ),
                line = LineContent(icon = { IslandIcon(R.drawable.rounded_car_24) }, start = ride.driver, end = ride.eta),
                expanded = ExpandedContent { scope -> RideExpanded(ride, scope) },
                onOpen = { launchPackage(context, ride.packageName) },
            ),
        )
    }

    companion object {
        const val ITEM_KEY = "rideshare"
    }
}
```

`BaseIslandPlugin` provides `ctx`, `context`, `settings` and `publish()`. `publish(null)` removes the item.

### 2. Register it

Add it to the `plugins` list in `IslandCoordinator`. The coordinator starts and stops plugins with the island, forwards setting changes listed in `settingKeys`, and feeds every published item into the controller. There is no other registry.

### 3. Add the setting

Following the existing "What to show" toggles:

- `SettingsRepository`: a `KEY_ISLAND_SHOW_*` constant with getter and setter.
- `MainViewModel`: a state field, its load in the settings-load function, a branch in the settings-change listener, and a setter.
- `IslandSettingsUI`: an `IconToggleItem` in the "What to show" card.
- `FeatureRegistry`: a `SearchSetting` so it is searchable.
- `strings.xml`: the title (English only; translations come from the community).

If the plugin needs a runtime permission, request it through `PermissionsBottomSheet` in `IslandSettingsUI` and register it in `PermissionRegistry`. The plugin itself must still check permissions and publish nothing when they are missing.

### 4. Choose a data source

- **Notifications**: prefer a shared parser/repository in `utils/` fed by `NotificationListener` (see `utils/call` and `utils/chronometer`) over parsing inside the plugin. Other features can then reuse it.
- **System state** (torch, battery, media sessions): register in `onStart`, unregister in `onStop`.
- **Anything periodic**: use `ctx.scope` coroutines; they are cancelled automatically.

### What a plugin must not do

- Animate the surface, compute bounds, or touch windows. The host does this for every plugin.
- Keep the island awake with its own timers. Use `PluginRequest.Peek` / `Expand` / `Collapse` and let the controller handle timing.
- Crash on missing permissions or dead `PendingIntent`s. Guard actions and fall back quietly.

---

## Item reference

| Field | Notes |
|---|---|
| `key` | Stable id. Also the animation key; keep it constant for the same logical item. |
| `priority` | See `IslandPriority`. Lower is more important. |
| `placement` | `Pinned` for always-present status (time, battery), `Dynamic` for everything else. |
| `compact` | 1–2 cells. Index 0 is the icon (outer edge), index 1 the value (inner). Cell keys must be unique across items. |
| `line` | Optional. Without it the item cannot peek. |
| `expanded` | Optional. Without it a tap has nothing to expand and the island answers with a "no" wiggle. |
| `dismissible` / `onDismiss` | Enables swipe-away. |
| `onOpen` | Long press and "open" actions. |
| `interactions` | Overrides for tap and long press. |
| `queue` | `QueueInfo` for stacked content (notifications): the next item is revealed behind the card while swiping. |

Requests a plugin can send through `ctx.request(...)`:

- `Peek(key, durationMs)`: show the item in the Line stage briefly. Ignored while something is expanded or if Line peek is off.
- `Expand(key)`: open the item expanded (used by incoming calls and heads-up when compact heads-up is off).
- `Collapse(key)`: collapse if this item is focused.

---

## Shared UI components

Use these in expanded views so every plugin looks and behaves the same.

| Component | File | Use |
|---|---|---|
| `CameraRow` | `components/ExpandedParts.kt` | Header row split around the camera; handles edge anchors. |
| `ConnectedButtonRow` / `ConnectedItem` | `components/ExpandedParts.kt` | Segmented action buttons with haptics; per-item colours supported. |
| `ArtworkBackdrop` | `components/ExpandedParts.kt` | Blurred image background with a camera-safe black band. |
| `accentGlow` / `cameraClearance` | `components/ExpandedParts.kt` | Bottom colour glow that never reaches the camera zone. |
| `MarqueeText` | `components/MarqueeText.kt` | Single line that scrolls with edge fades only when it overflows. |
| `RollingText` | `components/RollingText.kt` | Per-digit roll; rolls up when the value rises, down when it falls. |
| `IslandBitmap`, `IslandIcon`, `EqualizerBars`, `BatteryRing` | `components/IslandComponents.kt` | Small visual pieces. |
| `IslandTextStyles`, `IslandFontFamily` | `ui/LineTemplate.kt` | Google Sans Flex text styles with tabular figures. |

---

## Gestures

Handled once in `IslandRoot` for every item:

| Gesture | Behaviour |
|---|---|
| Tap | Compact or Line → Expanded. Expanded → Compact. Nothing to expand → "no" wiggle. |
| Long press | Item's `onOpen` (or `interactions.onLongPress`). Ramping haptics while held. |
| Swipe toward the camera | Scrubs the collapse 1:1 with the finger, then finishes with the release velocity. On a notification stack, hides the current card instead. |
| Swipe away from the camera | Dismissible items slide off and dismiss. A reveal (or, on a stack, the next card) shows behind the card, with a Hide/Dismiss chip at the threshold. |

"Toward" and "away" are relative to the camera's side of the surface, so they work for edge cameras too. Any touch resets the auto-collapse and peek timers.

---

## Animation and haptics

- All motion values live in `IslandMotion`. Expand uses a soft spring; collapse uses an eased in/out curve; swipe releases use a critically damped spring seeded with the finger velocity.
- The surface only ever grows past the camera, never shrinks below it, and is never scaled, so the cutout stays covered.
- Content transitions (scale around the top edge, outgoing layer) are shared by every stage and plugin; plugins do not declare them.
- All haptics go through `IslandHaptics`, which uses service-context vibration because View haptics do not fire from an accessibility overlay window.

Tune motion or haptics in those two files only.

---

## Windows

`IslandWindowHost` keeps two accessibility overlay windows:

- A **drawing window**: fixed size, centred on the camera, not touchable. It never moves or resizes during animation, which keeps the surface anchored.
- A **touch window**: invisible, sized to the surface's target bounds, forwarding events to the drawing window's ComposeView.

While a text field is open (inline reply), the drawing window becomes focusable and touchable and the touch window is removed; it reverts when the field closes or the stage changes.

---

## Settings that affect layout

| Setting | Effect |
|---|---|
| Camera position | `CameraAnchor`: Center, Left or Right. Changes compact layout, surface placement and header rows. |
| Auto-detect / offsets / camera size / gap | Camera geometry. |
| Max width / expanded width | Line and expanded widths, clamped to the space on the camera's open side. |
| Expanded scale | Extra room added evenly around expanded content (`expandedOutset`); content is not scaled. |
| Expanded corner radius / padding / top padding | Expanded card shape and spacing. |
| Line peek | Enables the Line stage at all. |

All of these flow through `IslandCoordinator.applyConfig()` into `IslandLayoutSpec` and `CameraGeometry`.
