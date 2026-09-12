/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Model - Apps
 * File: AppListBackup.kt
 * Description: Data model representing an exported backup of installed application details and device metadata.
 */

package com.sameerasw.essentials.domain.model

import com.google.gson.annotations.SerializedName

data class AppListBackup(
    @SerializedName("device")
    val device: DeviceBackupInfo,
    @SerializedName("app_count")
    val appCount: Int,
    @SerializedName("apps")
    val apps: List<AppBackupItem>,
)

data class DeviceBackupInfo(
    @SerializedName("manufacturer")
    val manufacturer: String,
    @SerializedName("brand")
    val brand: String,
    @SerializedName("model")
    val model: String,
    @SerializedName("device")
    val device: String,
    @SerializedName("product")
    val product: String,
    @SerializedName("android_version")
    val androidVersion: String,
    @SerializedName("sdk_int")
    val sdkInt: Int,
    @SerializedName("export_timestamp")
    val exportTimestamp: Long,
    @SerializedName("export_date")
    val exportDate: String,
)

data class AppBackupItem(
    @SerializedName("name")
    val name: String,
    @SerializedName("package_name")
    val packageName: String,
    @SerializedName("version_name")
    val versionName: String,
    @SerializedName("version_code")
    val versionCode: Long,
    @SerializedName("first_install_time")
    val firstInstallTime: Long,
    @SerializedName("last_update_time")
    val lastUpdateTime: Long,
    @SerializedName("installer")
    val installer: String?,
    @SerializedName("min_sdk_version")
    val minSdkVersion: Int?,
    @SerializedName("target_sdk_version")
    val targetSdkVersion: Int?,
)
