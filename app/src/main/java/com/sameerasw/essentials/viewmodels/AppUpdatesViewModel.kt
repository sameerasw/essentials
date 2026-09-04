/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: App Updates & Release Tracking
 * File: AppUpdatesViewModel.kt
 * Description: ViewModel managing GitHub repository tracking, update notifications,
 * release downloads, and in-app APK installation triggers.
 */

package com.sameerasw.essentials.viewmodels

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.GitHubRepository
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.NotificationApp
import org.json.JSONArray
import org.json.JSONObject
import com.sameerasw.essentials.domain.model.TrackedRepo
import com.sameerasw.essentials.domain.model.github.GitHubAsset
import com.sameerasw.essentials.domain.model.github.GitHubOwner
import com.sameerasw.essentials.domain.model.github.GitHubRelease
import com.sameerasw.essentials.domain.model.github.GitHubRepo
import com.sameerasw.essentials.utils.AppUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class AppUpdatesViewModel : ViewModel() {
    private val gitHubRepository = GitHubRepository()
    private val gson = Gson()

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    private val _isSearching = mutableStateOf(false)
    val isSearching: State<Boolean> = _isSearching

    private val _searchResult = mutableStateOf<GitHubRepo?>(null)
    val searchResult: State<GitHubRepo?> = _searchResult

    private val _latestRelease = mutableStateOf<GitHubRelease?>(null)
    val latestRelease: State<GitHubRelease?> = _latestRelease

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    private val _shouldDismissSheet = mutableStateOf(false)
    val shouldDismissSheet: State<Boolean> = _shouldDismissSheet

    private val _readmeContent = mutableStateOf<String?>(null)
    val readmeContent: State<String?> = _readmeContent

    private val _selectedApp = mutableStateOf<NotificationApp?>(null)
    val selectedApp: State<NotificationApp?> = _selectedApp

    private val _trackedRepos = mutableStateOf<List<TrackedRepo>>(emptyList())
    val trackedRepos: State<List<TrackedRepo>> = _trackedRepos

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _refreshingRepoIds = mutableStateOf<Set<String>>(emptySet())
    val refreshingRepoIds: State<Set<String>> = _refreshingRepoIds

    private val _updateProgress = mutableStateOf(0f)
    val updateProgress: State<Float> = _updateProgress

    // New options state
    private val _allowPreReleases = mutableStateOf(false)
    val allowPreReleases: State<Boolean> = _allowPreReleases

    private val _notificationsEnabled = mutableStateOf(true)
    val notificationsEnabled: State<Boolean> = _notificationsEnabled

    private val _selectedApkName = mutableStateOf("Auto")
    val selectedApkName: State<String> = _selectedApkName

    /**
     * Executes the set selected apk name operation.
     *
     * @param name [String] Target name.
     */
    fun setSelectedApkName(name: String) {
        _selectedApkName.value = name
    }

    private val _installingRepoId = mutableStateOf<String?>(null)
    val installingRepoId: State<String?> = _installingRepoId

    private val _installStatus = mutableStateOf<String?>(null)
    val installStatus: State<String?> = _installStatus

    /**
     * Executes the on search query changed operation.
     *
     * @param query [String] Target query.
     */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
    }

    /**
     * Executes the on app selected operation.
     *
     * @param app [NotificationApp?] Target app.
     */
    fun onAppSelected(app: NotificationApp?) {
        _selectedApp.value = app
    }

    /**
     * Executes the load tracked repos operation.
     *
     * @param context [Context] Target context.
     */
    fun loadTrackedRepos(context: Context) {
        _isLoading.value = true
        viewModelScope.launch {
            _trackedRepos.value = SettingsRepository(context).getTrackedRepos()
            _isLoading.value = false
        }
    }

    /**
     * Executes the search repo operation.
     *
     * @param context [Context] Target context.
     */
    fun searchRepo(context: Context) {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return

        val parts = parseRepoQuery(query)
        if (parts == null) {
            _errorMessage.value = context.getString(R.string.error_invalid_repo_format)
            return
        }

        val (owner, repo) = parts
        if (owner.lowercase() == "sameerasw" && repo.lowercase() == "essentials") {
            _errorMessage.value = context.getString(R.string.msg_restrict_own_app_repo)
            _shouldDismissSheet.value = true
            return
        }
        _isSearching.value = true
        _errorMessage.value = null
        _searchResult.value = null
        _latestRelease.value = null
        _readmeContent.value = null
        _selectedApp.value = null

        viewModelScope.launch {
            try {
                val token = SettingsRepository(context).getGitHubToken()
                val repoInfo = gitHubRepository.getRepoInfo(owner, repo, token)
                if (repoInfo == null) {
                    _errorMessage.value = context.getString(R.string.error_repo_not_found)
                } else {
                    var release = gitHubRepository.getLatestRelease(owner, repo, token)
                    var isPreRelease = false

                    if (release == null) {
                        val releases = gitHubRepository.getReleases(owner, repo, token)
                        release = releases.firstOrNull()
                        if (release != null) {
                            isPreRelease = true
                        }
                    }

                    if (release == null || !release.assets.any { it.name.endsWith(".apk") }) {
                        _errorMessage.value = context.getString(R.string.error_no_apk_found)
                    } else {
                        _searchResult.value = repoInfo
                        _latestRelease.value = release
                        _readmeContent.value = gitHubRepository.getReadme(owner, repo, token)

                        if (isPreRelease || release.prerelease) {
                            _allowPreReleases.value = true
                        }

                        // Try to find matching installed app
                        findMatchingApp(context, repoInfo.name)
                    }
                }
            } catch (e: Exception) {
                if (e.message == "RATE_LIMIT") {
                    _errorMessage.value =
                        context.getString(R.string.error_rate_limited)
                } else {
                    _errorMessage.value = context.getString(R.string.error_generic_search)
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Executes the track repo operation.
     *
     * @param context [Context] Target context.
     * @param selectedApk [String] Target selected apk.
     */
    fun trackRepo(
        context: Context,
        selectedApk: String,
    ) {
        val repo = _searchResult.value ?: return
        val release = _latestRelease.value ?: return
        val app = _selectedApp.value

        val trackedRepo =
            TrackedRepo(
                owner = repo.owner.login,
                name = repo.name,
                fullName = repo.fullName,
                description = repo.description,
                stars = repo.stars,
                avatarUrl = repo.owner.avatarUrl,
                latestTagName = release.tagName,
                latestReleaseName = release.name,
                latestReleaseBody = release.body,
                latestReleaseUrl = release.htmlUrl,
                downloadUrl =
                    release.assets.find { it.name == selectedApk }?.downloadUrl
                        ?: release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl,
                publishedAt = release.publishedAt,
                selectedApkName = selectedApk,
                mappedPackageName = app?.packageName,
                mappedAppName = app?.appName,
                allowPreReleases = _allowPreReleases.value,
                notificationsEnabled = _notificationsEnabled.value,
            )

        SettingsRepository(context).addOrUpdateTrackedRepo(trackedRepo)
        loadTrackedRepos(context)
        clearSearch()
    }

    /**
     * Executes the untrack repo operation.
     *
     * @param context [Context] Target context.
     * @param fullName [String] Target full name.
     */
    fun untrackRepo(
        context: Context,
        fullName: String,
    ) {
        SettingsRepository(context).removeTrackedRepo(fullName)
        loadTrackedRepos(context)
    }

    /**
     * Executes the prepare edit operation.
     *
     * @param context [Context] Target context.
     * @param repo [TrackedRepo] Target repo.
     */
    fun prepareEdit(
        context: Context,
        repo: TrackedRepo,
    ) {
        _searchQuery.value = repo.fullName
        _isSearching.value = false
        _errorMessage.value = null

        // Build local fallback repo info from cache so it displays instantly
        val localOwner =
            GitHubOwner(
                login = repo.owner,
                avatarUrl = repo.avatarUrl,
            )
        _searchResult.value =
            GitHubRepo(
                id = 0L,
                name = repo.name,
                fullName = repo.fullName,
                description = repo.description,
                stars = repo.stars,
                owner = localOwner,
            )

        // Build local fallback release info from cache
        val localAssets =
            if (repo.downloadUrl != null) {
                listOf(
                    GitHubAsset(
                        name =
                            repo.selectedApkName.takeIf { it != "Auto" }
                                ?: repo.downloadUrl.substringAfterLast("/"),
                        downloadUrl = repo.downloadUrl,
                    ),
                )
            } else {
                emptyList()
            }

        _latestRelease.value =
            GitHubRelease(
                tagName = repo.latestTagName,
                name = repo.latestReleaseName,
                body = repo.latestReleaseBody,
                publishedAt = repo.publishedAt,
                htmlUrl = repo.latestReleaseUrl ?: "",
                prerelease = repo.allowPreReleases,
                assets = localAssets,
            )

        _readmeContent.value = null
        _allowPreReleases.value = repo.allowPreReleases
        _notificationsEnabled.value = repo.notificationsEnabled
        _selectedApkName.value = repo.selectedApkName

        _selectedApp.value = null
        viewModelScope.launch {
            if (repo.mappedPackageName != null) {
                val installedApps =
                    AppUtil.getAppsByPackageNames(context, listOf(repo.mappedPackageName))
                _selectedApp.value = installedApps.firstOrNull()
            }

            try {
                val token = SettingsRepository(context).getGitHubToken()
                val repoInfo = gitHubRepository.getRepoInfo(repo.owner, repo.name, token)
                val release = gitHubRepository.getLatestRelease(repo.owner, repo.name, token)

                if (repoInfo != null) {
                    _searchResult.value = repoInfo
                }
                if (release != null) {
                    _latestRelease.value = release
                }
                _readmeContent.value = gitHubRepository.getReadme(repo.owner, repo.name, token)
            } catch (e: Exception) {
                if (e.message == "RATE_LIMIT") {
                    _errorMessage.value = context.getString(R.string.error_rate_limited)
                }
            }
        }
    }

    private suspend fun findMatchingApp(
        context: Context,
        repoName: String,
    ) {
        val installedApps = AppUtil.getInstalledApps(context)
        // Simple name matching logic
        val normalizedRepoName =
            repoName
                .lowercase()
                .replace("-", "")
                .replace("_", "")
                .trim()

        val matchedApp =
            installedApps.find { app ->
                val normalizedAppName =
                    app.appName
                        .lowercase()
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("_", "")
                        .trim()
                normalizedAppName == normalizedRepoName ||
                    normalizedAppName.contains(normalizedRepoName) ||
                    normalizedRepoName.contains(normalizedAppName)
            }

        _selectedApp.value = matchedApp
    }

    private fun parseRepoQuery(query: String): Pair<String, String>? {
        // Handle https://github.com/owner/repo or github.com/owner/repo
        val urlPattern = Regex("(?:https?://)?(?:www\\.)?github\\.com/([^/]+)/([^/\\s?#]+).*")
        val urlMatch = urlPattern.matchEntire(query)
        if (urlMatch != null) {
            return urlMatch.groupValues[1] to urlMatch.groupValues[2]
        }

        // Handle owner/repo
        val simplePattern = Regex("([^/\\s]+)/([^/\\s]+)")
        val simpleMatch = simplePattern.matchEntire(query)
        if (simpleMatch != null) {
            return simpleMatch.groupValues[1] to simpleMatch.groupValues[2]
        }

        return null
    }

    /**
     * Executes the clear search operation.
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResult.value = null
        _latestRelease.value = null
        _errorMessage.value = null
        _readmeContent.value = null
        _allowPreReleases.value = false
        _notificationsEnabled.value = true
        _selectedApkName.value = "Auto"
    }

    /**
     * Executes the clear error operation.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Executes the consume dismiss signal operation.
     */
    fun consumeDismissSignal() {
        _shouldDismissSheet.value = false
    }

    /**
     * Executes the set allow pre releases operation.
     *
     * @param allow [Boolean] Target allow.
     */
    fun setAllowPreReleases(allow: Boolean) {
        _allowPreReleases.value = allow
    }

    /**
     * Executes the set notifications enabled operation.
     *
     * @param enabled [Boolean] Target enabled.
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    /**
     * Executes the fetch release notes if needed operation.
     *
     * @param context [Context] Target context.
     * @param repo [TrackedRepo] Target repo.
     */
    fun fetchReleaseNotesIfNeeded(
        context: Context,
        repo: TrackedRepo,
    ) {
        if (!repo.latestReleaseBody.isNullOrBlank()) return

        viewModelScope.launch {
            try {
                val token = SettingsRepository(context).getGitHubToken()

                val release =
                    if (repo.allowPreReleases) {
                        val releases = gitHubRepository.getReleases(repo.owner, repo.name, token)
                        releases.firstOrNull()
                    } else {
                        gitHubRepository.getLatestRelease(repo.owner, repo.name, token)
                    }

                if (release != null) {
                    // Update the cached repo with new details
                    val updatedRepo =
                        repo.copy(
                            latestTagName = release.tagName,
                            latestReleaseName = release.name,
                            latestReleaseBody = release.body,
                            latestReleaseUrl = release.htmlUrl,
                            downloadUrl =
                                release.assets.find { it.name == repo.selectedApkName }?.downloadUrl
                                    ?: release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl,
                            publishedAt = release.publishedAt,
                            // Keep existing flags
                        )
                    SettingsRepository(context).addOrUpdateTrackedRepo(updatedRepo)
                    loadTrackedRepos(context)
                }
            } catch (e: Exception) {
                if (e.message == "RATE_LIMIT") {
                    _errorMessage.value =
                        context.getString(R.string.error_rate_limited)
                }
                // Ignore others
            }
        }
    }

    /**
     * Executes the check for updates operation.
     *
     * @param context [Context] Target context.
     */
    fun checkForUpdates(context: Context) {
        if (_trackedRepos.value.isEmpty()) return

        viewModelScope.launch {
            val reposToCheck = _trackedRepos.value
            _refreshingRepoIds.value = reposToCheck.map { it.fullName }.toSet()
            _updateProgress.value = 0f
            var completedCount = 0

            val settingsRepo = SettingsRepository(context)
            val token = settingsRepo.getGitHubToken()

            val updatedRepos = reposToCheck.toMutableList()
            var changesMade = false

            for (i in updatedRepos.indices) {
                val repo = updatedRepos[i]
                try {
                    val release =
                        if (repo.allowPreReleases) {
                            val releases =
                                gitHubRepository.getReleases(repo.owner, repo.name, token)
                            releases.firstOrNull()
                        } else {
                            gitHubRepository.getLatestRelease(repo.owner, repo.name, token)
                        }

                    if (release != null) {
                        var isUpdateAvailable = false

                        if (repo.mappedPackageName != null) {
                            val installedVersion =
                                AppUtil.getAppVersion(context, repo.mappedPackageName)
                            if (installedVersion != null) {
                                isUpdateAvailable = compareVersions(
                                    release.tagName,
                                    installedVersion,
                                ) > 0
                            }
                        }

                        val newRepo =
                            repo.copy(
                                latestTagName = release.tagName,
                                latestReleaseName = release.name,
                                latestReleaseBody = release.body,
                                latestReleaseUrl = release.htmlUrl,
                                downloadUrl =
                                    release.assets.find { it.name == repo.selectedApkName }?.downloadUrl
                                        ?: release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl,
                                publishedAt = release.publishedAt,
                                isUpdateAvailable = isUpdateAvailable,
                                lastETag = null,
                            )

                        if (newRepo != repo) {
                            updatedRepos[i] = newRepo
                            changesMade = true
                        }
                    }
                } catch (e: Exception) {
                    if (e.message == "RATE_LIMIT") {
                        _errorMessage.value =
                            context.getString(R.string.error_rate_limited)
                        break
                    }
                } finally {
                    _refreshingRepoIds.value = _refreshingRepoIds.value - repo.fullName
                    completedCount++
                    _updateProgress.value = completedCount.toFloat() / reposToCheck.size
                }
            }

            if (changesMade) {
                settingsRepo.saveTrackedRepos(updatedRepos)
                _trackedRepos.value = updatedRepos
            }
        }
    }

    private fun compareVersions(
        v1: String,
        v2: String,
    ): Int {
        val cleanV1 = v1.replace(Regex("[^0-9.]"), "").split(".")
        val cleanV2 = v2.replace(Regex("[^0-9.]"), "").split(".")

        val length = maxOf(cleanV1.size, cleanV2.size)

        for (i in 0 until length) {
            val num1 = cleanV1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = cleanV2.getOrNull(i)?.toIntOrNull() ?: 0

            if (num1 > num2) return 1
            if (num1 < num2) return -1
        }
        return 0
    }

    /**
     * Executes the download and install operation.
     *
     * @param context [Context] Target context.
     * @param repo [TrackedRepo] Target repo.
     */
    fun downloadAndInstall(
        context: Context,
        repo: TrackedRepo,
    ) {
        val downloadUrl = repo.downloadUrl ?: return
        _installingRepoId.value = repo.fullName
        _installStatus.value = "Downloading..."
        _updateProgress.value = 0f

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Use external cache dir if possible for better accessibility by shell/shizuku
                val cacheDir = context.externalCacheDir ?: context.cacheDir
                val file = File(cacheDir, "${repo.name}.apk")

                // Ensure parent exists
                file.parentFile?.mkdirs()

                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned ${connection.responseCode}")
                }

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val output = FileOutputStream(file)

                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count
                    if (fileLength > 0) {
                        _updateProgress.value = total.toFloat() / fileLength
                    }
                    output.write(data, 0, count)
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    installApk(context, file, repo)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Download failed: ${e.message}"
                    _installingRepoId.value = null
                    _installStatus.value = null
                }
            }
        }
    }

    private fun installApk(
        context: Context,
        file: File,
        repo: TrackedRepo,
    ) {
        _installStatus.value = "Installing..."

        // Standard install
        try {
            val apkUri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

            @Suppress("DEPRECATION")
            val intent =
                Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    data = apkUri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
            context.startActivity(intent)
            _installingRepoId.value = null
            _installStatus.value = null
        } catch (e: Exception) {
            _errorMessage.value = "Install failed: ${e.message}"
            _installStatus.value = null
            _installingRepoId.value = null
        }
    }

    /**
     * Executes the export tracked repos operation.
     *
     * @param context [Context] Target context.
     * @param outputStream [OutputStream] Target output stream.
     */
    fun exportTrackedRepos(
        context: Context,
        outputStream: OutputStream,
    ) {
        try {
            val repos = SettingsRepository(context).getTrackedRepos()
            val json = gson.toJson(repos)
            outputStream.write(json.toByteArray())
            outputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                outputStream.close()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Executes the import tracked repos operation.
     *
     * @param context [Context] Target context.
     * @param inputStream [InputStream] Target input stream.
     * @return The resulting Boolean data.
     */
    fun importTrackedRepos(
        context: Context,
        inputStream: InputStream,
    ): Boolean =
        try {
            val json = inputStream.bufferedReader().use { it.readText() }
            val importedRepos = parseImportedRepos(context, json)
            if (importedRepos.isNotEmpty()) {
                val settingsRepo = SettingsRepository(context)
                val currentRepos = settingsRepo.getTrackedRepos().toMutableList()
                importedRepos.forEach { imported ->
                    val index = currentRepos.indexOfFirst { it.fullName == imported.fullName }
                    if (index != -1) {
                        currentRepos[index] = imported
                    } else {
                        currentRepos.add(imported)
                    }
                }
                settingsRepo.saveTrackedRepos(currentRepos)
                loadTrackedRepos(context)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
            }
        }

    private fun parseImportedRepos(
        context: Context,
        json: String,
    ): List<TrackedRepo> {
        // Try native export format first
        try {
            val nativeRepos = gson.fromJson(json, Array<TrackedRepo>::class.java)?.toList()
            if (!nativeRepos.isNullOrEmpty() && nativeRepos.all { it.fullName.isNotBlank() }) {
                return nativeRepos
            }
        } catch (e: Exception) {
        }

        // Try ObtainX / Obtainium export format
        try {
            val jsonObject = JSONObject(json)
            if (jsonObject.has("apps")) {
                val appsArray = jsonObject.getJSONArray("apps")
                val pm = context.packageManager
                val list = mutableListOf<TrackedRepo>()

                for (i in 0 until appsArray.length()) {
                    val appObj = appsArray.getJSONObject(i)
                    val id = appObj.optString("id").takeIf { it.isNotBlank() }
                    val url = appObj.optString("url").takeIf { it.isNotBlank() } ?: continue
                    val name = appObj.optString("name").takeIf { it.isNotBlank() } ?: "Unknown"
                    val author = appObj.optString("author").takeIf { it.isNotBlank() }
                    val latestVersion = appObj.optString("latestVersion").takeIf { it.isNotBlank() } ?: ""

                    var owner = author
                    var repoName: String? = null

                    val ghRegex = Regex("""(?:https?://)?(?:www\.)?github\.com/([^/]+)/([^/\\s?#]+)""")
                    val ghMatch = ghRegex.find(url)
                    if (ghMatch != null) {
                        owner = ghMatch.groupValues[1]
                        repoName = ghMatch.groupValues[2].removeSuffix(".git")
                    } else {
                        val apkUrlsStr = appObj.optString("apkUrls")
                        val otherUrlsStr = appObj.optString("otherAssetUrls")
                        val apiGhRegex = Regex("""(?:https?://)?(?:api\.)?github\.com/repos/([^/]+)/([^/\\s?#]+)""")
                        val apiMatch = apiGhRegex.find(apkUrlsStr) ?: apiGhRegex.find(otherUrlsStr)
                        if (apiMatch != null) {
                            owner = apiMatch.groupValues[1]
                            repoName = apiMatch.groupValues[2].removeSuffix(".git")
                        }
                    }

                    if (owner.isNullOrBlank() || repoName.isNullOrBlank()) {
                        continue
                    }

                    val fullName = "$owner/$repoName"

                    val additionalSettings = appObj.optString("additionalSettings")
                    val allowPreReleases = additionalSettings.contains("\"includePrereleases\":true")

                    var mappedPackageName: String? = null
                    var mappedAppName: String? = null

                    if (!id.isNullOrBlank()) {
                        try {
                            val appInfo = pm.getApplicationInfo(id, 0)
                            mappedPackageName = id
                            mappedAppName = pm.getApplicationLabel(appInfo).toString()
                        } catch (e: Exception) {
                            // App not installed
                        }
                    }

                    // Pick selected APK name if available
                    var selectedApkName = "Auto"
                    try {
                        val apkUrlsStr = appObj.optString("apkUrls")
                        if (apkUrlsStr.isNotBlank()) {
                            val parsedApkUrls = JSONArray(apkUrlsStr)
                            val preferredIndex = appObj.optInt("preferredApkIndex", 0)
                            if (parsedApkUrls.length() > preferredIndex) {
                                val pair = parsedApkUrls.getJSONArray(preferredIndex)
                                selectedApkName = pair.getString(0)
                            } else if (parsedApkUrls.length() > 0) {
                                selectedApkName = parsedApkUrls.getJSONArray(0).getString(0)
                            }
                        }
                    } catch (e: Exception) {
                    }

                    val tracked = TrackedRepo(
                        owner = owner,
                        name = repoName,
                        fullName = fullName,
                        description = null,
                        stars = 0,
                        avatarUrl = "https://github.com/$owner.png",
                        latestTagName = latestVersion,
                        latestReleaseName = latestVersion.ifBlank { null },
                        latestReleaseBody = appObj.optString("changeLog").takeIf { it.isNotBlank() },
                        latestReleaseUrl = url,
                        downloadUrl = null,
                        publishedAt = "",
                        selectedApkName = selectedApkName,
                        mappedPackageName = mappedPackageName,
                        mappedAppName = mappedAppName ?: name,
                        allowPreReleases = allowPreReleases,
                        notificationsEnabled = true
                    )
                    list.add(tracked)
                }
                return list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return emptyList()
    }
}
