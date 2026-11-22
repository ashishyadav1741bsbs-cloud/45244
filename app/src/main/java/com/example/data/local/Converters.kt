package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.CreationMode
import com.example.data.model.ProjectStatus
import com.example.data.model.SceneStatus
import com.example.data.model.StoryLanguage
import com.example.data.model.TransitionStyle
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoResolution

class Converters {
    @TypeConverter fun fromCreationMode(value: CreationMode): String = value.name
    @TypeConverter fun toCreationMode(value: String): CreationMode =
        runCatching { CreationMode.valueOf(value) }.getOrDefault(CreationMode.SCRIPT_TO_VIDEO)

    @TypeConverter fun fromStoryLanguage(value: StoryLanguage): String = value.name
    @TypeConverter fun toStoryLanguage(value: String): StoryLanguage =
        runCatching { StoryLanguage.valueOf(value) }.getOrDefault(StoryLanguage.HINDI)

    @TypeConverter fun fromVideoResolution(value: VideoResolution): String = value.name
    @TypeConverter fun toVideoResolution(value: String): VideoResolution =
        runCatching { VideoResolution.valueOf(value) }.getOrDefault(VideoResolution.HD_720P)

    @TypeConverter fun fromVideoAspectRatio(value: VideoAspectRatio): String = value.name
    @TypeConverter fun toVideoAspectRatio(value: String): VideoAspectRatio =
        runCatching { VideoAspectRatio.valueOf(value) }.getOrDefault(VideoAspectRatio.HORIZONTAL_16_9)

    @TypeConverter fun fromTransitionStyle(value: TransitionStyle): String = value.name
    @TypeConverter fun toTransitionStyle(value: String): TransitionStyle =
        runCatching { TransitionStyle.valueOf(value) }.getOrDefault(TransitionStyle.CROSSFADE)

    @TypeConverter fun fromProjectStatus(value: ProjectStatus): String = value.name
    @TypeConverter fun toProjectStatus(value: String): ProjectStatus =
        runCatching { ProjectStatus.valueOf(value) }.getOrDefault(ProjectStatus.ANALYZED)

    @TypeConverter fun fromSceneStatus(value: SceneStatus): String = value.name
    @TypeConverter fun toSceneStatus(value: String): SceneStatus =
        runCatching { SceneStatus.valueOf(value) }.getOrDefault(SceneStatus.PENDING)
}
