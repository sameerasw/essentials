package com.sameerasw.essentials.ime

import android.content.Context
import android.util.Log
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import com.darkrockstudios.symspellkt.api.SpellChecker
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.impl.SymSpell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "SuggestionEngine"

enum class SuggestionType {
    Prediction,
    Correction,
    Learned
}

data class Suggestion(val text: String, val type: SuggestionType)

class SuggestionEngine(private val context: Context) :
    SpellCheckerSession.SpellCheckerSessionListener {

    private val userDictFile = File(context.filesDir, "user_dict.txt")
    private val userWords = mutableMapOf<String, Long>()

    // SymSpell
    private var symSpell: SpellChecker? = null
    private var isSymSpellReady = false

    // Android
    private var session: SpellCheckerSession? = null

    // State
    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions

    // Current word being looked up to handle sync/async merging
    private var currentWord: String = ""
    private var currentSymSpellSuggestions: List<Suggestion> = emptyList()

    fun initialize(scope: CoroutineScope) {
        // Init Android Session (Main Thread)
        scope.launch(Dispatchers.Main) {
            try {
                val tsm =
                    context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager
                if (tsm != null) {
                    session = tsm.newSpellCheckerSession(null, null, this@SuggestionEngine, true)
                    Log.d(TAG, "Android SpellCheckerSession created")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error init Android Session", e)
            }
        }

        // Init SymSpell (Background)
        scope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "freq_dict.txt")
                // Copy from assets if needed
                if (!file.exists()) {
                    try {
                        context.assets.open("frequency_dictionary_en_82_765.txt").use { input ->
                            FileOutputStream(file).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Dictionary asset missing. SymSpell will be disabled.", e)
                        return@launch
                    }
                }

                if (file.exists()) {
                    val checker = SymSpell()
                    file.forEachLine { line ->
                        val parts = line.split(" ")
                        if (parts.size >= 2) {
                            try {
                                checker.createDictionaryEntry(
                                    parts[0],
                                    parts[1].toLongOrNull()?.toDouble() ?: 0.0
                                )
                            } catch (e: Exception) { /* ignore */
                            }
                        }
                    }
                    symSpell = checker
                    isSymSpellReady = true
                    Log.d(TAG, "SymSpell initialized")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error init SymSpell", e)
            }
        }
        
        // Init User Dictionary (Background)
        scope.launch(Dispatchers.IO) {
            loadUserDictionary()
        }
    }

    fun loadUserDictionary() {
        if (!userDictFile.exists()) return

        try {
            userDictFile.forEachLine { line ->
                val parts = line.split(" ")
                if (parts.size >= 2) {
                    val word = parts[0]
                    val freq = parts[1].toLongOrNull() ?: 1L
                    userWords[word] = freq
                }
            }
            // Inject into SymSpell if ready
            if (isSymSpellReady && symSpell != null) {
                userWords.forEach { (word, freq) ->
                     try {
                         symSpell?.createDictionaryEntry(word, freq.toDouble() + 10000.0)
                     } catch (e: Exception) { }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user dict", e)
        }
    }

    fun learnWord(word: String) {
        if (word.length < 2) return
        if (word.any { it.isDigit() }) return 
        
        val currentFreq = userWords.getOrDefault(word, 0L)
        val newFreq = currentFreq + 1
        userWords[word] = newFreq
        
        if (isSymSpellReady) {
            try {
                 symSpell?.createDictionaryEntry(word, newFreq.toDouble() + 10000.0)
            } catch (e: Exception) { }
        }

        CoroutineScope(Dispatchers.IO).launch {
            saveUserDictionary()
        }
    }
    
    private fun saveUserDictionary() {
        try {
            FileOutputStream(userDictFile).use { fos ->
                userWords.forEach { (word, freq) ->
                    fos.write("$word $freq\n".toByteArray())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user dict", e)
        }
    }

    suspend fun lookup(word: String) = withContext(Dispatchers.Main) {
        currentWord = word
        if (word.isBlank()) {
            _suggestions.value = emptyList()
            currentSymSpellSuggestions = emptyList()
            return@withContext
        }

        // 1. SymSpell (Fast, Predictive)
        // Run on default dispatcher but wait for result to show immediately
        val symResults = if (isSymSpellReady) {
            withContext(Dispatchers.Default) {
                try {
                    // Max edit distance 2.0 for fuzzy
                    symSpell?.lookup(word, Verbosity.Closest, 2.0)
                        ?.map { item ->
                            val term = item.term
                            val type = if (userWords.containsKey(term)) SuggestionType.Learned else SuggestionType.Prediction
                            Suggestion(term, type)
                        }
                        ?.distinctBy { it.text }
                        ?.take(6) ?: emptyList()
                } catch (e: Exception) {
                    emptyList<Suggestion>()
                }
            }
        } else {
            emptyList()
        }

        currentSymSpellSuggestions = symResults
        // Update immediately with SymSpell results (Native usually takes longer)
        _suggestions.value = symResults

        // 2. Android (Corrective, Slower)
        val s = session
        if (s != null) {
            try {
                // Request suggestions. The callback onGetSuggestions will merge results
                @Suppress("DEPRECATION")
                s.getSuggestions(TextInfo(word), 5)
            } catch (e: Exception) {
                Log.e(TAG, "Android lookup failed", e)
            }
        }
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
        // Runs on binder thread usually, switch to Main logic if needed, but StateFlow is thread safe.
        // We want to merge with currentSymSpellSuggestions

        if (results.isNullOrEmpty()) return

        val info = results[0]
        // info.cookie or sequence is implementation dependent, assuming it matches 'currentWord' roughly
        // Ideally we check validity but for simplicity we merge.

        val androidSuggestions = mutableListOf<String>()
        val count = info.suggestionsCount
        if (count > 0) {
            for (i in 0 until count) {
                androidSuggestions.add(info.getSuggestionAt(i))
            }
        }

        // Merge: SymSpell (Prediction) + Android (Correction)
        // Deduplicate
        val androidSuggestionsList = androidSuggestions.map { Suggestion(it, SuggestionType.Correction) }
        val merged = (currentSymSpellSuggestions + androidSuggestionsList)
            .distinctBy { it.text }
            .take(8)

        _suggestions.value = merged
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {}

    fun clearSuggestions() {
        _suggestions.value = emptyList()
        currentSymSpellSuggestions = emptyList()
    }

    fun getUserWords() = userWords.toMap() 
    
    fun removeUserWord(word: String) {
        if (userWords.containsKey(word)) {
            userWords.remove(word)
             CoroutineScope(Dispatchers.IO).launch {
                saveUserDictionary()
             }
        }
    }
}
