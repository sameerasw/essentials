/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: GitHub OAuth Authentication
 * File: GitHubAuthViewModel.kt
 * Description: ViewModel managing GitHub OAuth device flow, access token storage,
 * and user profile synchronization.
 */

package com.sameerasw.essentials.viewmodels

import com.sameerasw.essentials.R
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.data.repository.GitHubAuthRepository
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GitHubAuthViewModel : ViewModel() {
    private val authRepository = GitHubAuthRepository()
    private val gitHubRepository =
        com.sameerasw.essentials.data.repository
            .GitHubRepository()

    private val _authState = mutableStateOf<AuthState>(AuthState.Idle)
    val authState: State<AuthState> = _authState

    private val _currentUser =
        mutableStateOf<com.sameerasw.essentials.domain.model.github.GitHubUser?>(null)
    val currentUser: State<com.sameerasw.essentials.domain.model.github.GitHubUser?> = _currentUser

    private var pollingJob: Job? = null

    fun startAuthFlow() {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val response = authRepository.requestDeviceCode()
            if (response != null) {
                _authState.value =
                    AuthState.CodeReceived(
                        userCode = response.userCode,
                        verificationUri = response.verificationUri,
                    )
                startPolling(response.deviceCode, response.interval)
            } else {
                _authState.value = AuthState.Error(R.string.github_auth_device_code_failed)
            }
        }
    }

    private fun startPolling(
        deviceCode: String,
        intervalSeconds: Int,
    ) {
        pollingJob?.cancel()
        pollingJob =
            viewModelScope.launch {
                var currentInterval = intervalSeconds * 1000L
                while (isActive) {
                    delay(currentInterval)
                    val tokenResponse = authRepository.pollForToken(deviceCode, intervalSeconds)

                    if (tokenResponse != null) {
                        when {
                            tokenResponse.accessToken != null -> {
                                _authState.value = AuthState.Authenticated(tokenResponse.accessToken)
                                pollingJob?.cancel()
                                return@launch
                            }

                            tokenResponse.error == "authorization_pending" -> {
                                // Continue polling
                            }

                            tokenResponse.error == "slow_down" -> {
                                currentInterval += 5000L
                            }

                            tokenResponse.error == "expired_token" -> {
                                _authState.value = AuthState.Error(R.string.github_auth_code_expired)
                                pollingJob?.cancel()
                                return@launch
                            }

                            else -> {
                                // "access_denied" or other errors
                                _authState.value =
                                    AuthState.Error(R.string.github_auth_failed, tokenResponse.error)
                                pollingJob?.cancel()
                                return@launch
                            }
                        }
                    }
                }
            }
    }

    fun saveToken(
        context: Context,
        token: String,
    ) {
        SettingsRepository(context).saveGitHubToken(token)
        loadUser(token, context)
    }

    fun loadUser(
        token: String,
        context: Context,
    ) {
        viewModelScope.launch {
            val user = gitHubRepository.getUserProfile(token)
            if (user != null) {
                _currentUser.value = user
                SettingsRepository(context).saveGitHubUser(user)
            }
        }
    }

    fun loadCachedUser(context: Context) {
        viewModelScope.launch {
            _currentUser.value = SettingsRepository(context).getGitHubUser()
        }
    }

    fun signOut(context: Context) {
        SettingsRepository(context).saveGitHubToken(null)
        SettingsRepository(context).saveGitHubUser(null)
        _currentUser.value = null
        _authState.value = AuthState.Idle
        pollingJob?.cancel()
    }

    fun cancelAuthFlow() {
        pollingJob?.cancel()
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()

    object Loading : AuthState()

    data class CodeReceived(
        val userCode: String,
        val verificationUri: String,
    ) : AuthState()

    data class Authenticated(
        val token: String,
    ) : AuthState()

    data class Error(
        val messageRes: Int,
        val formatArg: String? = null,
    ) : AuthState()
}
