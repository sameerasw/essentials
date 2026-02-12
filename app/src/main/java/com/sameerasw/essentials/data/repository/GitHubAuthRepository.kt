package com.sameerasw.essentials.data.repository

import com.google.gson.Gson
import com.sameerasw.essentials.domain.model.github.DeviceCodeResponse
import com.sameerasw.essentials.domain.model.github.TokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class GitHubAuthRepository {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val clientId = "Ov23lisMyhKfjlM5M5ec" // Provided by user

    suspend fun requestDeviceCode(): DeviceCodeResponse? = withContext(Dispatchers.IO) {
        try {
            val requestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("scope", "public_repo")
                .build()

            val request = Request.Builder()
                .url("https://github.com/login/device/code")
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            gson.fromJson(responseBody, DeviceCodeResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun pollForToken(deviceCode: String, interval: Int): TokenResponse? = withContext(Dispatchers.IO) {
        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("device_code", deviceCode)
            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            .build()

        val request = Request.Builder()
            .url("https://github.com/login/oauth/access_token")
            .header("Accept", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val responseBody = response.body?.string() ?: return@withContext null
            gson.fromJson(responseBody, TokenResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
