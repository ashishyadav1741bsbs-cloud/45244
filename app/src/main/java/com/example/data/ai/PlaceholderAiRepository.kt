package com.example.data.ai

import com.example.data.model.CreationMode
import com.example.data.model.StoryLanguage
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay

/**
 * Built-in deterministic script & scene analyzer used when Firebase AI Logic is not enabled.
 * Splits real user stories (Hindi/English) into timed scenes up to 30 minutes and injects
 * consistent character descriptors so users can edit and render real MP4 videos immediately.
 */
class PlaceholderAiRepository : AiRepository {
    override val isFirebaseAiEnabled: Boolean = false

    override suspend fun analyzeStoryAndBuildPlan(
        projectTitle: String,
        prompt: String,
        mode: CreationMode,
        language: StoryLanguage,
        targetDurationSeconds: Int,
        imageUris: List<String>
    ): AiStoryPlan {
        delay(350) // Brief natural analysis feedback

        val cleanPrompt = prompt.trim()
        val resolvedTitle = projectTitle.trim().ifEmpty {
            cleanPrompt.lineSequence().firstOrNull()?.take(42)?.trim()?.ifEmpty { "Studio Video Project" }
                ?: "Studio Video Project"
        }

        // Extract candidate characters from capitalized words or common Hindi/English story cues
        val characters = extractConsistentCharacters(cleanPrompt, language, imageUris)
        val characterNames = characters.joinToString(", ") { it.name }
        val primaryCharacterSummary = characters.firstOrNull()?.let {
            "${it.name} (${it.appearancePrompt})"
        } ?: ""

        // Split user's actual story/script into meaningful segments
        val rawSegments = when {
            mode == CreationMode.IMAGE_TO_VIDEO && imageUris.isNotEmpty() -> {
                val sentenceSegments = splitIntoSegments(cleanPrompt)
                imageUris.mapIndexed { idx, _ ->
                    sentenceSegments.getOrElse(idx) {
                        if (cleanPrompt.isNotBlank()) cleanPrompt
                        else if (language == StoryLanguage.HINDI) "फ़ोटो दृश्य ${idx + 1}: धीमी सिनेमैटिक मोशन और प्रकाश प्रभाव"
                        else "Photo Motion Scene ${idx + 1}: Smooth cinematic camera drift and atmospheric light"
                    }
                }
            }
            else -> {
                val parsed = splitIntoSegments(cleanPrompt)
                if (parsed.isEmpty()) listOf(cleanPrompt) else parsed
            }
        }

        // For long durations up to 30 minutes (1800s), expand segments cleanly into manageable scenes
        val desiredSceneCount = when {
            mode == CreationMode.IMAGE_TO_VIDEO && imageUris.isNotEmpty() -> max(rawSegments.size, imageUris.size)
            targetDurationSeconds <= 30 -> max(rawSegments.size, 3).coerceAtMost(6)
            targetDurationSeconds <= 180 -> max(rawSegments.size, 6).coerceAtMost(14)
            targetDurationSeconds <= 600 -> max(rawSegments.size, 10).coerceAtMost(24)
            else -> max(rawSegments.size, 18).coerceAtMost(36)
        }

        val expandedSegments = (0 until desiredSceneCount).map { index ->
            rawSegments[index % rawSegments.size]
        }

        val baseSceneDuration = max(4, targetDurationSeconds / expandedSegments.size)
        var remainingSeconds = targetDurationSeconds

        val scenes = expandedSegments.mapIndexed { index, segment ->
            val duration = if (index == expandedSegments.lastIndex) {
                max(4, remainingSeconds)
            } else {
                val alloc = min(baseSceneDuration, max(4, remainingSeconds - 4 * (expandedSegments.size - 1 - index)))
                remainingSeconds -= alloc
                alloc
            }

            val sceneImage = if (imageUris.isNotEmpty()) imageUris[index % imageUris.size] else null
            val sceneNum = index + 1
            val sceneTitle = when (language) {
                StoryLanguage.HINDI -> "दृश्य $sceneNum: ${segment.take(28)}${if (segment.length > 28) "..." else ""}"
                else -> "Scene $sceneNum: ${segment.take(28)}${if (segment.length > 28) "..." else ""}"
            }

            val cameraDirection = when (index % 4) {
                0 -> "Wide establishing shot, golden-hour cinematic depth"
                1 -> "Medium tracking shot, consistent subject framing"
                2 -> "Close-up expressive detail, dramatic studio rim light"
                else -> "Slow aerial/panning motion, atmospheric color grade"
            }

            val visualPrompt = buildString {
                append("[Sample Analyzer] ")
                append(segment)
                if (primaryCharacterSummary.isNotBlank()) {
                    append(" | Consistent character: ")
                    append(primaryCharacterSummary)
                }
                append(" | Camera: ")
                append(cameraDirection)
            }

            GeneratedScenePlan(
                title = sceneTitle,
                visualPrompt = visualPrompt,
                narrationText = segment,
                subtitleText = segment.take(120),
                durationSeconds = duration,
                sourceImageUri = sceneImage,
                characterTags = characterNames
            )
        }

        val notice = if (language == StoryLanguage.HINDI) {
            "ऑफ़लाइन सैंपल एनालाइज़र सक्रिय है (Gemini AI के लिए Firebase AI Logic आवश्यक है)।"
        } else {
            "Sample offline script structurer active (Gemini AI requires Firebase AI Logic)."
        }

        return AiStoryPlan(
            projectTitle = resolvedTitle,
            characters = characters,
            scenes = scenes,
            isPlaceholderMode = true,
            noticeMessage = notice
        )
    }

    override suspend fun enhanceScenePrompt(
        sceneTitle: String,
        rawPrompt: String,
        characterTraits: List<String>,
        language: StoryLanguage
    ): String {
        delay(200)
        val traitsBlock = if (characterTraits.isNotEmpty()) {
            " | Characters: ${characterTraits.joinToString("; ")}"
        } else ""
        return "[Sample Enhanced] $rawPrompt$traitsBlock | Cinematic lighting, 24fps film motion, crisp focus"
    }

    private fun splitIntoSegments(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return text
            .split(Regex("[\\n।.!?]+"))
            .map { it.trim() }
            .filter { it.length >= 3 }
    }

    private fun extractConsistentCharacters(
        prompt: String,
        language: StoryLanguage,
        imageUris: List<String>
    ): List<GeneratedCharacterPlan> {
        val detectedNames = Regex("\\b[A-Z][a-z]{2,14}\\b")
            .findAll(prompt)
            .map { it.value }
            .filterNot { it in setOf("The", "And", "But", "Scene", "Video", "Hindi", "English", "Story", "Once") }
            .distinct()
            .take(3)
            .toList()

        if (detectedNames.isNotEmpty()) {
            return detectedNames.mapIndexed { idx, name ->
                GeneratedCharacterPlan(
                    name = name,
                    appearancePrompt = "[Sample Profile] Consistent appearance, expressive eyes, warm cinematic wardrobe across all scenes",
                    voiceTone = if (language == StoryLanguage.HINDI) "स्पष्ट हिंदी स्टोरीटेलर आवाज़" else "Clear cinematic narrator",
                    referenceImageUri = imageUris.getOrNull(idx)
                )
            }
        }

        return listOf(
            GeneratedCharacterPlan(
                name = if (language == StoryLanguage.HINDI) "मुख्य किरदार (Protagonist)" else "Lead Character",
                appearancePrompt = if (language == StoryLanguage.HINDI) {
                    "[सैंपल प्रोफ़ाइल] सभी दृश्यों में एक जैसा चेहरा, परिधान और सिनेमैटिक रोशनी"
                } else {
                    "[Sample Profile] Consistent facial identity, wardrobe, and lighting continuity across every scene"
                },
                voiceTone = if (language == StoryLanguage.HINDI) "मधुर हिंदी नरेशन" else "Warm English/Hindi narration",
                referenceImageUri = imageUris.firstOrNull()
            )
        )
    }
}
