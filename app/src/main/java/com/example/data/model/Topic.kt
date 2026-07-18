package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "topics")
@JsonClass(generateAdapter = true)
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionNum: Int,
    val title: String
)
