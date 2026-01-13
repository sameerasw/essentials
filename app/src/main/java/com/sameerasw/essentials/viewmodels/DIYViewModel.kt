package com.sameerasw.essentials.viewmodels

import DIYRepository
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
}
