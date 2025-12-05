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
    // Key for accessibility (use unique string)
    PermissionRegistry.register("ACCESSIBILITY", "Screen off widget")
    // Key for write secure settings
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", "Statusbar icons")
    // Key for Shizuku (maps power saving)
    PermissionRegistry.register("SHIZUKU", "Maps power saving mode")
    // Key for notification listener permission
    PermissionRegistry.register("NOTIFICATION_LISTENER", "Maps power saving mode")
    // Key for draw over other apps permission (Edge lighting overlay)
    PermissionRegistry.register("DRAW_OVER_OTHER_APPS", "Edge lighting")
    // add other registrations here if needed in future
}