package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CreationMode
import com.example.data.model.ProjectWithDetails
import com.example.data.model.ProviderConfig
import com.example.data.model.StoryLanguage
import com.example.data.model.TransitionStyle
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoResolution
import com.example.data.repository.StudioRepository
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudioDraftState(
    val mode: CreationMode = CreationMode.SCRIPT_TO_VIDEO,
    val title: String = "",
    val prompt: String = "",
    val selectedImageUris: List<String> = emptyList(),
    val targetDurationSeconds: Int = 60,
    val resolution: VideoResolution = VideoResolution.HD_720P,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.HORIZONTAL_16_9,
    val transitionStyle: TransitionStyle = TransitionStyle.CROSSFADE,
    val storyLanguage: StoryLanguage = StoryLanguage.HINDI,
    val enableNarration: Boolean = true,
    val enableSubtitles: Boolean = true,
    val isCreating: Boolean = false,
    val errorMessage: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    val repository = StudioRepository(application.applicationContext)

    val appLanguage: StateFlow<AppLanguage> = repository.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    val providerConfig: StateFlow<ProviderConfig> = repository.providerConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProviderConfig())

    val projects: StateFlow<List<ProjectWithDetails>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _draft = MutableStateFlow(StudioDraftState())
    val draft: StateFlow<StudioDraftState> = _draft.asStateFlow()

    fun toggleAppLanguage() {
        viewModelScope.launch {
            val next = if (appLanguage.value == AppLanguage.ENGLISH) AppLanguage.HINDI else AppLanguage.ENGLISH
            repository.setAppLanguage(next)
        }
    }

    fun setMode(mode: CreationMode) = _draft.update { it.copy(mode = mode, errorMessage = null) }
    fun setTitle(title: String) = _draft.update { it.copy(title = title) }
    fun setPrompt(prompt: String) = _draft.update { it.copy(prompt = prompt, errorMessage = null) }
    fun addImages(uris: List<String>) = _draft.update { state ->
        state.copy(selectedImageUris = (state.selectedImageUris + uris).distinct(), errorMessage = null)
    }
    fun removeImage(uri: String) = _draft.update { state ->
        state.copy(selectedImageUris = state.selectedImageUris - uri)
    }
    fun setDurationSeconds(seconds: Int) = _draft.update {
        it.copy(targetDurationSeconds = seconds.coerceIn(10, 1800))
    }
    fun setResolution(resolution: VideoResolution) = _draft.update { it.copy(resolution = resolution) }
    fun setAspectRatio(aspectRatio: VideoAspectRatio) = _draft.update { it.copy(aspectRatio = aspectRatio) }
    fun setTransitionStyle(style: TransitionStyle) = _draft.update { it.copy(transitionStyle = style) }
    fun setStoryLanguage(language: StoryLanguage) = _draft.update { it.copy(storyLanguage = language) }
    fun setEnableNarration(enabled: Boolean) = _draft.update { it.copy(enableNarration = enabled) }
    fun setEnableSubtitles(enabled: Boolean) = _draft.update { it.copy(enableSubtitles = enabled) }

    fun applyStarterTemplate(title: String, prompt: String, language: StoryLanguage) {
        _draft.update {
            it.copy(
                title = title,
                prompt = prompt,
                storyLanguage = language,
                errorMessage = null
            )
        }
    }

    fun createAndOpenProject(onCreated: (String) -> Unit, errorEmptyPrompt: String) {
        val current = _draft.value
        if (current.prompt.isBlank() && current.selectedImageUris.isEmpty()) {
            _draft.update { it.copy(errorMessage = errorEmptyPrompt) }
            return
        }
        viewModelScope.launch {
            _draft.update { it.copy(isCreating = true, errorMessage = null) }
            try {
                val id = repository.createProjectFromInput(
                    title = current.title,
                    prompt = current.prompt,
                    mode = current.mode,
                    language = current.storyLanguage,
                    targetDurationSeconds = current.targetDurationSeconds,
                    resolution = current.resolution,
                    aspectRatio = current.aspectRatio,
                    transitionStyle = current.transitionStyle,
                    enableNarration = current.enableNarration,
                    enableSubtitles = current.enableSubtitles,
                    imageUris = current.selectedImageUris
                )
                _draft.update { it.copy(isCreating = false, title = "", prompt = "", selectedImageUris = emptyList()) }
                onCreated(id)
            } catch (e: Exception) {
                _draft.update { it.copy(isCreating = false, errorMessage = e.localizedMessage ?: "Failed to create project.") }
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }
}
