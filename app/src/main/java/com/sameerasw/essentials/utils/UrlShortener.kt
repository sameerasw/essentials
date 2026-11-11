/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - URL Shortener
 * File: UrlShortener.kt
 * Description: Zero-cost, high-speed URL shortener client with on-device history, user settings, tracking parameter removal, configurable TTL expiration (1h, 2h, 12h, 24h, 48h, Never), and offline stateless fallback.
 */

package com.sameerasw.essentials.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.receivers.UrlShortenerActionReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object UrlShortener {

    private const val TAG = "UrlShortener"
    private const val PREFS_NAME = "essentials_prefs"
    private const val PREF_KEY_HISTORY = "url_shortener_history"
    private const val PREF_KEY_CUSTOM_DOMAIN = "url_shortener_custom_domain"
    private const val PREF_KEY_DEFAULT_EXPIRATION = "url_shortener_default_expiration"
    private const val PREF_KEY_AUTO_STRIP_TRACKING = "url_shortener_auto_strip_tracking"
    private const val PREF_KEY_AUTO_COPY = "url_shortener_auto_copy"
    private const val PREF_KEY_NOTIFICATIONS_ENABLED = "url_shortener_notifications_enabled"

    const val NOTIFICATION_ID = 830830
    private const val NOTIFICATION_CHANNEL_ID = "url_shortener_channel"
    const val DEFAULT_DOMAIN = "https://btl.dpdns.org"

    sealed class ShortenException(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class LockdownException(message: String = "Shortener service is currently in lockdown mode. Cannot create URL right now.") : ShortenException(message)
        class SlugConflictException(val slug: String, message: String = "Custom alias '$slug' is already taken.") : ShortenException(message)
        class ReservedAliasException(val slug: String, message: String = "This alias is reserved for administrator use. Please choose a different custom alias.") : ShortenException(message)
        class InvalidUrlException(message: String = "Invalid URL") : ShortenException(message)
        class InvalidAliasException(message: String = "Invalid custom alias") : ShortenException(message)
        class NetworkException(val isOffline: Boolean = false, message: String = "Network error occurred") : ShortenException(message)
        class GenericException(message: String, cause: Throwable? = null) : ShortenException(message, cause)
    }

    enum class Expiration(val key: String, val label: String, val seconds: Long) {
        ONE_HOUR("1h", "1h", 3600),
        TWO_HOURS("2h", "2h", 7200),
        TWELVE_HOURS("12h", "12h", 43200),
        TWENTY_FOUR_HOURS("24h", "24h", 86400),
        FORTY_EIGHT_HOURS("48h", "48h", 172800),
        NEVER("never", "Never", 0),
    }

    data class ShortLinkHistoryItem(
        val id: String = UUID.randomUUID().toString(),
        val originalUrl: String,
        val shortUrl: String,
        val expirationKey: String,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = 0,
        val isPasswordProtected: Boolean = false,
    ) {
        fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
            return expiresAt > 0 && nowMs > expiresAt
        }

        fun getRemainingTimeLabel(
            context: Context,
            nowMs: Long = System.currentTimeMillis(),
        ): String {
            if (expiresAt == 0L) return context.getString(R.string.shorten_permanent)
            val diffMs = expiresAt - nowMs
            if (diffMs <= 0) return context.getString(R.string.shorten_status_expired)
            val hours = diffMs / (1000 * 3600)
            val minutes = (diffMs % (1000 * 3600)) / (1000 * 60)
            val seconds = (diffMs % (1000 * 60)) / 1000
            return when {
                hours > 0 -> context.getString(R.string.shorten_time_left_hms, hours, minutes, seconds)
                minutes > 0 -> context.getString(R.string.shorten_time_left_ms, minutes, seconds)
                else -> context.getString(R.string.shorten_time_left_s, seconds)
            }
        }
    }

    // --- Settings Persistence ---

    fun getCustomDomain(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY_CUSTOM_DOMAIN, DEFAULT_DOMAIN) ?: DEFAULT_DOMAIN
    }

    fun setCustomDomain(context: Context, domain: String) {
        val clean = if (domain.isBlank()) DEFAULT_DOMAIN else domain.trim().trimEnd('/')
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_CUSTOM_DOMAIN, clean)
            .apply()
    }

    fun getDefaultExpiration(context: Context): Expiration {
        val key = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_KEY_DEFAULT_EXPIRATION, Expiration.TWENTY_FOUR_HOURS.key)
        return Expiration.entries.find { it.key == key } ?: Expiration.TWENTY_FOUR_HOURS
    }

    fun setDefaultExpiration(context: Context, expiration: Expiration) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_DEFAULT_EXPIRATION, expiration.key)
            .apply()
    }

    fun isAutoStripTrackingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_KEY_AUTO_STRIP_TRACKING, true)
    }

    fun setAutoStripTrackingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_KEY_AUTO_STRIP_TRACKING, enabled)
            .apply()
    }

    fun isAutoCopyEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_KEY_AUTO_COPY, true)
    }

    fun setAutoCopyEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_KEY_AUTO_COPY, enabled)
            .apply()
    }

    fun isNotificationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }

    fun showShortLinkNotification(
        context: Context,
        shortUrl: String,
        expiration: Expiration = Expiration.TWENTY_FOUR_HOURS,
    ) {
        if (!isNotificationEnabled(context)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.tile_url_shortener),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.tile_url_shortener_subtitle)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        // Action 1: Copy
        val copyIntent = Intent(context, UrlShortenerActionReceiver::class.java).apply {
            action = UrlShortenerActionReceiver.ACTION_COPY
            putExtra(UrlShortenerActionReceiver.EXTRA_URL, shortUrl)
            putExtra(UrlShortenerActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action 2: Share
        val shareIntent = Intent(context, UrlShortenerActionReceiver::class.java).apply {
            action = UrlShortenerActionReceiver.ACTION_SHARE
            putExtra(UrlShortenerActionReceiver.EXTRA_URL, shortUrl)
            putExtra(UrlShortenerActionReceiver.EXTRA_NOTIFICATION_ID, NOTIFICATION_ID)
        }
        val sharePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Tap notification to open link in browser
        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(shortUrl))
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val expiryText = if (expiration.seconds > 0) expiration.label else context.getString(R.string.shorten_never_expire)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.rounded_link_24)
            .setContentTitle("${context.getString(R.string.shorten_url_title)} ($expiryText)")
            .setContentText(shortUrl)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.rounded_link_24, context.getString(R.string.shorten_copy_link), copyPendingIntent)
            .addAction(R.drawable.rounded_devices_24, context.getString(R.string.shorten_share_link), sharePendingIntent)
            .build()

        try {
            nm.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Could not post short link notification", e)
        }
    }

    // --- Core Shortening Engine ---

    fun sanitizeUrl(rawUrl: String): String {
        var clean = rawUrl.trim()
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "https://$clean"
        }
        return clean
    }

    fun cleanTrackingParameters(url: String): String {
        return try {
            val sanitized = sanitizeUrl(url)
            val uri = Uri.parse(sanitized)
            val trackingKeys = setOf(
                "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
                "fbclid", "gclid", "dclid", "igshid", "si", "ref", "ref_src", "feature",
                "_ga", "_gl", "mc_cid", "mc_eid", "yclid", "mkt_tok", "trk",
            )
            val builder = uri.buildUpon().clearQuery()
            for (param in uri.queryParameterNames) {
                if (!trackingKeys.contains(param.lowercase())) {
                    for (value in uri.getQueryParameters(param)) {
                        builder.appendQueryParameter(param, value)
                    }
                }
            }
            builder.build().toString()
        } catch (_: Exception) {
            url
        }
    }

    fun generateStatelessShortUrl(
        url: String,
        expiration: Expiration = Expiration.TWENTY_FOUR_HOURS,
        baseDomain: String = DEFAULT_DOMAIN,
    ): String {
        val cleanedUrl = cleanTrackingParameters(url)
        val nowSec = System.currentTimeMillis() / 1000
        val expiryHex = if (expiration.seconds > 0) {
            (nowSec + expiration.seconds).toString(16)
        } else {
            "0"
        }
        val encodedUrl = Base64.encodeToString(
            cleanedUrl.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        val domain = baseDomain.trimEnd('/')
        return "$domain/e_${expiryHex}_$encodedUrl"
    }

    suspend fun shortenUrl(
        url: String,
        expiration: Expiration = Expiration.TWENTY_FOUR_HOURS,
        customSlug: String? = null,
        passcode: String? = null,
        customDomain: String? = null,
        context: Context? = null,
    ): String = withContext(Dispatchers.IO) {
        val rawClean = url.trim()
        if (rawClean.isBlank() || (!rawClean.startsWith("http://", ignoreCase = true) && !rawClean.startsWith("https://", ignoreCase = true) && !rawClean.contains("."))) {
            throw ShortenException.InvalidUrlException("Please enter a valid URL.")
        }

        val trimmedSlug = customSlug?.trim()?.takeIf { it.isNotBlank() }
        if (trimmedSlug != null) {
            if (trimmedSlug.length > 32 || !trimmedSlug.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
                throw ShortenException.InvalidAliasException("Custom alias can only contain letters, numbers, hyphens, and underscores (max 32 chars).")
            }
        }

        val trimmedPasscode = passcode?.trim()?.takeIf { it.isNotBlank() }

        val shouldClean = context?.let { isAutoStripTrackingEnabled(it) } ?: true
        val cleanedUrl = if (shouldClean) cleanTrackingParameters(rawClean) else sanitizeUrl(rawClean)
        val domainFromSettings = context?.let { getCustomDomain(it) }
        val baseDomain = (customDomain?.takeIf { it.isNotBlank() } ?: domainFromSettings ?: DEFAULT_DOMAIN).trimEnd('/')
        val now = System.currentTimeMillis()
        val expiresAt = if (expiration.seconds > 0) now + expiration.seconds * 1000 else 0L

        var finalShortUrl: String? = null
        var isProtected = trimmedPasscode != null

        try {
            val endpoint = URL("$baseDomain/api/shorten")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-Client-App", "Essentials")
                setRequestProperty("Connection", "close")
                connectTimeout = 2000
                readTimeout = 2000
                instanceFollowRedirects = false
                doOutput = true
            }

            val payload = JSONObject().apply {
                put("url", cleanedUrl)
                put("expiration", expiration.key)
                if (trimmedSlug != null) {
                    put("customSlug", trimmedSlug)
                }
                if (trimmedPasscode != null) {
                    put("passcode", trimmedPasscode)
                }
                put("stripTracking", shouldClean)
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val shortUrl = json.optString("shortUrl")
                if (shortUrl.isNotBlank()) {
                    finalShortUrl = shortUrl
                }
                if (json.optBoolean("isPasswordProtected")) {
                    isProtected = true
                }
            } else if (responseCode == 503) {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val errMsg = try { JSONObject(errBody ?: "").optString("error") } catch (_: Exception) { null }
                throw ShortenException.LockdownException(errMsg ?: "Shortener service is currently in lockdown mode. Cannot create URL right now.")
            } else if (responseCode == 403) {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val errMsg = try { JSONObject(errBody ?: "").optString("error") } catch (_: Exception) { null }
                throw ShortenException.ReservedAliasException(trimmedSlug ?: "", errMsg ?: "This alias is reserved for administrator use. Please choose a different custom alias.")
            } else if (responseCode == 409) {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val errMsg = try { JSONObject(errBody ?: "").optString("error") } catch (_: Exception) { null }
                throw ShortenException.SlugConflictException(trimmedSlug ?: "", errMsg ?: "Custom alias '$trimmedSlug' is already in use.")
            } else if (responseCode == 400) {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val errMsg = try { JSONObject(errBody ?: "").optString("error") } catch (_: Exception) { null }
                throw ShortenException.InvalidUrlException(errMsg ?: "Invalid URL or custom alias format.")
            } else {
                val errBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
                val errMsg = try { JSONObject(errBody ?: "").optString("error") } catch (_: Exception) { null }
                throw ShortenException.GenericException(errMsg ?: "Failed to create short link on server (Code $responseCode)")
            }
        } catch (e: ShortenException) {
            throw e
        } catch (e: java.net.UnknownHostException) {
            throw ShortenException.NetworkException(isOffline = true, message = "No internet connection. Unable to sync short link with server.")
        } catch (e: java.net.SocketTimeoutException) {
            throw ShortenException.NetworkException(isOffline = false, message = "Edge server timed out. Please try again.")
        } catch (e: Exception) {
            throw ShortenException.GenericException("Failed to register short link: ${e.message}", e)
        }

        val resultUrl = finalShortUrl ?: throw ShortenException.GenericException("Failed to create short link.")

        // Save to user's local device history & post instant action notification
        if (context != null) {
            saveToHistory(
                context = context,
                item = ShortLinkHistoryItem(
                    originalUrl = cleanedUrl,
                    shortUrl = resultUrl,
                    expirationKey = expiration.key,
                    createdAt = now,
                    expiresAt = expiresAt,
                    isPasswordProtected = isProtected,
                )
            )
            showShortLinkNotification(context, resultUrl, expiration)
        }

        return@withContext resultUrl
    }

    fun getHistory(context: Context): List<ShortLinkHistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(PREF_KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<ShortLinkHistoryItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ShortLinkHistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        originalUrl = obj.getString("originalUrl"),
                        shortUrl = obj.getString("shortUrl"),
                        expirationKey = obj.optString("expirationKey", "24h"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        expiresAt = obj.optLong("expiresAt", 0),
                        isPasswordProtected = obj.optBoolean("isPasswordProtected", false),
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse history", e)
        }
        return list.sortedByDescending { it.createdAt }
    }

    fun saveToHistory(context: Context, item: ShortLinkHistoryItem) {
        val currentHistory = getHistory(context).toMutableList()
        currentHistory.removeAll { it.shortUrl == item.shortUrl }
        currentHistory.add(0, item)

        val trimmed = if (currentHistory.size > 50) currentHistory.take(50) else currentHistory
        val array = JSONArray()
        for (h in trimmed) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("originalUrl", h.originalUrl)
                put("shortUrl", h.shortUrl)
                put("expirationKey", h.expirationKey)
                put("createdAt", h.createdAt)
                put("expiresAt", h.expiresAt)
                put("isPasswordProtected", h.isPasswordProtected)
            }
            array.put(obj)
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_HISTORY, array.toString())
            .apply()
    }

    fun deleteFromHistory(context: Context, id: String) {
        val current = getHistory(context).filterNot { it.id == id }
        val array = JSONArray()
        for (h in current) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("originalUrl", h.originalUrl)
                put("shortUrl", h.shortUrl)
                put("expirationKey", h.expirationKey)
                put("createdAt", h.createdAt)
                put("expiresAt", h.expiresAt)
                put("isPasswordProtected", h.isPasswordProtected)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_HISTORY, array.toString())
            .apply()
    }

    fun clearHistory(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREF_KEY_HISTORY)
            .apply()
    }
}
