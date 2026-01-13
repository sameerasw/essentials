import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.diy.Automation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DIYRepository {
    private const val PREFS_NAME = "diy_automations_prefs"
    private const val KEY_AUTOMATIONS = "saved_automations"

    private val _automations = MutableStateFlow<List<Automation>>(emptyList())
    val automations = _automations.asStateFlow()
    
    private var prefs: SharedPreferences? = null
    private val gson = Gson()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadAutomations()
    }

    private fun loadAutomations() {
        val json = prefs?.getString(KEY_AUTOMATIONS, null)
        val loadedList: List<Automation> = if (json != null) {
            try {
                val type = object : TypeToken<List<Automation>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        _automations.value = loadedList
    }

    private fun saveToPrefs() {
        val json = gson.toJson(_automations.value)
        prefs?.edit()?.putString(KEY_AUTOMATIONS, json)?.apply()
    }

    fun addAutomation(automation: Automation) {
        val current = _automations.value.toMutableList()
        current.add(automation)
        _automations.value = current
        saveToPrefs()
    }

    fun updateAutomation(automation: Automation) {
        val current = _automations.value.toMutableList()
        val index = current.indexOfFirst { it.id == automation.id }
        if (index != -1) {
            current[index] = automation
            _automations.value = current
            saveToPrefs()
        }
    }

    fun removeAutomation(id: String) {
        val current = _automations.value.toMutableList()
        current.removeAll { it.id == id }
        _automations.value = current
        saveToPrefs()
    }

    fun getAutomation(id: String): Automation? {
        return _automations.value.find { it.id == id }
    }
}
