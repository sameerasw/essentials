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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
    }

    fun onAppSelected(app: NotificationApp?) {
        _selectedApp.value = app
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
                    val release = gitHubRepository.getLatestRelease(owner, repo)
                    if (release == null || !release.assets.any { it.name.endsWith(".apk") }) {
                        _errorMessage.value = "No APK found in the latest release"
                    } else {
                        _searchResult.value = repoInfo
                        _latestRelease.value = release
                        _readmeContent.value = gitHubRepository.getReadme(owner, repo)
                        
                        // Try to find matching installed app
                        findMatchingApp(context, repoInfo.name)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "An error occurred during search"
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
    }
}
