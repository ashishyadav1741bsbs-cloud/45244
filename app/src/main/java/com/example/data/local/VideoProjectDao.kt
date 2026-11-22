package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.CharacterProfileEntity
import com.example.data.model.ProjectWithDetails
import com.example.data.model.VideoProjectEntity
import com.example.data.model.VideoSceneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoProjectDao {

    @Transaction
    @Query("SELECT * FROM video_projects ORDER BY updatedAt DESC")
    fun observeAllProjectsWithDetails(): Flow<List<ProjectWithDetails>>

    @Transaction
    @Query("SELECT * FROM video_projects WHERE id = :projectId")
    fun observeProjectWithDetails(projectId: String): Flow<ProjectWithDetails?>

    @Transaction
    @Query("SELECT * FROM video_projects WHERE id = :projectId")
    suspend fun getProjectWithDetails(projectId: String): ProjectWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProjectEntity)

    @Update
    suspend fun updateProject(project: VideoProjectEntity)

    @Query("DELETE FROM video_projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacters(characters: List<CharacterProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterProfileEntity)

    @Query("DELETE FROM character_profiles WHERE id = :characterId")
    suspend fun deleteCharacterById(characterId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<VideoSceneEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: VideoSceneEntity)

    @Update
    suspend fun updateScene(scene: VideoSceneEntity)

    @Query("DELETE FROM video_scenes WHERE id = :sceneId")
    suspend fun deleteSceneById(sceneId: String)

    @Query("SELECT * FROM video_scenes WHERE projectId = :projectId ORDER BY sceneIndex ASC")
    suspend fun getScenesForProject(projectId: String): List<VideoSceneEntity>

    @Transaction
    suspend fun insertFullProject(
        project: VideoProjectEntity,
        characters: List<CharacterProfileEntity>,
        scenes: List<VideoSceneEntity>
    ) {
        insertProject(project)
        if (characters.isNotEmpty()) insertCharacters(characters)
        if (scenes.isNotEmpty()) insertScenes(scenes)
    }
}
