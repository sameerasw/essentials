package com.sameerasw.essentials

object PermissionRegistry {
    private val registry = mutableMapOf<String, MutableList<String>>()

    fun register(permissionKey: String, featureName: String) {
        val list = registry.getOrPut(permissionKey) { mutableListOf() }
        if (!list.contains(featureName)) list.add(featureName)
    }

    fun getFeatures(permissionKey: String): List<String> = registry[permissionKey]?.toList() ?: emptyList()
}

// Register existing dependencies
fun initPermissionRegistry() {
    // Accessibility permission
    PermissionRegistry.register("ACCESSIBILITY", "Screen off widget")
    PermissionRegistry.register("ACCESSIBILITY", "Edge lighting")
    PermissionRegistry.register("ACCESSIBILITY", "Flashlight toggle")
    PermissionRegistry.register("ACCESSIBILITY", "Dynamic night light")
    PermissionRegistry.register("ACCESSIBILITY", "Screen locked security")

    // Write secure settings permission
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", "Statusbar icons")
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", "Sound Mode")
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", "Dynamic night light")
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", "Screen locked security")

    // Shizuku permission
    PermissionRegistry.register("SHIZUKU", "Maps power saving mode")
    PermissionRegistry.register("SHIZUKU", "Automatic write secure settings")

    // Notification listener permission
    PermissionRegistry.register("NOTIFICATION_LISTENER", "Maps power saving mode")
    PermissionRegistry.register("NOTIFICATION_LISTENER", "Edge lighting")

    // Draw over other apps permission
    PermissionRegistry.register("DRAW_OVER_OTHER_APPS", "Edge lighting")

    // Post notifications permission
    PermissionRegistry.register("POST_NOTIFICATIONS", "Caffeinate show notification")

    // Read phone state permission
    PermissionRegistry.register("READ_PHONE_STATE", "Smart data")

    // Default browser permission
    PermissionRegistry.register("DEFAULT_BROWSER", "Link picker - open with")

    // Device Admin permission
    PermissionRegistry.register("DEVICE_ADMIN", "Screen locked security")
}