package com.example.imageviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class ImageSequence(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    val name: String,
    val coverPath: String,
    val imagePaths: String, // JSON数组字符串
    val lastPlayedIndex: Int = 0, // 上次播放位置
    val timestamp: Long = System.currentTimeMillis()
)