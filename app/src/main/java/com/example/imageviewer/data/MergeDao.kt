package com.example.imageviewer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MergeDao {
    @Query("SELECT * FROM merge_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<MergeProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: MergeProject): Long

    @Update
    suspend fun updateProject(project: MergeProject)

    @Delete
    suspend fun deleteProject(project: MergeProject)

    @Query("SELECT * FROM merge_projects WHERE id = :id")
    suspend fun getProjectById(id: Long): MergeProject?
}
