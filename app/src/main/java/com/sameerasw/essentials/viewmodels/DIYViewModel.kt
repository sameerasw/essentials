package com.sameerasw.essentials.viewmodels

import com.sameerasw.essentials.domain.diy.DIYRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sameerasw.essentials.domain.diy.Automation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DIYViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DIYRepository

    init {
        repository.init(application)
    }

    val automations: StateFlow<List<Automation>> = repository.automations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteAutomation(id: String) {
        repository.removeAutomation(id)
    }

    fun toggleAutomation(id: String) {
        repository.getAutomation(id)?.let { automation ->
            repository.updateAutomation(automation.copy(isEnabled = !automation.isEnabled))
        }
    }
}
