package com.sameerasw.essentials.services

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.MapsState
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.services.receivers.FlashlightActionReceiver
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.AppUtil
import com.sameerasw.essentials.utils.HapticUtil
import kotlin.math.abs

class NotificationListener : NotificationListenerService() {
    
    companion object {
        const val ACTION_LIKE_CURRENT_SONG = "com.sameerasw.essentials.ACTION_LIKE_CURRENT_SONG"
        const val ACTION_REQUEST_AMBIENT_GLANCE = "com.sameerasw.essentials.ACTION_REQUEST_AMBIENT_GLANCE"
    }

    private val likeActionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_LIKE_CURRENT_SONG) {
                handleLikeSongAction()
            } else if (intent?.action == ACTION_REQUEST_AMBIENT_GLANCE) {
                handleRequestAmbientGlance()
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        try {
            val filter = android.content.IntentFilter().apply {
                addAction(ACTION_LIKE_CURRENT_SONG)
                addAction(ACTION_REQUEST_AMBIENT_GLANCE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(likeActionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(likeActionReceiver, filter)
            }

            // Initial discovery from active notifications
            activeNotifications?.forEach { sbn ->
                val isSystem = sbn.packageName == "android" || sbn.packageName == "com.android.systemui"
                if (isSystem) {
                    discoverSystemChannel(sbn.packageName, sbn.notification.channelId, sbn.user)
                }
            }
        } catch (_: Exception) {}
    }

    private fun discoverSystemChannel(packageName: String, channelId: String?, userHandle: android.os.UserHandle) {
        if (channelId.isNullOrBlank()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                val discoveredJson = prefs.getString("snooze_discovered_channels", null)
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.SnoozeChannel>>() {}.type
                val discoveredChannels: MutableList<com.sameerasw.essentials.domain.model.SnoozeChannel> = if (discoveredJson != null) {
                    try { gson.fromJson(discoveredJson, type) ?: mutableListOf() } catch (_: Exception) { mutableListOf() }
                } else mutableListOf()

                if (discoveredChannels.none { it.id == channelId }) {
                    var foundName: String? = null
                    try {
                        val channels = getNotificationChannels(packageName, userHandle)
                        val channel = channels.find { it.id == channelId }
                        foundName = channel?.name?.toString()
                    } catch (_: Exception) {}

                    val name = if (!foundName.isNullOrBlank()) foundName 
                               else channelId.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
                    
                    val finalName = if (packageName == "android") name else "[$packageName] $name"
                    
                    discoveredChannels.add(com.sameerasw.essentials.domain.model.SnoozeChannel(channelId, finalName))
                    prefs.edit().putString("snooze_discovered_channels", gson.toJson(discoveredChannels)).apply()
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(likeActionReceiver)
        } catch (_: Exception) {}
    }

    private fun handleRequestAmbientGlance() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val componentName = android.content.ComponentName(this, NotificationListener::class.java)
            val sessions = mediaSessionManager.getActiveSessions(componentName)
            
            val activeSession = sessions.firstOrNull { 
                it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING 
            } ?: return
            
            triggerAmbientGlance(activeSession, "play_pause", bypassInteractiveCheck = true)
        } catch (_: Exception) {}
    }

    private fun handleLikeSongAction() {
        try {
            val mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as android.media.session.MediaSessionManager
            val componentName = android.content.ComponentName(this, NotificationListener::class.java)
            val sessions = mediaSessionManager.getActiveSessions(componentName)
            
            // Check if toast is enabled
            val prefs = getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
            val showToast = prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_LIKE_SONG_TOAST_ENABLED, true)
            
            // STRICT: Only target playing sessions
            val activeSession = sessions.firstOrNull { 
                it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING 
            } ?: return

            if (isLikedState(activeSession)) {
                if (showToast) android.widget.Toast.makeText(applicationContext, "Already Liked \u2665", android.widget.Toast.LENGTH_SHORT).show()
                triggerAmbientGlance(activeSession, "like", true)
                return
            }

            val playbackState = activeSession.playbackState
            if (playbackState != null) {
                for (action in playbackState.customActions) {
                    val name = action.name.toString()
                    val isLike = name.contains("Like", ignoreCase = true) || 
                                 name.contains("Heart", ignoreCase = true) ||
                                 name.contains("Favorite", ignoreCase = true) ||
                                 name.contains("Love", ignoreCase = true) ||
                                 name.contains("ThumbsUp", ignoreCase = true) ||
                                 name.contains("Thumbs Up", ignoreCase = true) ||
                                 name.contains("Add to collection", ignoreCase = true) ||
                                 name.contains("Add to library", ignoreCase = true) ||
                                 name.contains("Add to favorites", ignoreCase = true) ||
                                 name.contains("Save to", ignoreCase = true)
                    
                    if (isLike) {
                        activeSession.transportControls.sendCustomAction(action, action.extras)
                        if (showToast) android.widget.Toast.makeText(applicationContext, "Liked song \u2665", android.widget.Toast.LENGTH_SHORT).show()
                        
                        triggerAmbientGlance(activeSession, "like", true)
                        return
                    }
                }
            }

            val sbn = activeNotifications?.find { it.packageName == activeSession.packageName }
            if (sbn != null) {
                val actions = sbn.notification.actions
                if (actions != null) {
                    for (action in actions) {
                        val title = action.title?.toString() ?: ""
                        val isLike = title.contains("Like", ignoreCase = true) || 
                                     title.contains("Heart", ignoreCase = true) ||
                                     title.contains("Favorite", ignoreCase = true) ||
                                     title.contains("Love", ignoreCase = true) ||
                                     title.contains("ThumbsUp", ignoreCase = true) ||
                                     title.contains("Add to", ignoreCase = true) ||
                                     title.contains("Save", ignoreCase = true)

                        if (isLike) {
                            action.actionIntent.send()
                            if (showToast) android.widget.Toast.makeText(applicationContext, "Liked song \u2665", android.widget.Toast.LENGTH_SHORT).show()
                            
                            triggerAmbientGlance(activeSession, "like", true)
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isLikedState(activeSession: android.media.session.MediaController): Boolean {
        try {
            // 1. Check Metadata
            val metadata = activeSession.metadata
            if (metadata != null) {
                val rating = metadata.getRating(android.media.MediaMetadata.METADATA_KEY_USER_RATING)
                if (rating != null && rating.isRated) {
                    val isLiked = rating.hasHeart() || rating.isThumbUp || 
                                 (rating.ratingStyle == android.media.Rating.RATING_3_STARS && rating.starRating > 0) ||
                                 (rating.ratingStyle == android.media.Rating.RATING_4_STARS && rating.starRating > 0) ||
                                 (rating.ratingStyle == android.media.Rating.RATING_5_STARS && rating.starRating > 0) ||
                                 (rating.ratingStyle == android.media.Rating.RATING_PERCENTAGE && rating.percentRating >= 50)
                    if (isLiked) return true
                }
            }

            // 2. Check Custom Actions
            val playbackState = activeSession.playbackState
            if (playbackState != null) {
                for (action in playbackState.customActions) {
                    val name = action.name.toString()
                    if (name.contains("Playlist", ignoreCase = true) || 
                        name.contains("Queue", ignoreCase = true) ||
                        name.contains("Dislike", ignoreCase = true) || 
                        name.contains("ThumbsDown", ignoreCase = true)) continue

                    val isAlreadyLikedState = name.contains("Unlike", ignoreCase = true) || 
                                              name.contains("Unheart", ignoreCase = true) ||
                                              name.contains("Remove from collection", ignoreCase = true) ||
                                              name.contains("Remove from library", ignoreCase = true) ||
                                              name.contains("Remove from favorites", ignoreCase = true) ||
                                              name.contains("Saved", ignoreCase = true) ||
                                              name.contains("In your library", ignoreCase = true) ||
                                              name.contains("In your favorites", ignoreCase = true) ||
                                              name.equals("Added", ignoreCase = true)
                    if (isAlreadyLikedState) return true
                }
            }

            // 3. Check Notification Actions
            val notifications = activeNotifications
            val sbn = notifications?.find { it.packageName == activeSession.packageName }
            if (sbn != null) {
                val actions = sbn.notification.actions
                if (actions != null) {
                    for (action in actions) {
                        val title = action.title?.toString() ?: ""
                        if (title.contains("Playlist", ignoreCase = true) || 
                            title.contains("Queue", ignoreCase = true) ||
                            title.contains("Dislike", ignoreCase = true) ||
                            title.contains("ThumbsDown", ignoreCase = true) ||
                            title.contains("Thumbs Down", ignoreCase = true)) continue
                        
                        val isAlreadyLiked = title.contains("Unlike", ignoreCase = true) || 
                                             title.contains("Unheart", ignoreCase = true) ||
                                             title.contains("Remove from", ignoreCase = true) ||
                                             title.contains("Saved", ignoreCase = true) ||
                                             title.contains("In your", ignoreCase = true)
                        if (isAlreadyLiked) return true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private data class MediaState(
        val title: String?,
        val artist: String?,
        val isPlaying: Boolean,
        val isLiked: Boolean
    )
    
    private val lastMediaStates = mutableMapOf<String, MediaState>()

    private fun triggerAmbientGlance(
        activeSession: android.media.session.MediaController, 
        eventType: String, 
        isAlreadyLikedOverride: Boolean? = null,
        bypassInteractiveCheck: Boolean = false
    ) {
        val prefs = getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED, false)
        
        if (isEnabled) {
             val metadata = activeSession.metadata
             val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
             val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
             val isAlreadyLiked = isAlreadyLikedOverride ?: isLikedState(activeSession)
             val isDockedMode = prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE, false)
             
             // 1. Always Extract & Cache Album Art (Dictionary style)
             var bitmap = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
             if (bitmap == null) {
                 bitmap = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
             }
             
             if (title != null) {
                 val artHash = kotlin.math.abs("${title}_${artist}".hashCode())
                 val artFile = java.io.File(cacheDir, "art_$artHash.png")

                 if (bitmap != null) {
                     try {
                         val out = java.io.FileOutputStream(artFile)
                         bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                         out.flush()
                         out.close()

                         val tempFile = java.io.File(cacheDir, "temp_album_art.png")
                         val tempOut = java.io.FileOutputStream(tempFile)
                         bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, tempOut)
                         tempOut.flush()
                         tempOut.close()

                         // Cleanup old art files (Keep last 3)
                         val files = cacheDir.listFiles { _, name -> name.startsWith("art_") }
                         if (files != null && files.size > 3) {
                             files.sortByDescending { it.lastModified() }
                             for (i in 5 until files.size) {
                                 files[i].delete()
                             }
                         }
                     } catch (e: Exception) {
                         e.printStackTrace()
                     }
                 }
             }
                        // 2. Trigger Glance only if screen is OFF or Screensaver is Active
             val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
             val isDreaming = com.sameerasw.essentials.services.dreams.AmbientDreamService.isDreaming
             
             if (!powerManager.isInteractive || bypassInteractiveCheck || isDreaming) {
                  val intent = Intent("SHOW_AMBIENT_GLANCE").apply {
                      putExtra("event_type", eventType)
                      putExtra("track_title", title)
                      putExtra("artist_name", artist)
                      putExtra("is_already_liked", isAlreadyLiked)
                      putExtra("is_docked_mode", isDockedMode)
                      putExtra("package_name", activeSession.packageName)
                      setPackage(packageName)
                  }
                  sendBroadcast(intent)
             }
        }
    }
    
    private fun handleMediaUpdate(sbn: StatusBarNotification) {
         try {
             val extras = sbn.notification.extras
             val token = extras.getParcelable<android.media.session.MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)
             
             if (token != null) {
                 val controller = android.media.session.MediaController(this, token)
                 val metadata = controller.metadata
                 val playbackState = controller.playbackState
                 
                 val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                 val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                 val isPlaying = playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
                 
                 val lastState = lastMediaStates[sbn.packageName]
                 
                 var eventType: String? = null
                 
                 
                 val isLiked = isLikedState(controller)

                 if (lastState == null) {
                     if (isPlaying) {
                         eventType = "play_pause"
                     }
                 } else {
                     val titleChanged = title != lastState.title
                     val stateChanged = isPlaying != lastState.isPlaying
                     val likedChanged = isLiked != lastState.isLiked
                     
                     if (titleChanged) {
                         eventType = "track_change"
                     } else if (stateChanged) {
                         if (isPlaying) {
                             eventType = "play_pause"
                         }
                     } else if (likedChanged) {
                         eventType = "like"
                     }
                 }
                 
                 lastMediaStates[sbn.packageName] = MediaState(title, artist, isPlaying, isLiked)
                 
                 val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                 prefs.edit()
                     .putString("current_media_title", title)
                     .putString("current_media_artist", artist)
                     .putBoolean("current_media_is_liked", isLiked)
                     .apply()
                 
                 if (eventType != null) {
                     triggerAmbientGlance(controller, eventType, isLiked)
                 }
             }
         } catch (e: Exception) {
             e.printStackTrace()
         }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        onNotificationPostedInternal(sbn)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap) {
        onNotificationPostedInternal(sbn)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onNotificationPostedInternal(sbn: StatusBarNotification) {
        // Skip our own app's notifications early to avoid flooding logs and redundant processing
        if (sbn.packageName == packageName) {
            return
        }

        val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

        // Maps navigation state update
        if (sbn.packageName == "com.google.android.apps.maps") {
            MapsState.hasNavigationNotification = isNavigationNotification(sbn)
        }

        // Handle Snooze System Notifications
        try {
            val pkg = sbn.packageName
            val isSystem = pkg == "android" || pkg.startsWith("com.android.") || pkg == "com.google.android.gms"
            
            if (isSystem) {
                val channelId = sbn.notification.channelId
                
                // 1. Discovery
                discoverSystemChannel(pkg, channelId, sbn.user)

                // 2. Snoozing
                if (channelId != null) {
                    val blockedChannelsJson = prefs.getString("snooze_blocked_channels", null)
                    val blockedChannels: Set<String> = if (blockedChannelsJson != null) {
                        try {
                            val type = object : com.google.gson.reflect.TypeToken<Set<String>>() {}.type
                            com.google.gson.Gson().fromJson(blockedChannelsJson, type) ?: emptySet()
                        } catch (_: Exception) { emptySet() }
                    } else emptySet()

                    if (blockedChannels.contains(channelId)) {
                        snoozeNotification(sbn.key, 24 * 60 * 60 * 1000L) // Snooze for 24 hours
                    }
                }
            }
        } catch (_: Exception) {
            // Safe to ignore
        }

        // trigger notification lighting for any newly posted notification if feature enabled
        try {
            handleCallVibrations(sbn)

            val packageName = sbn.packageName
            val notification = sbn.notification
            val extras = notification.extras

            // Skip media sessions
            val isMedia = extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                    extras.getString(Notification.EXTRA_TEMPLATE) == "android.app.Notification\$MediaStyle"
            
            if (isMedia) {
                handleMediaUpdate(sbn)
                return
            }

            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            
            // Skip silent notifications if enabled
            val skipSilent = prefs.getBoolean("edge_lighting_skip_silent", true)
            if (skipSilent) {
                val ranking = Ranking()
                if (currentRanking.getRanking(sbn.key, ranking)) {
                    val importance = ranking.importance
                    val isSilent = importance <= android.app.NotificationManager.IMPORTANCE_LOW
                    if (isSilent) {
                        return
                    }
                }
            }
            
            // Skip persistent notifications if enabled
            val skipPersistent = prefs.getBoolean("edge_lighting_skip_persistent", false)
            if (skipPersistent && isPersistentNotification(notification)) {
                return
            }

            val enabled = prefs.getBoolean("edge_lighting_enabled", false)
            if (enabled) {
                // Check all required permissions before triggering notification lighting
                val hasPermissions = hasAllRequiredPermissions()
                if (hasPermissions) {
                    // Check if the app is selected for notification lighting
                    val appSelected = isAppSelectedForNotificationLighting(sbn.packageName)
                    if (appSelected) {
                        val cornerRadius = try {
                            prefs.getFloat("edge_lighting_corner_radius", 20f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_corner_radius", 20).toFloat()
                        }
                        val strokeThickness = try {
                            prefs.getFloat("edge_lighting_stroke_thickness", 8f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_stroke_thickness", 8).toFloat()
                        }
                        val colorModeName = prefs.getString("edge_lighting_color_mode", NotificationLightingColorMode.SYSTEM.name)
                        val colorMode = NotificationLightingColorMode.valueOf(colorModeName ?: NotificationLightingColorMode.SYSTEM.name)
                        val pulseCount = try {
                            prefs.getInt("edge_lighting_pulse_count", 1)
                        } catch (e: ClassCastException) {
                            prefs.getFloat("edge_lighting_pulse_count", 1f).toInt()
                        }
                        val pulseDuration = try {
                            prefs.getFloat("edge_lighting_pulse_duration", 3000f).toLong()
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_pulse_duration", 3000).toLong()
                        }
                        val styleName = prefs.getString("edge_lighting_style", com.sameerasw.essentials.domain.model.NotificationLightingStyle.STROKE.name)
                        
                        val gson = com.google.gson.Gson()
                        val glowSidesJson = prefs.getString("edge_lighting_glow_sides", null)
                        val glowSides: Set<NotificationLightingSide> = if (glowSidesJson != null) {
                            val type = object : com.google.gson.reflect.TypeToken<Set<NotificationLightingSide>>() {}.type
                            try { gson.fromJson(glowSidesJson, type) } catch (_: Exception) { setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT) }
                        } else {
                            setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
                        }
                        
                        val indicatorX = try {
                            prefs.getFloat("edge_lighting_indicator_x", 50f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_indicator_x", 50).toFloat()
                        }
                        val indicatorY = try {
                            prefs.getFloat("edge_lighting_indicator_y", 2f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_indicator_y", 2).toFloat()
                        }
                        val indicatorScale = try {
                            prefs.getFloat("edge_lighting_indicator_scale", 1.0f)
                        } catch (e: ClassCastException) {
                            prefs.getInt("edge_lighting_indicator_scale", 1).toFloat()
                        }

                        fun startNotificationLighting(resolvedColor: Int? = null) {
                            val intent = Intent(applicationContext, NotificationLightingService::class.java).apply {
                                putExtra("corner_radius_dp", cornerRadius)
                                putExtra("stroke_thickness_dp", strokeThickness)
                                putExtra("color_mode", colorMode.name)
                                putExtra("pulse_count", pulseCount)
                                putExtra("pulse_duration", pulseDuration)
                                putExtra("style", styleName)
                                putExtra("glow_sides", glowSides.map { it.name }.toTypedArray())
                                putExtra("indicator_x", indicatorX)
                                putExtra("indicator_y", indicatorY)
                                putExtra("indicator_scale", indicatorScale)
                                if (resolvedColor != null) {
                                    putExtra("resolved_color", resolvedColor)
                                } else if (colorMode == NotificationLightingColorMode.CUSTOM) {
                                    putExtra("custom_color", prefs.getInt("edge_lighting_custom_color", 0xFF6200EE.toInt()))
                                }
                                putExtra("is_ambient_display", prefs.getBoolean("edge_lighting_ambient_display", false))
                                putExtra("is_ambient_show_lock_screen", prefs.getBoolean("edge_lighting_ambient_show_lock_screen", false))
                            }
                            applicationContext.startForegroundService(intent)
                        }

                        if (colorMode == NotificationLightingColorMode.APP_SPECIFIC) {
                            AppUtil.getAppBrandColor(applicationContext, sbn.packageName) { brandColor ->
                                startNotificationLighting(brandColor)
                            }
                        } else {
                            startNotificationLighting()
                        }

                        // Also trigger flashlight pulse if enabled
                        if (prefs.getBoolean("flashlight_pulse_enabled", false)) {
                            val pulseIntent = Intent(applicationContext, FlashlightActionReceiver::class.java).apply {
                                action = FlashlightActionReceiver.ACTION_PULSE_NOTIFICATION
                            }
                            applicationContext.sendBroadcast(pulseIntent)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // ignore failures
        }
    }

    private val lastCallVibrateTime = mutableMapOf<String, Long>()

    private fun handleCallVibrations(sbn: StatusBarNotification) {
        try {
            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean(SettingsRepository.KEY_CALL_VIBRATIONS_ENABLED, false)) return

            val notification = sbn.notification
            val extras = notification.extras ?: return
            
            val pkg = sbn.packageName
            val isDialer = pkg.contains("dialer") || pkg.contains("telecom") || pkg.contains("phone") || pkg.contains("miui.voiceassist")
            if (!isDialer) return

            val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            if (!isOngoing) return

            val hasChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false)
            
            if (hasChronometer) {
                val lastVibrate = lastCallVibrateTime[sbn.key] ?: 0L
                val now = System.currentTimeMillis()
                
                if (now - lastVibrate > 5000) {
                    HapticUtil.performHapticForService(applicationContext, HapticFeedbackType.DOUBLE)
                    lastCallVibrateTime[sbn.key] = now
                    Log.d("NotificationListener", "Outgoing/Incoming call answer detected for ${sbn.packageName}")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationListener", "Error in handleCallVibrations", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        lastCallVibrateTime.remove(sbn.key)
        if (sbn.packageName == "com.google.android.apps.maps") {
            MapsState.hasNavigationNotification = false
        }
    }

    private fun hasAllRequiredPermissions(): Boolean {
        // Check overlay permission
        if (!canDrawOverlays()) {
            return false
        }

        // Check accessibility service is enabled - only required for Android 12+ AOD support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isAccessibilityServiceEnabled()) {
                return false
            }
        }

        return true
    }

    private fun canDrawOverlays(): Boolean {
            return Settings.canDrawOverlays(applicationContext)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val serviceName = "${applicationContext.packageName}/${ScreenOffAccessibilityService::class.java.name}"
            enabledServices?.contains(serviceName) == true
        } catch (_: Exception) {
            false
        }
    }

    private fun isNavigationNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        if (!isPersistentNotification(notification)) return false
        return hasNavigationCategory(notification)
    }

    private fun isPersistentNotification(notification: Notification): Boolean {
        return (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
    }

    private fun hasNavigationCategory(notification: Notification): Boolean {
        val category = notification.category ?: return false
        val navigationRegex = Regex("(?i).*navigation.*")
        return navigationRegex.containsMatchIn(category)
    }

    private fun isAppSelectedForNotificationLighting(packageName: String): Boolean {
        try {
            val prefs = applicationContext.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

            // Check if only show when screen off is enabled
            val onlyShowWhenScreenOff = prefs.getBoolean("edge_lighting_only_screen_off", true)
            if (onlyShowWhenScreenOff) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isScreenOn = powerManager.isInteractive
                if (isScreenOn) {
                    return false
                }
            }

            val json = prefs.getString("edge_lighting_selected_apps", null)
            if (json == null) {
                return true
            }

            // If no saved preferences, allow all apps by default

            val gson = com.google.gson.Gson()
            val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.domain.model.AppSelection>>() {}.type
            val selectedApps: List<com.sameerasw.essentials.domain.model.AppSelection> = gson.fromJson(json, type)

            // Find the app in the saved list
            val app = selectedApps.find { it.packageName == packageName }
            val result = app?.isEnabled ?: true
            return result

        } catch (_: Exception) {
            // If there's an error, default to allowing all apps (backward compatibility)
            return true
        }
    }
}