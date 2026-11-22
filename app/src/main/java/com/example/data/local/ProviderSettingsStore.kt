package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ProviderConfig
import com.example.data.model.ProviderIds
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "studio_settings")

class ProviderSettingsStore(private val context: Context) {

    private object Keys {
        val UI_LANGUAGE = stringPreferencesKey("ui_language")
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider_id")
        val EXTERNAL_ENDPOINT = stringPreferencesKey("external_endpoint_url")
        val EXTERNAL_API_KEY = stringPreferencesKey("external_api_key")
        val EXTERNAL_MODEL = stringPreferencesKey("external_model_name")
    }

    val appLanguage: Flow<AppLanguage> = context.studioPrefsDataStore.data.map { prefs ->
        when (prefs[Keys.UI_LANGUAGE]) {
            AppLanguage.HINDI.code -> AppLanguage.HINDI
            else -> AppLanguage.ENGLISH
        }
    }

    val providerConfig: Flow<ProviderConfig> = context.studioPrefsDataStore.data.map { prefs ->
        ProviderConfig(
            activeProviderId = prefs[Keys.ACTIVE_PROVIDER] ?: ProviderIds.ON_DEVICE,
            externalEndpointUrl = prefs[Keys.EXTERNAL_ENDPOINT] ?: "",
            externalApiKey = prefs[Keys.EXTERNAL_API_KEY] ?: "",
            externalModelName = prefs[Keys.EXTERNAL_MODEL] ?: "veo-3"
        )
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[Keys.UI_LANGUAGE] = language.code
        }
    }

    suspend fun saveProviderConfig(config: ProviderConfig) {
        context.studioPrefsDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROVIDER] = config.activeProviderId
            prefs[Keys.EXTERNAL_ENDPOINT] = config.externalEndpointUrl.trim()
            prefs[Keys.EXTERNAL_API_KEY] = config.externalApiKey.trim()
            prefs[Keys.EXTERNAL_MODEL] = config.externalModelName.trim().ifEmpty { "veo-3" }
        }
    }
}
