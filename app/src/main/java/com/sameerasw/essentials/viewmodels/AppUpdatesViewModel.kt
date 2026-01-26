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
            _errorMessage.value = "Invalid format. Use owner/repo or GitHub URL"
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
                val repoInfo = gitHubRepository.getRepoInfo(owner, repo)
                if (repoInfo == null) {
                    _errorMessage.value = "Repository not found"
                } else {
                    var release = gitHubRepository.getLatestRelease(owner, repo)
                    var isPreRelease = false
                    
                    if (release == null) {
                        val releases = gitHubRepository.getReleases(owner, repo)
                        release = releases.firstOrNull()
                        if (release != null) {
                            isPreRelease = true
                        }
                    }

                    if (release == null || !release.assets.any { it.name.endsWith(".apk") }) {
                        _errorMessage.value = "No APK found in the latest release"
                    } else {
                        _searchResult.value = repoInfo
                        _latestRelease.value = release
                        _readmeContent.value = gitHubRepository.getReadme(owner, repo)
                        
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
                    _errorMessage.value = "An error occurred during search"
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
                val repoInfo = gitHubRepository.getRepoInfo(repo.owner, repo.name)
                val release = gitHubRepository.getLatestRelease(repo.owner, repo.name)
                _searchResult.value = repoInfo
                _latestRelease.value = release
                _readmeContent.value = gitHubRepository.getReadme(repo.owner, repo.name)
                
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
                
                val release = if (repo.allowPreReleases) {
                   val releases = gitHubRepository.getReleases(repo.owner, repo.name)
                   releases.firstOrNull()
                } else {
                   gitHubRepository.getLatestRelease(repo.owner, repo.name)
                }

                if (release != null) {
                   // Update the cached repo with new details
                   val updatedRepo = repo.copy(
                       latestTagName = release.tagName,
                       latestReleaseName = release.name,
                       latestReleaseBody = release.body,
                       latestReleaseUrl = release.htmlUrl,
                       downloadUrl = release.assets.find { it.name == repo.selectedApkName }?.downloadUrl ?: release.assets.firstOrNull { it.name.endsWith(".apk") }?.downloadUrl,
                       publishedAt = release.publishedAt,
                       // Keep existing flags
                   )
                   SettingsRepository(context).addOrUpdateTrackedRepo(updatedRepo)
                   loadTrackedRepos(context)
                }
            } catch (e: Exception) {
               if (e.message == "RATE_LIMIT") {
                   _errorMessage.value = context.getString(com.sameerasw.essentials.R.string.error_rate_limited)
               }
               // Ignore others
            }
        }
    }
}
