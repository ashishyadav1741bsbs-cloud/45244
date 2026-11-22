package com.example

import com.example.data.ai.PlaceholderAiRepository
import com.example.data.model.CreationMode
import com.example.data.model.StoryLanguage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioPlanUnitTest {

    private val aiRepository = PlaceholderAiRepository()

    @Test
    fun `30 minute script to video sums to exact 1800 seconds`() = runTest {
        val plan = aiRepository.analyzeStoryAndBuildPlan(
            projectTitle = "Long Documentary",
            prompt = "Aarav walks into the ancient Himalayan valley. The mist rises over pine trees. Meera joins him at the monastery gate. They discover an old brass telescope.",
            mode = CreationMode.SCRIPT_TO_VIDEO,
            language = StoryLanguage.ENGLISH,
            targetDurationSeconds = 1800,
            imageUris = emptyList()
        )

        assertTrue(plan.scenes.size >= 18)
        assertEquals(1800, plan.scenes.sumOf { it.durationSeconds })
        assertTrue(plan.characters.any { it.name == "Aarav" })
        assertTrue(plan.isPlaceholderMode)
    }

    @Test
    fun `hindi script splits cleanly on danda and preserves narration`() = runTest {
        val plan = aiRepository.analyzeStoryAndBuildPlan(
            projectTitle = "",
            prompt = "आरव सुबह बनारस पहुंचा। गंगा किनारे सूरज उग रहा था। मीरा ने चाय दी।",
            mode = CreationMode.TEXT_TO_VIDEO,
            language = StoryLanguage.HINDI,
            targetDurationSeconds = 30,
            imageUris = emptyList()
        )

        assertTrue(plan.scenes.size >= 3)
        assertEquals(30, plan.scenes.sumOf { it.durationSeconds })
        assertTrue(plan.scenes.first().narrationText.contains("आरव"))
    }

    @Test
    fun `image to video maps all picked images to scenes`() = runTest {
        val images = listOf("content://media/1", "content://media/2", "content://media/3")
        val plan = aiRepository.analyzeStoryAndBuildPlan(
            projectTitle = "Photo Reel",
            prompt = "Cinematic travel memories",
            mode = CreationMode.IMAGE_TO_VIDEO,
            language = StoryLanguage.ENGLISH,
            targetDurationSeconds = 24,
            imageUris = images
        )

        assertEquals(3, plan.scenes.size)
        assertEquals(images, plan.scenes.map { it.sourceImageUri })
        assertEquals(24, plan.scenes.sumOf { it.durationSeconds })
    }
}
