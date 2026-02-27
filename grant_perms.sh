#!/bin/bash

# Package name
PKG="com.sameerasw.essentials"

echo "Granting permissions for $PKG..."

# Runtime Permissions
adb shell pm grant $PKG android.permission.READ_CALENDAR
adb shell pm grant $PKG android.permission.READ_PHONE_STATE
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION
adb shell pm grant $PKG android.permission.ACCESS_BACKGROUND_LOCATION
adb shell pm grant $PKG android.permission.BLUETOOTH_CONNECT
adb shell pm grant $PKG android.permission.BLUETOOTH_SCAN

# Secure Settings (Critical for many features)
adb shell pm grant $PKG android.permission.WRITE_SECURE_SETTINGS

# AppOps (Special Permissions)
adb shell appops set $PKG GET_USAGE_STATS allow
adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
adb shell appops set $PKG WRITE_SETTINGS allow

# Enable Services
# Notification Listener
LISTENER="$PKG/com.sameerasw.essentials.services.NotificationListener"
adb shell settings put secure enabled_notification_listeners $LISTENER

# Accessibility Service
ACCESSIBILITY="$PKG/com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService"
adb shell settings put secure enabled_accessibility_services $ACCESSIBILITY
adb shell settings put secure accessibility_enabled 1

# Device Admin (Requires manual activation usually, but we can try)
# adb shell dpm set-active-admin $PKG/.services.receivers.SecurityDeviceAdminReceiver

echo "Permissions granted! You might need to restart the app for some changes to take effect."
