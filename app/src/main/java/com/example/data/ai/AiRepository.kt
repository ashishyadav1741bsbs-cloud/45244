package com.example.data.ai

import com.example.data.model.CreationMode
import com.example.data.model.StoryLanguage

data class GeneratedCharacterPlan(
    val name: String,
    val appearancePrompt: String,
    val voiceTone: String,
    val referenceImageUri: String? = null
)

data class GeneratedScenePlan(
    val title: String,
    val visualPrompt: String,
    val narrationText: String,
    val subtitleText: String,
    val durationSeconds: Int,
    val sourceImageUri: String? = null,
    val characterTags: String = ""
)

data class AiStoryPlan(
    val projectTitle: String,
    val characters: List<GeneratedCharacterPlan>,
    val scenes: List<GeneratedScenePlan>,
    val isPlaceholderMode: Boolean,
    val noticeMessage: String
)

interface AiRepository {
    val isFirebaseAiEnabled: Boolean

    suspend fun analyzeStoryAndBuildPlan(
        projectTitle: String,
        prompt: String,
        mode: CreationMode,
        language: StoryLanguage,
        targetDurationSeconds: Int,
        imageUris: List<String>
    ): AiStoryPlan

    suspend fun enhanceScenePrompt(
        sceneTitle: String,
        rawPrompt: String,
        characterTraits: List<String>,
        language: StoryLanguage
    ): String
}
