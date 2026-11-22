package com.example.data.video

import com.example.data.model.CharacterProfileEntity
import com.example.data.model.ProviderConfig
import com.example.data.model.VideoProjectEntity
import com.example.data.model.VideoSceneEntity
import java.io.File

data class SceneGenerationRequest(
    val project: VideoProjectEntity,
    val scene: VideoSceneEntity,
    val characters: List<CharacterProfileEntity>,
    val outputFile: File,
    val providerConfig: ProviderConfig
)

interface VideoGenerationProvider {
    val id: String
    val displayNameEn: String
    val displayNameHi: String
    val limitsNoteEn: String
    val limitsNoteHi: String

    suspend fun generateSceneClip(
        request: SceneGenerationRequest,
        onProgress: (Float, String) -> Unit
    ): Result<File>
}
