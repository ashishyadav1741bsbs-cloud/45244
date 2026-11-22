package com.example.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class CreationMode(val labelEn: String, val labelHi: String) {
    TEXT_TO_VIDEO("Text to Video", "टेक्स्ट से वीडियो"),
    IMAGE_TO_VIDEO("Image to Video", "फ़ोटो से वीडियो"),
    SCRIPT_TO_VIDEO("Script to Video", "स्क्रिप्ट से वीडियो")
}

enum class StoryLanguage(val labelEn: String, val labelHi: String, val localeTag: String) {
    HINDI("Hindi (हिंदी)", "हिंदी (Hindi)", "hi-IN"),
    ENGLISH("English", "अंग्रेज़ी (English)", "en-US"),
    HINGLISH("Hinglish (Mix)", "हिंग्लिश (Mix)", "hi-IN")
}

enum class VideoResolution(
    val label: String,
    val horizontalWidth: Int,
    val horizontalHeight: Int,
    val bitrateBps: Int
) {
    HD_720P("720p HD", 1280, 720, 2_500_000),
    FULL_HD_1080P("1080p Full HD", 1920, 1080, 4_500_000);

    fun dimensionsFor(aspectRatio: VideoAspectRatio): Pair<Int, Int> = when (aspectRatio) {
        VideoAspectRatio.HORIZONTAL_16_9 -> horizontalWidth to horizontalHeight
        VideoAspectRatio.VERTICAL_9_16 -> horizontalHeight to horizontalWidth
    }
}

enum class VideoAspectRatio(val labelEn: String, val labelHi: String, val ratioValue: Float) {
    HORIZONTAL_16_9("16:9 Horizontal", "16:9 लैंडस्केप", 16f / 9f),
    VERTICAL_9_16("9:16 Vertical", "9:16 वर्टिकल (Reels)", 9f / 16f)
}

enum class TransitionStyle(val labelEn: String, val labelHi: String) {
    CROSSFADE("Crossfade", "क्रॉसफ़ेड"),
    SLIDE_LEFT("Slide Pan", "स्लाइड पैन"),
    ZOOM_DISSOLVE("Cinema Zoom", "सिनेमा ज़ूम"),
    NONE("Hard Cut", "सीधा कट")
}

enum class ProjectStatus {
    DRAFT,
    ANALYZED,
    GENERATING_CLIPS,
    COMBINING,
    READY,
    ERROR
}

enum class SceneStatus {
    PENDING,
    GENERATING,
    COMPLETED,
    FAILED
}

@Entity(tableName = "video_projects")
data class VideoProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mode: CreationMode,
    val storyPrompt: String,
    val language: StoryLanguage,
    val targetDurationSeconds: Int,
    val resolution: VideoResolution,
    val aspectRatio: VideoAspectRatio,
    val transitionStyle: TransitionStyle,
    val enableNarration: Boolean,
    val enableSubtitles: Boolean,
    val backgroundMusicUri: String? = null,
    val backgroundMusicName: String? = null,
    val providerId: String = ProviderIds.ON_DEVICE,
    val finalVideoPath: String? = null,
    val exportedMediaUri: String? = null,
    val status: ProjectStatus = ProjectStatus.ANALYZED,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "character_profiles",
    foreignKeys = [
        ForeignKey(
            entity = VideoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class CharacterProfileEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val appearancePrompt: String,
    val voiceTone: String,
    val referenceImageUri: String? = null,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "video_scenes",
    foreignKeys = [
        ForeignKey(
            entity = VideoProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class VideoSceneEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val sceneIndex: Int,
    val title: String,
    val visualPrompt: String,
    val narrationText: String,
    val subtitleText: String,
    val durationSeconds: Int,
    val transitionStyle: TransitionStyle = TransitionStyle.CROSSFADE,
    val sourceImageUri: String? = null,
    val characterTags: String = "",
    val clipFilePath: String? = null,
    val remoteJobId: String? = null,
    val status: SceneStatus = SceneStatus.PENDING,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

data class ProjectWithDetails(
    @Embedded val project: VideoProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val characters: List<CharacterProfileEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val scenes: List<VideoSceneEntity>
)

object ProviderIds {
    const val ON_DEVICE = "on_device_mp4"
    const val EXTERNAL_REST = "external_rest_api"
}

data class ProviderConfig(
    val activeProviderId: String = ProviderIds.ON_DEVICE,
    val externalEndpointUrl: String = "",
    val externalApiKey: String = "",
    val externalModelName: String = "veo-3"
)
