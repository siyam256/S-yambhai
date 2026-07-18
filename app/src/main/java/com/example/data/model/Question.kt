package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Option(
    val text: String,
    val image: String? = null // local file path or base64
)

@Entity(tableName = "questions")
@JsonClass(generateAdapter = true)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionText: String,
    val qImage: String? = null,
    val options: List<Option>,
    val correctIndices: List<Int>,
    val isNoAnswer: Boolean = false,
    val explanation: String? = null,
    val expImage: String? = null,
    val qImgSize: Int = 160,
    val optImgSize: Int = 160,
    val expImgSize: Int = 160,
    val timestamp: Long = System.currentTimeMillis()
)
