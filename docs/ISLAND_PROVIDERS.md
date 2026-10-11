# Island providers

Any app can put content on the Essentials island without shipping any code into Essentials. Essentials is the **host**: it owns the window, the animations, the gestures, the priority order and every piece of permission UI. A **provider** app only describes what to show and, optionally, draws its own expanded view. Overcast is the reference provider.

For how the island itself works, see [ISLAND.md](ISLAND.md).

---

## Overview

```
 provider app                                   Essentials (host)
 ─────────────                                  ─────────────────
 IslandProviderService  ◄── bind (AIDL) ──────  RemoteProvidersPlugin
   publish(payload, icons) ─────────────────►     ProviderConnection ─► PayloadValidator
                                                   RemoteItemMapper  ─► IslandItem (compact / line / expanded)
   onAction(...)          ◄───────────────────     taps and long presses
   Surface (SurfaceControlViewHost) ──────────►    RemoteExpanded (SurfaceView + snapshot)
   snapshot / resize / peek / expand ──────────►    host requests (rate limited)
```

- The compact pill and the line are **data**. The provider sends text, icons and tones; Essentials draws them with its own look, so every provider matches.
- The expanded view is **provider-drawn**. The provider renders Compose into a `SurfaceControlViewHost` and Essentials embeds it. This needs Android 11 or later; older versions simply get no expanded view.
- Transport is a bound service with a small AIDL interface. Payloads are JSON plus icon bitmaps.
- The host binds a provider only after the user approved it. See [Security model](#security-model).

---

## Modules

Both live in the Essentials repository and are referenced as Gradle projects until they are published.

| Module | Contents | Use it when |
|---|---|---|
| `island-sdk` | Payload model, AIDL, `IslandProviderService`, `IslandHostHandle`, `HostGrantStore`, `IslandIdentity` | You only publish compact/line data |
| `island-sdk-compose` | `ComposeIslandProviderService`, `IslandSurface`, `Modifier.islandSnapshot` (depends on `island-sdk`) | You also draw an expanded view with Compose |

```kotlin
// settings.gradle.kts of the provider app
include(":island-sdk")
project(":island-sdk").projectDir = file("../essentials/island-sdk")
include(":island-sdk-compose")
project(":island-sdk-compose").projectDir = file("../essentials/island-sdk-compose")

// app/build.gradle.kts
implementation(project(":island-sdk-compose"))
```

The modules use the `com.android.library` plugin and Compose; the provider needs `libs.plugins.android.library` available in its version catalog.

---

## Build a provider

### 1. Declare the service

```xml
<service
    android:name=".MyIslandService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.sameerasw.island.PROVIDER" />
    </intent-filter>
    <meta-data android:name="com.sameerasw.island.provider.label" android:value="@string/island_label" />
    <meta-data android:name="com.sameerasw.island.provider.description" android:value="@string/island_description" />
    <meta-data android:name="com.sameerasw.island.provider.priority" android:value="58" />
</service>
```

| Metadata | Required | Meaning |
|---|---|---|
| `…provider.label` | No (falls back to the service label) | Name shown in Island settings |
| `…provider.description` | No | One sentence shown in the approval sheet |
| `…provider.priority` | No (58) | Default position in the priority order. Lower wins. Clamped to 11 to 100 |
| `…provider.settings` | No | Exported activity to open from the gear in Island settings. Falls back to the launcher |

The app icon is used as the provider's icon. Nothing else about the UI is provider-controlled.

### 2. Publish data

```kotlin
class MyIslandService : ComposeIslandProviderService() {

    override fun onAttached(host: IslandHostHandle, info: HostInfo) {
        host.publish(payload(), mapOf("icon" to iconBitmap()))
    }

    private fun payload() = IslandPayload(
        items = listOf(
            IslandItemPayload(
                key = "demo",
                compact = listOf(
                    IslandCellPayload(key = "demo.icon", iconId = "icon", tone = IslandTone.ACCENT),
                    IslandCellPayload(key = "demo.value", text = "42", tone = IslandTone.NONE),
                ),
                line = IslandLinePayload(start = "Demo", end = "Something happened", iconId = "icon"),
                expanded = IslandExpandedPayload(heightDp = 240),
                interactions = IslandInteractionsPayload(
                    onLongPress = IslandAction(IslandActionType.OPEN_APP),
                ),
            ),
        ),
    )

    override fun hasExpandedContent(itemKey: String) = itemKey == "demo"

    @Composable
    override fun ExpandedContent(itemKey: String) {
        // Your Compose UI. See "Expanded view" below.
    }
}
```

Rules for publishing:

- Call `publish` from `onAttached` so the island has data as soon as it connects, then again whenever your data changes. The host coalesces updates to roughly one every 150 ms.
- Publishing an empty `items` list clears everything the provider has on the island.
- A provider can only run while bound. Bind only happens while the island is running, so also persist your state.
- If you only publish data, extend `IslandProviderService` and override `onHostAttached` instead.

### 3. Items, cells and the line

| Field | Notes |
|---|---|
| `key` | Unique inside the provider. The host namespaces it with your package |
| `compact` | 1 or 2 cells. Each has `key`, optional `iconId`, optional `text`, `tone`, `soloOnly` |
| `compactVisible` | `false` keeps the item out of the compact pill (it can still peek and expand) |
| `placement` | `dynamic` (default) or `pinned` |
| `priority` / `priorityOverride` | Base priority, and a temporary one while something is urgent. The user's order from Island settings overrides `priority`, never `priorityOverride` |
| `line` | `start`, `end`, `iconId`, `tone`. Shown when the item peeks |
| `expanded` | `heightDp` is only the first guess. Report the real height, see below |
| `accent` | ARGB colour for the item |
| `interactions` | `onTap`, `onLongPress`, `onExpandedTap`, `onOpen` |

Tones: `accent` (themed accent), `alert` (error colour) and `none` (draw the bitmap as is, use this for multi colour icons).

Icons are bitmaps sent next to the JSON, keyed by the `iconId`. Keep them small, 96 px is plenty.

### 4. Actions

An `IslandAction(type, target)` can be:

| Type | Effect |
|---|---|
| `open_app` | Launches your app |
| `callback` | Calls `onAction(itemKey, target)` in your service |
| `expand_item` | Expands another item. `target` is one of your item keys, or `brief` for the Brief card |
| `collapse` | Collapses the island |
| `none` | No action, host default applies |

### 5. Talking back to the host

`IslandHostHandle` gives you:

| Call | Purpose |
|---|---|
| `publish(payload, assets)` | Replace everything you show |
| `peek(itemKey, durationMs)` | Briefly show the line. At most once every 10 s and 8 s long |
| `expand(itemKey)` | Open the expanded view. At most once every 30 s |
| `collapse(itemKey)` / `dismiss(itemKey)` | Close it |
| `openApp(itemKey)` | Launch your app and collapse |
| `keepAlive(itemKey)` | Postpone auto collapse while the user is doing something |
| `setTextInput(itemKey, active)` | Make the window focusable for a text field |
| `resize(itemKey, heightPx)` | Report the real height of your expanded view |
| `snapshot(itemKey, bitmap)` | Send a picture of the expanded view for transitions |

Requests over the limits are silently dropped.

### 6. Brief cards

Provide `briefCards = listOf(BriefCardPayload(...))` in the payload to appear in the Brief overview. A card has a title, subtitle, icon, optional trailing icon and text, and an optional `pageHeightDp`. With a page height, tapping the card opens a page whose surface key is `IslandSurfaceKey.brief(card.key)`; return content for it from `ExpandedContent` the same way as for items.

---

## Expanded view

The host asks for a surface with `requestSurface`. `ComposeIslandProviderService` creates an `IslandSurface` (a `SurfaceControlViewHost` with a `ComposeView`) per item key and calls your `ExpandedContent(itemKey)`.

What to do inside it:

1. **Read the layout from `hostInfoState.value`** (`HostInfo`): `cameraClearancePx` is the space to leave at the top for the camera row, `compactHeightPx` the camera row height, `outsetPx` and `expandedPaddingPx` the paddings, `cornerPx` the corner radius, `accent`, `darkTheme`, `powerSave` (turn effects off when true).
2. **Draw your own black background and clip to `cornerPx`.** The surface is a hole in the island window, so what you do not draw shows the wallpaper behind it.
3. **Let the content take its natural height** (`wrapContentHeight(unbounded = true)`) and report it with `host.resize(itemKey, px)` from `onSizeChanged`.
4. **Add `Modifier.islandSnapshot { host.snapshot(itemKey, it) }`** above the clip and background. The host draws the last snapshot while the island animates, because a surface cannot follow the island's scale, fade and clip.
5. Make the root clickable and call `host.openApp(itemKey)` if tapping the view should open your app.

A skeleton:

```kotlin
@Composable
override fun ExpandedContent(itemKey: String) {
    val info = hostInfoState.value
    val density = LocalDensity.current
    val corner = with(density) { (info?.cornerPx ?: 0).toDp() }
    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top, unbounded = true)
            .onSizeChanged { host?.resize(itemKey, it.height) }
            .islandSnapshot { host?.snapshot(itemKey, it) }
            .clip(RoundedCornerShape(corner))
            .background(Color.Black),
    ) {
        Column {
            Spacer(Modifier.height(with(density) { (info?.cameraClearancePx ?: 0).toDp() }))
            // content
        }
    }
}
```

The host hides the live surface during collapse drags, page swipes and transitions and shows the snapshot instead, so keep sending fresh snapshots (the modifier does it about twice a second while the view is shown).

Limits: surface requests only work on Android 11 and later; the surface is never interactive outside its bounds; the host sizes the surface to the island body width plus outset.

---

## Security model

Installing a provider is not enough. Nothing flows until the user approves it, and both sides pin each other.

1. **User approval in Essentials.** Island settings, Island apps lists installed providers. Turning one on opens an Essentials bottom sheet with the provider's icon, its description, what a provider can do and its signing key. The provider never draws any approval UI.
2. **Pairing.** On Allow, Essentials briefly binds the provider and calls `grantHost()`. The provider accepts only a caller whose package is on its allow list (`IslandSchema.ALLOWED_HOSTS`, currently `com.sameerasw.essentials`), stores the grant with Essentials' signing certificate and returns true. Override `onHostGrantRequested(hostPackage)` to refuse.
3. **Pinning.** Essentials stores the provider's signing certificate. Every connect re-checks both: the provider only serves a caller whose package and certificate match its stored grant, and Essentials only binds a provider whose certificate still matches.
4. **No grant, no data.** Every binder call is checked against the grant, including `attach`, actions and surface requests. If the provider reports it is not authorised, Essentials turns the provider off and stops retrying.
5. **Revocation.** Turning the provider off in Essentials calls `revokeHost()`. A provider can also revoke from its own settings with `HostGrantStore.revoke(hostPackage)`; Overcast lists approved hosts with a switch for this.
6. **Sanitising.** Everything a provider sends passes `PayloadValidator`: at most 6 items and 4 Brief cards, keys up to 64 characters, cell text up to 32, line text up to 120, expanded height 80 to 640 dp, icons up to 256 px and 48 of them within 4 MB, unknown tones fall back to `none`. Providers can only open their own app.
7. **No user data leaves.** The host sends only layout and theme values (`HostInfo`): accent, density, island size, whether power saving is on.

The host never executes provider code. The only provider code that runs in Essentials' window is a surface the provider draws in its own process.

---

## Priority order

An approved provider is added automatically to the ordered list in Island settings, at the position its default priority gives it, or at the end if the user has already customised the order. The user can drag it like any built in entry, switch it off or open its settings. Items from a provider rank by that user position; `priorityOverride` still jumps ahead while set.

---

## Lifecycle and gotchas

- The service is bound when the island starts and unbound when it stops. Rebinds after process death use exponential backoff from 2 s to 60 s.
- After `onHostDetached` clear anything tied to the host; after the next `onAttached` publish again.
- Item and cell keys must be unique. Cell keys share a namespace with the other items on the island, so prefix them with your item key.
- Alerts that should interrupt use `priorityOverride` plus a `peek`; remember the 10 s peek limit.
- Test with the Essentials debug build and a provider build signed with a key you keep; a different key after pairing makes Essentials turn the provider off until the user approves it again.
- A reference implementation is in the Overcast repository: `weather/island/WeatherIslandService.kt`, `WeatherIslandPayload.kt` and `WeatherIslandExpanded.kt`.
