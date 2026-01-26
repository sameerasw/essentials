package com.sameerasw.essentials.viewmodels

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.data.repository.GitHubRepository
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.github.GitHubRelease
import com.sameerasw.essentials.domain.model.github.GitHubRepo
import com.sameerasw.essentials.utils.AppUtil
import kotlinx.coroutines.launch
import com.sameerasw.essentials.R

import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.TrackedRepo

class AppUpdatesViewModel : ViewModel() {
    private val gitHubRepository = GitHubRepository()

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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
    }

    fun onAppSelected(app: NotificationApp?) {
        _selectedApp.value = app
    }
    
    fun loadTrackedRepos(context: Context) {
        _isLoading.value = true
        viewModelScope.launch {
            _trackedRepos.value = SettingsRepository(context).getTrackedRepos()
            _isLoading.value = false
        }
    }

    fun searchRepo(context: Context) {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return

        val parts = parseRepoQuery(query)
        if (parts == null) {
            _errorMessage.value = context.getString(R.string.error_invalid_repo_format)
            return
        }

        val (owner, repo) = parts
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
                    _errorMessage.value = context.getString(com.sameerasw.essentials.R.string.error_rate_limited)
                } else {
                    _errorMessage.value = context.getString(R.string.error_generic_search)
                }
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    fun trackRepo(context: Context, selectedApk: String) {
        val repo = _searchResult.value ?: return
        val release = _latestRelease.value ?: return
        val app = _selectedApp.value
        
        val trackedRepo = TrackedRepo(
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
            downloadUrl = release.assets.find { it.name == selectedApk }?.downloadUrl ?: release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl,
            publishedAt = release.publishedAt,
            selectedApkName = selectedApk,
            mappedPackageName = app?.packageName,
            mappedAppName = app?.appName,
            allowPreReleases = _allowPreReleases.value,
            notificationsEnabled = _notificationsEnabled.value
        )
        
        SettingsRepository(context).addOrUpdateTrackedRepo(trackedRepo)
        loadTrackedRepos(context)
        clearSearch()
    }
    
    fun untrackRepo(context: Context, fullName: String) {
        SettingsRepository(context).removeTrackedRepo(fullName)
        loadTrackedRepos(context)
    }
    
    fun prepareEdit(context: Context, repo: TrackedRepo) {
        _searchQuery.value = repo.fullName
        _isSearching.value = true
        _errorMessage.value = null
        _searchResult.value = null
        _latestRelease.value = null
        _readmeContent.value = null
        _selectedApp.value = null
        _allowPreReleases.value = repo.allowPreReleases
        _notificationsEnabled.value = repo.notificationsEnabled
        
        viewModelScope.launch {
            try {
                val token = SettingsRepository(context).getGitHubToken()
                val repoInfo = gitHubRepository.getRepoInfo(repo.owner, repo.name, token)
                val release = gitHubRepository.getLatestRelease(repo.owner, repo.name, token)
                _searchResult.value = repoInfo
                _latestRelease.value = release
                _readmeContent.value = gitHubRepository.getReadme(repo.owner, repo.name, token)
                
                // Set mapped app
                if (repo.mappedPackageName != null) {
                    val installedApps = AppUtil.getInstalledApps(context)
                    _selectedApp.value = installedApps.find { it.packageName == repo.mappedPackageName }
                }
            } catch (e: Exception) {
                if (e.message == "RATE_LIMIT") {
                    _errorMessage.value = context.getString(com.sameerasw.essentials.R.string.error_rate_limited)
                }
                // Fallback to offline data? User said it should open but didn't specify offline support.
                // For now just clear searching
            } finally {
                _isSearching.value = false
            }
        }
    }

    private suspend fun findMatchingApp(context: Context, repoName: String) {
        val installedApps = AppUtil.getInstalledApps(context)
        // Simple name matching logic
        val normalizedRepoName = repoName.lowercase().replace("-", "").replace("_", "").trim()
        
        val matchedApp = installedApps.find { app ->
            val normalizedAppName = app.appName.lowercase().replace(" ", "").replace("-", "").replace("_", "").trim()
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

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResult.value = null
        _latestRelease.value = null
        _errorMessage.value = null
        _readmeContent.value = null
        _allowPreReleases.value = false
        _notificationsEnabled.value = true
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setAllowPreReleases(allow: Boolean) {
        _allowPreReleases.value = allow
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    fun fetchReleaseNotesIfNeeded(context: Context, repo: TrackedRepo) {
        if (!repo.latestReleaseBody.isNullOrBlank()) return

        viewModelScope.launch {
            try {
                val token = SettingsRepository(context).getGitHubToken()

                val release = if (repo.allowPreReleases) {
                    val releases = gitHubRepository.getReleases(repo.owner, repo.name, token)
                    releases.firstOrNull()
                } else {
                    gitHubRepository.getLatestRelease(repo.owner, repo.name, token)
                }

                if (release != null) {
                    // Update the cached repo with new details
                    val updatedRepo = repo.copy(
                        latestTagName = release.tagName,
                        latestReleaseName = release.name,
                        latestReleaseBody = release.body,
                        latestReleaseUrl = release.htmlUrl,
                        downloadUrl = release.assets.find { it.name == repo.selectedApkName }?.downloadUrl
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
                        context.getString(com.sameerasw.essentials.R.string.error_rate_limited)
                }
                // Ignore others
            }
        }
    }

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
                    val release = if (repo.allowPreReleases) {
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
                                    installedVersion
                                ) > 0
                            }
                        }

                        val newRepo = repo.copy(
                            latestTagName = release.tagName,
                            latestReleaseName = release.name,
                            latestReleaseBody = release.body,
                            latestReleaseUrl = release.htmlUrl,
                            downloadUrl = release.assets.find { it.name == repo.selectedApkName }?.downloadUrl
                                ?: release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl,
                            publishedAt = release.publishedAt,
                            isUpdateAvailable = isUpdateAvailable,
                            lastETag = null
                        )

                        if (newRepo != repo) {
                            updatedRepos[i] = newRepo
                            changesMade = true
                        }
                    }
                } catch (e: Exception) {
                    if (e.message == "RATE_LIMIT") {
                        _errorMessage.value =
                            context.getString(com.sameerasw.essentials.R.string.error_rate_limited)
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

    private fun compareVersions(v1: String, v2: String): Int {
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
}