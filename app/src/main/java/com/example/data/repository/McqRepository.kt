package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.QuestionDao
import com.example.data.local.TopicDao
import com.example.data.model.Question
import com.example.data.model.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class McqRepository(
    private val questionDao: QuestionDao,
    private val topicDao: TopicDao
) {
    val allQuestions: Flow<List<Question>> = questionDao.getAllQuestions()
    val allTopics: Flow<List<Topic>> = topicDao.getAllTopics()

    suspend fun insertQuestion(question: Question): Long = withContext(Dispatchers.IO) {
        questionDao.insertQuestion(question)
    }

    suspend fun updateQuestion(question: Question) = withContext(Dispatchers.IO) {
        questionDao.updateQuestion(question)
    }

    suspend fun deleteQuestionById(id: Long) = withContext(Dispatchers.IO) {
        questionDao.deleteQuestionById(id)
    }

    suspend fun deleteAllQuestions() = withContext(Dispatchers.IO) {
        questionDao.deleteAllQuestions()
    }

    suspend fun insertTopic(topic: Topic): Long = withContext(Dispatchers.IO) {
        topicDao.insertTopic(topic)
    }

    suspend fun updateTopic(topic: Topic) = withContext(Dispatchers.IO) {
        topicDao.updateTopic(topic)
    }

    suspend fun deleteTopicById(id: Long) = withContext(Dispatchers.IO) {
        topicDao.deleteTopicById(id)
    }

    suspend fun deleteAllTopics() = withContext(Dispatchers.IO) {
        topicDao.deleteAllTopics()
    }

    /**
     * Copies an image from a system URI to the app's local internal storage
     */
    suspend fun saveImageLocally(context: Context, uri: Uri, prefix: String): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val ext = when (context.contentResolver.getType(uri)) {
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val fileName = "${prefix}_${System.currentTimeMillis()}.$ext"
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
