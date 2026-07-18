package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Topic
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics ORDER BY questionNum ASC")
    fun getAllTopics(): Flow<List<Topic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Update
    suspend fun updateTopic(topic: Topic)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)

    @Query("DELETE FROM topics")
    suspend fun deleteAllTopics()
}
