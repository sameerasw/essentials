package com.sameerasw.essentials.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.data.repository.GitHubRepository
import com.sameerasw.essentials.domain.model.github.GitHubRelease
import com.sameerasw.essentials.domain.model.github.GitHubRepo
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

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _errorMessage.value = null
    }

    fun searchRepo() {
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

        viewModelScope.launch {
            try {
                val repoInfo = gitHubRepository.getRepoInfo(owner, repo)
                if (repoInfo == null) {
                    _errorMessage.value = "Repository not found"
                } else {
                    val release = gitHubRepository.getLatestRelease(owner, repo)
                    if (release == null || !release.assets.any { it.name.endsWith(".apk") }) {
                        _errorMessage.value = "No APK found in the latest release"
                        // Still show repo info? User said "search should validate it... contains at least 1 apk file"
                        // So if no APK, it's an error.
                    } else {
                        _searchResult.value = repoInfo
                        _latestRelease.value = release
                        _readmeContent.value = gitHubRepository.getReadme(owner, repo)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "An error occurred during search"
            } finally {
                _isSearching.value = false
            }
        }
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
