package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Option
import com.example.data.model.Question
import com.example.data.model.Topic
import com.example.data.prefs.SettingsManager
import com.example.data.repository.McqRepository
import com.example.util.CSVHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class McqViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = McqRepository(db.questionDao(), db.topicDao())
    val settings = SettingsManager(application)

    val questions: StateFlow<List<Question>> = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topics: StateFlow<List<Topic>> = repository.allTopics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- UI Navigation / View State ---
    // Section can be: "add", "list", "settings", "topic", "style"
    val currentSection = MutableStateFlow("add")

    // --- Add/Edit Form State ---
    val editingQuestionId = MutableStateFlow<Long?>(null) // null if adding new, non-null if editing
    val formQuestionText = MutableStateFlow("")
    val formQImage = MutableStateFlow<String?>(null)
    
    val formOptions = MutableStateFlow<List<Option>>(listOf(Option(""), Option(""), Option(""), Option("")))
    val formCorrectIndices = MutableStateFlow<Set<Int>>(emptySet())
    val formIsNoAnswer = MutableStateFlow(false)
    val formExplanation = MutableStateFlow("")
    val formExpImage = MutableStateFlow<String?>(null)
    
    val formQImgSize = MutableStateFlow(160)
    val formOptImgSize = MutableStateFlow(160)
    val formExpImgSize = MutableStateFlow(160)

    // --- Topic Form State ---
    val topicQNumInput = MutableStateFlow("")
    val topicTitleInput = MutableStateFlow("")

    fun switchSection(section: String) {
        currentSection.value = section
    }

    // --- Image Handling ---
    fun saveQuestionImage(uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveImageLocally(getApplication(), uri, "q")
            formQImage.value = path
        }
    }

    fun saveOptionImage(index: Int, uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveImageLocally(getApplication(), uri, "opt")
            val current = formOptions.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(image = path)
                formOptions.value = current
            }
        }
    }

    fun saveExplanationImage(uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveImageLocally(getApplication(), uri, "exp")
            formExpImage.value = path
        }
    }

    fun saveWatermarkImage(uri: Uri) {
        viewModelScope.launch {
            val path = repository.saveImageLocally(getApplication(), uri, "wm")
            settings.watermarkPath = path
        }
    }

    fun clearWatermark() {
        settings.watermarkPath = null
    }

    // --- Form Actions ---
    fun addOptionField() {
        formOptions.value = formOptions.value + Option("")
    }

    fun removeOptionField(index: Int) {
        val current = formOptions.value.toMutableList()
        if (index in current.indices) {
            val removedOption = current.removeAt(index)
            formOptions.value = current
            
            // Adjust correct indices
            val currentCorrect = formCorrectIndices.value.toMutableSet()
            currentCorrect.remove(index)
            // Shift any index greater than deleted index
            val updated = currentCorrect.map { if (it > index) it - 1 else it }.toSet()
            formCorrectIndices.value = updated
        }
    }

    fun updateOptionText(index: Int, text: String) {
        val current = formOptions.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(text = text)
            formOptions.value = current
        }
    }

    fun removeOptionImageField(index: Int) {
        val current = formOptions.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(image = null)
            formOptions.value = current
        }
    }

    fun toggleCorrectOption(index: Int) {
        val current = formCorrectIndices.value.toMutableSet()
        if (current.contains(index)) {
            current.remove(index)
        } else {
            current.add(index)
        }
        formCorrectIndices.value = current
        if (current.isNotEmpty()) {
            formIsNoAnswer.value = false
        }
    }

    fun toggleIsNoAnswer(checked: Boolean) {
        formIsNoAnswer.value = checked
        if (checked) {
            formCorrectIndices.value = emptySet()
        }
    }

    fun resetForm() {
        editingQuestionId.value = null
        formQuestionText.value = ""
        formQImage.value = null
        formOptions.value = listOf(Option(""), Option(""), Option(""), Option(""))
        formCorrectIndices.value = emptySet()
        formIsNoAnswer.value = false
        formExplanation.value = ""
        formExpImage.value = null
        formQImgSize.value = 160
        formOptImgSize.value = 160
        formExpImgSize.value = 160
    }

    fun setEditingQuestion(question: Question) {
        editingQuestionId.value = question.id
        formQuestionText.value = question.questionText
        formQImage.value = question.qImage
        formOptions.value = question.options
        formCorrectIndices.value = question.correctIndices.toSet()
        formIsNoAnswer.value = question.isNoAnswer
        formExplanation.value = question.explanation ?: ""
        formExpImage.value = question.expImage
        formQImgSize.value = question.qImgSize
        formOptImgSize.value = question.optImgSize
        formExpImgSize.value = question.expImgSize
        
        switchSection("add")
    }

    fun saveQuestion(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val qText = formQuestionText.value.trim()
            val qImg = formQImage.value
            
            if (qText.isEmpty() && qImg == null) {
                onError("দয়া করে প্রশ্ন অথবা প্রশ্নের ছবি দিন।")
                return@launch
            }

            // Filter out empty options unless they have images
            val filteredOptions = formOptions.value.filter { it.text.trim().isNotEmpty() || it.image != null }
            if (filteredOptions.size < 2) {
                onError("সঠিকভাবে অন্তত ২টি অপশন দিন।")
                return@launch
            }

            // Map old correct indices to filtered options
            val mappedCorrectIndices = mutableListOf<Int>()
            formOptions.value.forEachIndexed { index, opt ->
                if (formCorrectIndices.value.contains(index)) {
                    val filteredIdx = filteredOptions.indexOf(opt)
                    if (filteredIdx != -1) {
                        mappedCorrectIndices.add(filteredIdx)
                    }
                }
            }

            val isNoAnswer = formIsNoAnswer.value

            if (isNoAnswer && mappedCorrectIndices.isNotEmpty()) {
                onError("\"উত্তর নেই\" নির্বাচন করা থাকলে অপশনে কোনো সঠিক উত্তর সিলেক্ট করা যাবে না।")
                return@launch
            }

            if (!isNoAnswer && mappedCorrectIndices.isEmpty()) {
                onError("দয়া করে সঠিক উত্তর দিন অথবা \"উত্তর নেই\" নির্বাচন করুন।")
                return@launch
            }

            val question = Question(
                id = editingQuestionId.value ?: 0,
                questionText = qText,
                qImage = qImg,
                options = filteredOptions,
                correctIndices = mappedCorrectIndices,
                isNoAnswer = isNoAnswer,
                explanation = formExplanation.value.trim().takeIf { it.isNotEmpty() },
                expImage = formExpImage.value,
                qImgSize = formQImgSize.value,
                optImgSize = formOptImgSize.value,
                expImgSize = formExpImgSize.value
            )

            if (editingQuestionId.value == null) {
                repository.insertQuestion(question)
            } else {
                repository.updateQuestion(question)
            }

            resetForm()
            onSuccess()
        }
    }

    fun deleteQuestion(question: Question) {
        viewModelScope.launch {
            repository.deleteQuestionById(question.id)
        }
    }

    fun clearAllQuestions() {
        viewModelScope.launch {
            repository.deleteAllQuestions()
        }
    }

    // --- Topic Actions ---
    fun addTopic(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val qNum = topicQNumInput.value.trim().toIntOrNull()
            val title = topicTitleInput.value.trim()

            if (qNum == null || qNum <= 0) {
                onError("সঠিক প্রশ্ন নম্বর দিন।")
                return@launch
            }
            if (title.isEmpty()) {
                onError("টপিকের নাম দিন।")
                return@launch
            }

            val existing = topics.value.find { it.questionNum == qNum }
            val topic = Topic(
                id = existing?.id ?: 0,
                questionNum = qNum,
                title = title
            )

            if (existing == null) {
                repository.insertTopic(topic)
            } else {
                repository.updateTopic(topic)
            }

            topicQNumInput.value = ""
            topicTitleInput.value = ""
            onSuccess()
        }
    }

    fun deleteTopic(topic: Topic) {
        viewModelScope.launch {
            repository.deleteTopicById(topic.id)
        }
    }

    fun importFromCSV(uri: Uri, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val imported = CSVHelper.parseCSV(getApplication(), uri)
                if (imported.isEmpty()) {
                    onError("CSV ফাইলে কোনো প্রশ্ন পাওয়া যায়নি বা ফরম্যাট ভুল।")
                    return@launch
                }
                for (question in imported) {
                    repository.insertQuestion(question)
                }
                onSuccess(imported.size)
            } catch (e: Exception) {
                onError("আমদানি করতে সমস্যা হয়েছে: ${e.message}")
            }
        }
    }
}
