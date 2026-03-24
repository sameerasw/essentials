package com.sameerasw.essentials.data.model

data class DeviceSpecItem(
    val name: String,
    val value: String
)

data class DeviceSpecCategory(
    val category: String,
    val specifications: List<DeviceSpecItem>
)

data class DeviceSpecs(
    val deviceName: String,
    val detailSpec: List<DeviceSpecCategory>,
    val imageUrls: List<String> = emptyList(),
    val localImagePaths: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)
