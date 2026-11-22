package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CharacterProfileEntity
import com.example.data.model.ProjectWithDetails
import com.example.data.model.SceneStatus
import com.example.data.model.TransitionStyle
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoResolution
import com.example.data.model.VideoSceneEntity
import com.example.data.repository.StudioRepository
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkspaceOperationState(
    val isBatchRendering: Boolean = false,
    val isCombining: Boolean = false,
    val isExporting: Boolean = false,
    val activeSceneId: String? = null,
    val progress: Float = 0f,
    val statusText: String = "",
    val errorMessage: String? = null,
    val snackbarMessage: String? = null
)

class ProjectWorkspaceViewModel(
    application: Application,
    private val projectId: String
) : AndroidViewModel(application) {

    private val repository = StudioRepository(application.applicationContext)

    val projectDetails: StateFlow<ProjectWithDetails?> = repository.observeProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _opState = MutableStateFlow(WorkspaceOperationState())
    val opState: StateFlow<WorkspaceOperationState> = _opState.asStateFlow()

    private var batchJob: Job? = null

    fun clearMessages() {
        _opState.update { it.copy(errorMessage = null, snackbarMessage = null) }
    }

    fun addOrUpdateCharacter(
        existingId: String?,
        name: String,
        appearance: String,
        voiceTone: String,
        referenceImageUri: String?
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val count = projectDetails.value?.characters?.size ?: 0
            repository.saveCharacter(
                CharacterProfileEntity(
                    id = existingId ?: UUID.randomUUID().toString(),
                    projectId = projectId,
                    name = name.trim(),
                    appearancePrompt = appearance.trim(),
                    voiceTone = voiceTone.trim(),
                    referenceImageUri = referenceImageUri,
                    orderIndex = count
                )
            )
        }
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            repository.deleteCharacter(projectId, characterId)
        }
    }

    fun addNewScene() {
        viewModelScope.launch {
            repository.addBlankScene(projectId)
        }
    }

    fun updateScene(updated: VideoSceneEntity) {
        viewModelScope.launch {
            repository.saveScene(updated)
        }
    }

    fun deleteScene(sceneId: String) {
        viewModelScope.launch {
            repository.deleteScene(projectId, sceneId)
        }
    }

    fun updateResolutionAndAspect(resolution: VideoResolution, aspectRatio: VideoAspectRatio) {
        val current = projectDetails.value?.project ?: return
        viewModelScope.launch {
            repository.updateProjectConfig(current.copy(resolution = resolution, aspectRatio = aspectRatio))
        }
    }

    fun updateAudioSubtitleToggles(narration: Boolean, subtitles: Boolean) {
        val current = projectDetails.value?.project ?: return
        viewModelScope.launch {
            repository.updateProjectConfig(current.copy(enableNarration = narration, enableSubtitles = subtitles))
        }
    }

    fun setBackgroundMusic(uri: Uri?, displayName: String?) {
        val current = projectDetails.value?.project ?: return
        viewModelScope.launch {
            repository.updateProjectConfig(
                current.copy(
                    backgroundMusicUri = uri?.toString(),
                    backgroundMusicName = displayName
                )
            )
        }
    }

    fun renderSingleScene(sceneId: String) {
        viewModelScope.launch {
            _opState.update {
                it.copy(
                    activeSceneId = sceneId,
                    progress = 0.05f,
                    statusText = "Starting clip render...",
                    errorMessage = null
                )
            }
            val result = repository.renderSceneClipWithRetry(projectId, sceneId) { frac, msg ->
                _opState.update { it.copy(progress = frac, statusText = msg) }
            }
            _opState.update { state ->
                result.fold(
                    onSuccess = {
                        state.copy(
                            activeSceneId = null,
                            progress = 1f,
                            statusText = "",
                            snackbarMessage = "Scene clip rendered."
                        )
                    },
                    onFailure = { err ->
                        state.copy(
                            activeSceneId = null,
                            progress = 0f,
                            statusText = "",
                            errorMessage = err.localizedMessage ?: "Scene rendering failed."
                        )
                    }
                )
            }
        }
    }

    fun renderAllScenesAndCombine(autoCombine: Boolean = true) {
        batchJob?.cancel()
        batchJob = viewModelScope.launch {
            val details = projectDetails.value ?: return@launch
            val scenes = details.scenes.sortedBy { it.sceneIndex }
            if (scenes.isEmpty()) return@launch

            _opState.update {
                it.copy(
                    isBatchRendering = true,
                    progress = 0.02f,
                    statusText = "Preparing ${scenes.size} scenes...",
                    errorMessage = null
                )
            }

            try {
                for ((idx, scene) in scenes.withIndex()) {
                    if (scene.status == SceneStatus.COMPLETED && !scene.clipFilePath.isNullOrBlank()) {
                        continue
                    }
                    _opState.update { it.copy(activeSceneId = scene.id) }
                    val res = repository.renderSceneClipWithRetry(projectId, scene.id) { sceneFrac, msg ->
                        val overall = (idx.toFloat() + sceneFrac) / scenes.size.toFloat()
                        _opState.update {
                            it.copy(
                                progress = overall.coerceIn(0f, 0.9f),
                                statusText = "Scene ${idx + 1}/${scenes.size}: $msg"
                            )
                        }
                    }
                    if (res.isFailure) {
                        val err = res.exceptionOrNull()?.localizedMessage ?: "Scene ${idx + 1} failed."
                        _opState.update {
                            it.copy(
                                isBatchRendering = false,
                                activeSceneId = null,
                                errorMessage = err
                            )
                        }
                        return@launch
                    }
                }

                if (autoCombine) {
                    _opState.update {
                        it.copy(
                            isBatchRendering = false,
                            isCombining = true,
                            activeSceneId = null,
                            progress = 0.92f,
                            statusText = "Combining all scenes into final MP4..."
                        )
                    }
                    val combineRes = repository.combineCompletedScenes(projectId) { frac ->
                        _opState.update { it.copy(progress = 0.9f + frac * 0.1f) }
                    }
                    _opState.update { state ->
                        combineRes.fold(
                            onSuccess = {
                                state.copy(
                                    isCombining = false,
                                    progress = 1f,
                                    statusText = "",
                                    snackbarMessage = "Final MP4 ready to preview & download!"
                                )
                            },
                            onFailure = { err ->
                                state.copy(
                                    isCombining = false,
                                    progress = 0f,
                                    statusText = "",
                                    errorMessage = err.localizedMessage ?: "Failed to combine scenes."
                                )
                            }
                        )
                    }
                } else {
                    _opState.update {
                        it.copy(isBatchRendering = false, activeSceneId = null, progress = 1f, statusText = "")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _opState.update {
                    it.copy(
                        isBatchRendering = false,
                        isCombining = false,
                        activeSceneId = null,
                        errorMessage = e.localizedMessage ?: "Batch rendering failed."
                    )
                }
            }
        }
    }

    fun combineScenesOnly() {
        viewModelScope.launch {
            _opState.update {
                it.copy(isCombining = true, progress = 0.2f, statusText = "Combining rendered scenes...", errorMessage = null)
            }
            val result = repository.combineCompletedScenes(projectId) { frac ->
                _opState.update { it.copy(progress = frac) }
            }
            _opState.update { state ->
                result.fold(
                    onSuccess = {
                        state.copy(
                            isCombining = false,
                            progress = 1f,
                            statusText = "",
                            snackbarMessage = "Combined MP4 video is ready!"
                        )
                    },
                    onFailure = { err ->
                        state.copy(
                            isCombining = false,
                            progress = 0f,
                            statusText = "",
                            errorMessage = err.localizedMessage ?: "Combine failed."
                        )
                    }
                )
            }
        }
    }

    fun downloadMp4ToGallery(onSuccessMessage: (String) -> String) {
        viewModelScope.launch {
            _opState.update { it.copy(isExporting = true, errorMessage = null) }
            val result = repository.exportFinalMp4ToGallery(projectId)
            _opState.update { state ->
                result.fold(
                    onSuccess = { exported ->
                        state.copy(
                            isExporting = false,
                            snackbarMessage = onSuccessMessage(exported.displayName)
                        )
                    },
                    onFailure = { err ->
                        state.copy(
                            isExporting = false,
                            errorMessage = err.localizedMessage ?: "MP4 export failed."
                        )
                    }
                )
            }
        }
    }

    fun getShareUri(path: String): Uri? = repository.getShareUri(path)

    fun cancelBatchRender() {
        batchJob?.cancel()
        _opState.update {
            it.copy(isBatchRendering = false, isCombining = false, activeSceneId = null, statusText = "")
        }
    }
}
