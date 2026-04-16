package com.sameerasw.essentials.ui.ime

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.sameerasw.essentials.R
import java.io.InputStreamReader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.annotation.Keep
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Keep
data class KaomojiObject(
    @SerializedName("category") val category: String,
    @SerializedName("value") val value: String
)

@androidx.annotation.Keep
data class KaomojiCategory(
    val name: String,
    val kaomojis: List<KaomojiObject>
)

@Keep
data class KaomojiDataResponse(
    @SerializedName("kaomoji") val kaomoji: List<KaomojiObject>
)

object KaomojiData {
    var categories by mutableStateOf<List<KaomojiCategory>>(listOf())
    private var isLoaded = false

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun load(context: Context, scope: CoroutineScope) {
        if (isLoaded || _isLoading.value) return
        _isLoading.value = true

        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("kaomoji.json")
                val reader = InputStreamReader(inputStream)
                val response = Gson().fromJson(reader, KaomojiDataResponse::class.java)
                
                val grouped = response.kaomoji.groupBy { it.category }
                val loadedCategories = grouped.map { (categoryName, list) ->
                    KaomojiCategory(
                        name = categoryName,
                        kaomojis = list
                    )
                }

                // Sort categories
                val finalCategories = loadedCategories.sortedBy { it.name }

                withContext(Dispatchers.Main) {
                    categories = finalCategories
                    isLoaded = true
                    _isLoading.value = false
                }
                
                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                Log.e("KaomojiData", "Error loading kaomojis", e)
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
}
