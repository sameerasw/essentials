# Status Bar / Notification Shade Expansion Detection

This document explains how status bar and notification shade expansion/collapse can be detected on Android (specifically tested and analyzed on Pixel devices running modern Android versions) and outlines the code implementation, requirements, and permissions.

---

## 1. How It Works

When a user drags down or expands the status bar (Notification Shade or Quick Settings panel) and when it collapses back up, Android's WindowManager and SystemUI perform several window and surface operations:

1. **System Window Creation & Focus**:
   - SystemUI displays a system window with title / token matching `NotificationShade` (`WindowToken{... type=2036 ... NotificationShade}`).
   - It registers and unregisters back gesture callbacks (`OnBackInvokedCallbackInfo`) on the `NotificationShade` window.

2. **Accessibility Window Layer**:
   - With interactive window retrieval enabled in an Accessibility Service, Android tracks this top-level system window (`AccessibilityWindowInfo.TYPE_SYSTEM`).
   - Checking `windows.any { it.type == AccessibilityWindowInfo.TYPE_SYSTEM && it.title?.contains("NotificationShade", ignoreCase = true) == true }` evaluates to `true` when the notification shade is expanded, and `false` when fully collapsed.

---

## 2. Required Permissions & Configuration

### A. Accessibility Service Declaration
In `res/xml/accessibility_service_config.xml`:

```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged|typeViewClicked"
    android:accessibilityFeedbackType=""
    android:accessibilityFlags="flagRequestFilterKeyEvents|flagRetrieveInteractiveWindows"
    android:canRequestFilterKeyEvents="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
```

* **`typeWindowsChanged`**: Needed so the service is notified when windows appear, disappear, or change layer/order on screen.
* **`flagRetrieveInteractiveWindows`**: Mandatory flag allowing the service to inspect system and application window objects via `getWindows()` / `windows`.

### B. Runtime Permissions
* The user must grant the app's **Accessibility Service** in Android System Settings (`Settings > Accessibility > Essentials`).

---

## 3. Implementation Code

Inside the accessibility service (e.g., [`ScreenOffAccessibilityService`](file:///Users/sameerasandakelum/GIT/essentials/app/src/main/java/com/sameerasw/essentials/services/tiles/ScreenOffAccessibilityService.kt)):

```kotlin
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.utils.HapticUtil

class ScreenOffAccessibilityService : AccessibilityService() {

    private var isShadeExpanded = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (packageName != null) {
                appFlowHandler.onPackageChanged(packageName)
            }
        }

        checkStatusBarExpansion(event)
    }

    private fun checkStatusBarExpansion(event: AccessibilityEvent) {
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val expanded = try {
            val currentWindows = windows
            currentWindows.any { window ->
                window.type == AccessibilityWindowInfo.TYPE_SYSTEM &&
                    window.title?.contains("NotificationShade", ignoreCase = true) == true
            }
        } catch (e: Exception) {
            false
        }

        if (expanded != isShadeExpanded) {
            isShadeExpanded = expanded
            if (expanded) {
                // Status bar / Shade expanded
                HapticUtil.performHapticForService(this, HapticFeedbackType.SUBTLE)
            } else {
                // Status bar / Shade collapsed
                HapticUtil.performHapticForService(this, HapticFeedbackType.TICK)
            }
        }
    }
}
```

---

## 4. Alternative Detection Methods

1. **Privileged Shizuku / Shell (`dumpsys statusbar`)**:
   - Running `dumpsys statusbar` provides internal status bar flags:
     - `mExpandedVisible=true` (or `panelExpanded=true`)
   - Useful for one-off state checks via ADB/Shizuku, but polling is needed for real-time transitions.
2. **Broadcast Receiver (`Intent.ACTION_CLOSE_SYSTEM_DIALOGS`)**:
   - Triggers when system dialogs or shade collapse, but does not notify when the shade expands.
