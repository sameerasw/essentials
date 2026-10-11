package com.sameerasw.island.sdk

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

object IslandIdentity {
    @Suppress("DEPRECATION")
    fun fingerprint(context: Context, packageName: String): String? = try {
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
        }
        signatures?.takeIf { it.isNotEmpty() }?.joinToString(",") { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    } catch (_: Exception) {
        null
    }

    fun shortFingerprint(fingerprint: String): String =
        fingerprint.substringBefore(',').chunked(2).take(8).joinToString(":").uppercase()

    fun label(context: Context, packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (_: Exception) {
        packageName
    }
}
