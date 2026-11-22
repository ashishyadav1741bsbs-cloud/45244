package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ProviderConfig
import com.example.data.repository.StudioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProvidersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudioRepository(application.applicationContext)

    val providerConfig: StateFlow<ProviderConfig> = repository.providerConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProviderConfig())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    fun saveConfig(config: ProviderConfig) {
        viewModelScope.launch {
            repository.saveProviderConfig(config)
            _statusMessage.value = "Provider settings saved."
        }
    }

    fun testConnection(endpointUrl: String, apiKey: String) {
        viewModelScope.launch {
            _isTesting.value = true
            _statusMessage.value = null
            val res = repository.testExternalConnection(endpointUrl, apiKey)
            _isTesting.value = false
            _statusMessage.value = res.fold(
                onSuccess = { "Endpoint reachable: $it" },
                onFailure = { "Connection failed: ${it.localizedMessage}" }
            )
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
