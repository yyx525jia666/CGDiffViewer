package com.example.imageviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merge_projects")
data class MergeProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetFolderUri: String,
    val sourceFoldersJson: String = "[]", // List of {uri, name} as JSON
    val timestamp: Long = System.currentTimeMillis()
)
