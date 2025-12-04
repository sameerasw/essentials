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
    PermissionRegistry.register("WRITE_SECURE_SETTINGS", "Status Bar Icon Control")
    // add other registrations here if needed in future
}

