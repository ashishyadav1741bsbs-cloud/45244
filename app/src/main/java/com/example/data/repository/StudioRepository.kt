package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.ai.AiRepository
import com.example.data.ai.PlaceholderAiRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ProviderSettingsStore
import com.example.data.model.CharacterProfileEntity
import com.example.data.model.CreationMode
import com.example.data.model.ProjectStatus
import com.example.data.model.ProjectWithDetails
import com.example.data.model.ProviderConfig
import com.example.data.model.ProviderIds
import com.example.data.model.SceneStatus
import com.example.data.model.StoryLanguage
import com.example.data.model.TransitionStyle
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoProjectEntity
import com.example.data.model.VideoResolution
import com.example.data.model.VideoSceneEntity
import com.example.data.video.ExportedVideoResult
import com.example.data.video.ExternalRestVideoProvider
import com.example.data.video.Mp4GalleryExporter
import com.example.data.video.Mp4VideoCombiner
import com.example.data.video.OnDeviceMp4SceneProvider
import com.example.data.video.SceneGenerationRequest
import com.example.data.video.VideoGenerationProvider
import com.example.localization.AppLanguage
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StudioRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.videoProjectDao()
    val settingsStore = ProviderSettingsStore(context)
    val aiRepository: AiRepository = PlaceholderAiRepository()

    val onDeviceProvider = OnDeviceMp4SceneProvider(context)
    val externalRestProvider = ExternalRestVideoProvider()
    private val combiner = Mp4VideoCombiner(context)
    private val exporter = Mp4GalleryExporter(context)

    val allProjects: Flow<List<ProjectWithDetails>> = dao.observeAllProjectsWithDetails()
    val appLanguage: Flow<AppLanguage> = settingsStore.appLanguage
    val providerConfig: Flow<ProviderConfig> = settingsStore.providerConfig

    fun observeProject(projectId: String): Flow<ProjectWithDetails?> =
        dao.observeProjectWithDetails(projectId)

    fun availableProviders(): List<VideoGenerationProvider> =
        listOf(onDeviceProvider, externalRestProvider)

    fun resolveProvider(providerId: String): VideoGenerationProvider =
        if (providerId == ProviderIds.EXTERNAL_REST) externalRestProvider else onDeviceProvider

    suspend fun setAppLanguage(language: AppLanguage) = settingsStore.setAppLanguage(language)

    suspend fun saveProviderConfig(config: ProviderConfig) = settingsStore.saveProviderConfig(config)

    suspend fun testExternalConnection(url: String, apiKey: String): Result<String> =
        externalRestProvider.testEndpointReachable(url, apiKey)

    suspend fun createProjectFromInput(
        title: String,
        prompt: String,
        mode: CreationMode,
        language: StoryLanguage,
        targetDurationSeconds: Int,
        resolution: VideoResolution,
        aspectRatio: VideoAspectRatio,
        transitionStyle: TransitionStyle,
        enableNarration: Boolean,
        enableSubtitles: Boolean,
        imageUris: List<String>
    ): String {
        val config = settingsStore.providerConfig.first()
        val plan = aiRepository.analyzeStoryAndBuildPlan(
            projectTitle = title,
            prompt = prompt,
            mode = mode,
            language = language,
            targetDurationSeconds = targetDurationSeconds,
            imageUris = imageUris
        )

        val projectId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val project = VideoProjectEntity(
            id = projectId,
            title = plan.projectTitle,
            mode = mode,
            storyPrompt = prompt.trim(),
            language = language,
            targetDurationSeconds = targetDurationSeconds,
            resolution = resolution,
            aspectRatio = aspectRatio,
            transitionStyle = transitionStyle,
            enableNarration = enableNarration,
            enableSubtitles = enableSubtitles,
            providerId = config.activeProviderId,
            status = ProjectStatus.ANALYZED,
            createdAt = now,
            updatedAt = now
        )

        val characters = plan.characters.mapIndexed { idx, charPlan ->
            CharacterProfileEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                name = charPlan.name,
                appearancePrompt = charPlan.appearancePrompt,
                voiceTone = charPlan.voiceTone,
                referenceImageUri = charPlan.referenceImageUri,
                orderIndex = idx
            )
        }

        val scenes = plan.scenes.mapIndexed { idx, scenePlan ->
            VideoSceneEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                sceneIndex = idx,
                title = scenePlan.title,
                visualPrompt = scenePlan.visualPrompt,
                narrationText = scenePlan.narrationText,
                subtitleText = scenePlan.subtitleText,
                durationSeconds = scenePlan.durationSeconds,
                transitionStyle = transitionStyle,
                sourceImageUri = scenePlan.sourceImageUri,
                characterTags = scenePlan.characterTags,
                status = SceneStatus.PENDING
            )
        }

        dao.insertFullProject(project, characters, scenes)
        return projectId
    }

    suspend fun deleteProject(projectId: String) {
        val projectDir = File(context.filesDir, "videos/$projectId")
        if (projectDir.exists()) projectDir.deleteRecursively()
        dao.deleteProjectById(projectId)
    }

    suspend fun saveCharacter(character: CharacterProfileEntity) {
        dao.insertCharacter(character)
        touchProject(character.projectId)
    }

    suspend fun deleteCharacter(projectId: String, characterId: String) {
        dao.deleteCharacterById(characterId)
        touchProject(projectId)
    }

    suspend fun saveScene(scene: VideoSceneEntity) {
        dao.insertScene(scene)
        touchProject(scene.projectId)
    }

    suspend fun addBlankScene(projectId: String) {
        val existing = dao.getScenesForProject(projectId)
        val nextIndex = (existing.maxOfOrNull { it.sceneIndex } ?: -1) + 1
        dao.insertScene(
            VideoSceneEntity(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                sceneIndex = nextIndex,
                title = "Scene ${nextIndex + 1}",
                visualPrompt = "Cinematic wide shot, smooth motion, atmospheric studio lighting",
                narrationText = "",
                subtitleText = "",
                durationSeconds = 6,
                transitionStyle = TransitionStyle.CROSSFADE,
                status = SceneStatus.PENDING
            )
        )
        touchProject(projectId)
    }

    suspend fun deleteScene(projectId: String, sceneId: String) {
        dao.deleteSceneById(sceneId)
        val remaining = dao.getScenesForProject(projectId).sortedBy { it.sceneIndex }
        remaining.forEachIndexed { idx, scene ->
            if (scene.sceneIndex != idx) {
                dao.updateScene(scene.copy(sceneIndex = idx))
            }
        }
        touchProject(projectId)
    }

    suspend fun updateProjectConfig(project: VideoProjectEntity) {
        dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun renderSceneClipWithRetry(
        projectId: String,
        sceneId: String,
        onProgress: (Float, String) -> Unit
    ): Result<File> {
        val details = dao.getProjectWithDetails(projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))
        val scene = details.scenes.firstOrNull { it.id == sceneId }
            ?: return Result.failure(IllegalStateException("Scene not found"))

        val config = settingsStore.providerConfig.first()
        val provider = resolveProvider(config.activeProviderId)
        val outFile = File(context.filesDir, "videos/$projectId/scene_${scene.id}.mp4")

        dao.updateProject(details.project.copy(status = ProjectStatus.GENERATING_CLIPS, updatedAt = System.currentTimeMillis()))
        dao.updateScene(scene.copy(status = SceneStatus.GENERATING, errorMessage = null))

        var lastError: Throwable? = null
        val maxRetries = 2
        for (attempt in 0..maxRetries) {
            val res = provider.generateSceneClip(
                request = SceneGenerationRequest(
                    project = details.project,
                    scene = scene,
                    characters = details.characters,
                    outputFile = outFile,
                    providerConfig = config
                ),
                onProgress = onProgress
            )
            if (res.isSuccess) {
                dao.updateScene(
                    scene.copy(
                        clipFilePath = outFile.absolutePath,
                        status = SceneStatus.COMPLETED,
                        errorMessage = null,
                        retryCount = attempt
                    )
                )
                touchProject(projectId)
                return res
            } else {
                lastError = res.exceptionOrNull()
                if (attempt < maxRetries) {
                    onProgress(0.1f, "Retrying scene render (${attempt + 2}/${maxRetries + 1})...")
                    delay(800L * (attempt + 1))
                }
            }
        }

        val errMsg = lastError?.localizedMessage ?: "Scene clip rendering failed."
        dao.updateScene(
            scene.copy(
                status = SceneStatus.FAILED,
                errorMessage = errMsg,
                retryCount = scene.retryCount + 1
            )
        )
        dao.updateProject(details.project.copy(status = ProjectStatus.ERROR, errorMessage = errMsg, updatedAt = System.currentTimeMillis()))
        return Result.failure(lastError ?: IllegalStateException(errMsg))
    }

    suspend fun combineCompletedScenes(
        projectId: String,
        onProgress: (Float) -> Unit
    ): Result<File> {
        val details = dao.getProjectWithDetails(projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))

        val orderedClips = details.scenes
            .sortedBy { it.sceneIndex }
            .mapNotNull { it.clipFilePath?.let(::File) }
            .filter { it.exists() && it.length() > 0L }

        if (orderedClips.isEmpty()) {
            return Result.failure(IllegalStateException("Please render at least one scene clip first."))
        }

        dao.updateProject(details.project.copy(status = ProjectStatus.COMBINING, errorMessage = null, updatedAt = System.currentTimeMillis()))
        val finalFile = File(context.filesDir, "videos/$projectId/final_output.mp4")

        val result = combiner.combineScenes(
            clipFiles = orderedClips,
            outputFile = finalFile,
            backgroundMusicUri = details.project.backgroundMusicUri,
            onProgress = onProgress
        )

        return result.onSuccess { file ->
            dao.updateProject(
                details.project.copy(
                    finalVideoPath = file.absolutePath,
                    status = ProjectStatus.READY,
                    errorMessage = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }.onFailure { err ->
            dao.updateProject(
                details.project.copy(
                    status = ProjectStatus.ERROR,
                    errorMessage = err.localizedMessage ?: "Failed to combine MP4 clips.",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun exportFinalMp4ToGallery(projectId: String): Result<ExportedVideoResult> {
        val details = dao.getProjectWithDetails(projectId)
            ?: return Result.failure(IllegalStateException("Project not found"))
        val path = details.project.finalVideoPath
            ?: return Result.failure(IllegalStateException("No final MP4 video generated yet."))
        val file = File(path)
        val exportResult = exporter.exportToGallery(file, details.project.title)
        exportResult.onSuccess { exported ->
            dao.updateProject(
                details.project.copy(
                    exportedMediaUri = exported.contentUri.toString(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return exportResult
    }

    fun getShareUri(filePath: String): Uri? {
        val file = File(filePath)
        if (!file.exists() || file.length() <= 0L) return null
        return exporter.getShareableUri(file)
    }

    private suspend fun touchProject(projectId: String) {
        val current = dao.getProjectWithDetails(projectId)?.project ?: return
        dao.updateProject(current.copy(updatedAt = System.currentTimeMillis()))
    }
}
