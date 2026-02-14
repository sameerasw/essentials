package com.sameerasw.essentials.ui.ime

import android.content.Context
import com.google.gson.Gson
import com.sameerasw.essentials.R
import java.io.InputStreamReader
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EmojiObject(
    val emoji: String,
    val name: String
)

data class EmojiCategory(
    val name: String,
    val iconRes: Int,
    val emojis: List<EmojiObject>
)

data class EmojiDataResponse(
    val emojis: Map<String, Map<String, List<EmojiObject>>>
)

object EmojiData {
    var categories by mutableStateOf<List<EmojiCategory>>(listOf())
    var allEmojis by mutableStateOf<List<EmojiObject>>(listOf())
    private var isLoaded = false

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun load(context: Context, scope: CoroutineScope) {
        if (isLoaded || _isLoading.value) return
        _isLoading.value = true

        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("emojis.json")
                val reader = InputStreamReader(inputStream)
                val response = Gson().fromJson(reader, EmojiDataResponse::class.java)
                
                val loadedCategories = mutableListOf<EmojiCategory>()
                val loadedAllEmojis = mutableListOf<EmojiObject>()

                response.emojis.forEach { (categoryName, subcategories) ->
                    val combinedEmojis = mutableListOf<EmojiObject>()
                    subcategories.values.forEach { emojiList ->
                        combinedEmojis.addAll(emojiList)
                        loadedAllEmojis.addAll(emojiList)
                    }
                    
                    if (combinedEmojis.isNotEmpty()) {
                        loadedCategories.add(
                            EmojiCategory(
                                name = categoryName,
                                iconRes = categoryIcons[categoryName] ?: R.drawable.rounded_action_key_24,
                                emojis = combinedEmojis
                            )
                        )
                    }
                }

                val sortedCategoryNames = listOf(
                    "Smileys & Emotion",
                    "People & Body",
                    "Animals & Nature",
                    "Food & Drink",
                    "Travel & Places",
                    "Activities",
                    "Objects",
                    "Symbols",
                    "Flags"
                )

                val finalCategories = loadedCategories.sortedBy { category ->
                    val index = sortedCategoryNames.indexOf(category.name)
                    if (index != -1) index else Int.MAX_VALUE
                }

                withContext(Dispatchers.Main) {
                    categories = finalCategories
                    allEmojis = loadedAllEmojis
                    isLoaded = true
                    _isLoading.value = false
                }
                
                reader.close()
                inputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    private val categoryIcons = mapOf(
        "Smileys & Emotion" to R.drawable.rounded_heart_smile_24,
        "People & Body" to R.drawable.round_android_24,
        "Animals & Nature" to R.drawable.outline_bubble_chart_24,
        "Food & Drink" to R.drawable.rounded_coffee_24,
        "Travel & Places" to R.drawable.rounded_map_24,
        "Activities" to R.drawable.outline_circle_notifications_24,
        "Objects" to R.drawable.rounded_action_key_24,
        "Symbols" to R.drawable.rounded_numbers_24,
        "Flags" to R.drawable.rounded_globe_24
    )
}
