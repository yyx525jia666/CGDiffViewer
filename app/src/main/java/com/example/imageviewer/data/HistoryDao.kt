package com.example.imageviewer.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSequences(limit: Int = 50): Flow<List<ImageSequence>>
    
    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getSequenceById(id: Long): ImageSequence?
    
    @Insert
    suspend fun insertSequence(sequence: ImageSequence): Long
    
    @Update
    suspend fun updateSequence(sequence: ImageSequence)
    
    @Delete
    suspend fun deleteSequence(sequence: ImageSequence)
    
    @Query("UPDATE history SET lastPlayedIndex = :index WHERE id = :id")
    suspend fun updateLastPlayedIndex(id: Long, index: Int)
    
    @Query("DELETE FROM history")
    suspend fun clearAll()
}