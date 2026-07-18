package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.Option
import com.example.data.model.Question
import com.example.data.model.Topic
import com.example.ui.viewmodel.McqViewModel
import com.example.util.CSVHelper
import com.example.util.HtmlGenerator
import com.example.util.PdfPrintHelper
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McqAppScreen(
    viewModel: McqViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MCQ Builder",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = {
                            val csvContent = CSVHelper.exportToCSV(questions)
                            if (csvContent.isNotEmpty()) {
                                saveCsvToFile(context, csvContent)
                            } else {
                                Toast.makeText(context, "এক্সপোর্ট করার জন্য কোনো প্রশ্ন নেই!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.navigationBarsPadding()
            ) {
                val items = listOf(
                    NavigationItem("add", "নতুন প্রশ্ন", Icons.Default.AddCircle),
                    NavigationItem("topic", "টপিক", Icons.Default.Book),
                    NavigationItem("list", "প্রিভিউ", Icons.Default.Visibility),
                    NavigationItem("style", "স্টাইল", Icons.Default.Palette),
                    NavigationItem("settings", "সেটিংস", Icons.Default.Settings)
                )

                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentSection == item.id,
                        onClick = { viewModel.switchSection(item.id) },
                        label = { Text(item.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        modifier = Modifier.testTag("nav_tab_${item.id}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentSection) {
                "add" -> FormSection(viewModel)
                "topic" -> TopicSection(viewModel, topics)
                "list" -> PreviewSection(viewModel, questions, topics)
                "style" -> StyleSection(viewModel)
                "settings" -> SettingsSection(viewModel, questions)
            }
        }
    }
}

data class NavigationItem(val id: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun FormSection(viewModel: McqViewModel) {
    val context = LocalContext.current
    val editingId by viewModel.editingQuestionId.collectAsStateWithLifecycle()
    val qText by viewModel.formQuestionText.collectAsStateWithLifecycle()
    val qImage by viewModel.formQImage.collectAsStateWithLifecycle()
    val options by viewModel.formOptions.collectAsStateWithLifecycle()
    val correctIndices by viewModel.formCorrectIndices.collectAsStateWithLifecycle()
    val isNoAnswer by viewModel.formIsNoAnswer.collectAsStateWithLifecycle()
    val explanation by viewModel.formExplanation.collectAsStateWithLifecycle()
    val expImage by viewModel.formExpImage.collectAsStateWithLifecycle()

    val qImgSize by viewModel.formQImgSize.collectAsStateWithLifecycle()
    val optImgSize by viewModel.formOptImgSize.collectAsStateWithLifecycle()
    val expImgSize by viewModel.formExpImgSize.collectAsStateWithLifecycle()

    val qImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.saveQuestionImage(it) }
    }
    val expImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.saveExplanationImage(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Title
        Text(
            text = if (editingId == null) "নতুন MCQ তৈরি করুন" else "MCQ এডিট করুন",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Question Field Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("প্রশ্ন (Question)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = qText,
                    onValueChange = { viewModel.formQuestionText.value = it },
                    placeholder = { Text("এখানে প্রশ্ন লিখুন...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("question_input"),
                    minLines = 2
                )

                // Question Image Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { qImageLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Add Image")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (qImage == null) "প্রশ্নের ছবি যোগ করুন" else "ছবি পরিবর্তন করুন")
                    }

                    if (qImage != null) {
                        IconButton(onClick = { viewModel.formQImage.value = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Image", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                qImage?.let { imagePath ->
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Question Image Preview",
                        modifier = Modifier
                            .height(qImgSize.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )
                    
                    // Question Image Size Slider
                    Column {
                        Text("প্রশ্নের ছবির সাইজ: ${qImgSize}dp", fontSize = 12.sp)
                        Slider(
                            value = qImgSize.toFloat(),
                            onValueChange = { viewModel.formQImgSize.value = it.toInt() },
                            valueRange = 50f..300f
                        )
                    }
                }
            }
        }

        // Options List Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "অপশনসমূহ (Options)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "একাধিক সঠিক উত্তর থাকলে টিক মার্ক দিন।",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                options.forEachIndexed { idx, opt ->
                    val isChecked = correctIndices.contains(idx)
                    val optImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                        uri?.let { viewModel.saveOptionImage(idx, it) }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, if (isChecked) MaterialTheme.colorScheme.primary else Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { viewModel.toggleCorrectOption(idx) },
                                modifier = Modifier.testTag("option_checkbox_$idx")
                            )

                            OutlinedTextField(
                                value = opt.text,
                                onValueChange = { viewModel.updateOptionText(idx, it) },
                                placeholder = { Text("অপশন ${getMarkerLabel(idx)}...") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("option_input_$idx")
                            )

                            IconButton(
                                onClick = { viewModel.removeOptionField(idx) },
                                modifier = Modifier.testTag("delete_option_$idx")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Option", tint = Color.Gray)
                            }
                        }

                        // Option Image Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(onClick = { optImageLauncher.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Option Image")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (opt.image == null) "অপশনে ছবি দিন" else "অপশনের ছবি পরিবর্তন")
                            }

                            if (opt.image != null) {
                                TextButton(onClick = { viewModel.removeOptionImageField(idx) }) {
                                    Text("ছবি মুছুন", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        opt.image?.let { optImgPath ->
                            AsyncImage(
                                model = File(optImgPath),
                                contentDescription = "Option Image Preview",
                                modifier = Modifier
                                    .height(optImgSize.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Color.LightGray, RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }

                // Add Option Button
                Button(
                    onClick = { viewModel.addOptionField() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Option")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন অপশন যোগ করুন")
                }

                // Option Image Slider if any options contain images
                val hasOptImg = options.any { it.image != null }
                if (hasOptImg) {
                    Column {
                        Text("অপশনের ছবির সাইজ: ${optImgSize}dp", fontSize = 12.sp)
                        Slider(
                            value = optImgSize.toFloat(),
                            onValueChange = { viewModel.formOptImgSize.value = it.toInt() },
                            valueRange = 50f..300f
                        )
                    }
                }

                HorizontalDivider()

                // No Answer Selection Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Checkbox(
                        checked = isNoAnswer,
                        onCheckedChange = { viewModel.toggleIsNoAnswer(it) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "উত্তর নেই (প্রশ্নে কোনো সঠিক উত্তর না থাকলে এটি সিলেক্ট করুন)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Explanation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ব্যাখ্যা (Explanation - ঐচ্ছিক)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = explanation,
                    onValueChange = { viewModel.formExplanation.value = it },
                    placeholder = { Text("উত্তরের বিস্তারিত ব্যাখ্যা...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Explanation Image Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { expImageLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "Add Explanation Image")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (expImage == null) "ব্যাখ্যার ছবি যোগ করুন" else "ছবি পরিবর্তন করুন")
                    }

                    if (expImage != null) {
                        IconButton(onClick = { viewModel.formExpImage.value = null }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Image", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                expImage?.let { imagePath ->
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = "Explanation Image Preview",
                        modifier = Modifier
                            .height(expImgSize.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                    )
                    
                    // Size Slider
                    Column {
                        Text("ব্যাখ্যার ছবির সাইজ: ${expImgSize}dp", fontSize = 12.sp)
                        Slider(
                            value = expImgSize.toFloat(),
                            onValueChange = { viewModel.formExpImgSize.value = it.toInt() },
                            valueRange = 50f..300f
                        )
                    }
                }
            }
        }

        // Save & Cancel Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (editingId != null) {
                OutlinedButton(
                    onClick = { viewModel.resetForm() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("বাতিল করুন")
                }
            }

            Button(
                onClick = {
                    viewModel.saveQuestion(
                        onSuccess = {
                            Toast.makeText(context, "সফলভাবে সংরক্ষণ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            viewModel.switchSection("list")
                        },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("submit_button")
            ) {
                Text(if (editingId == null) "তালিকায় যুক্ত করুন" else "আপডেট করুন")
            }
        }
    }
}

@Composable
fun TopicSection(viewModel: McqViewModel, topics: List<Topic>) {
    val context = LocalContext.current
    val qNum by viewModel.topicQNumInput.collectAsStateWithLifecycle()
    val title by viewModel.topicTitleInput.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("টপিক তৈরি করুন", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "যেই নম্বর দেবেন, প্রিভিউতে সেই নম্বরের প্রশ্নের ঠিক ওপরে টপিকটি শো করবে।",
            fontSize = 13.sp,
            color = Color.DarkGray
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = qNum,
                    onValueChange = { viewModel.topicQNumInput.value = it },
                    label = { Text("প্রশ্ন নম্বর (যেমন: ৫)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { viewModel.topicTitleInput.value = it },
                    label = { Text("টপিকের নাম (যেমন: কোষ রসায়ন)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.addTopic(
                            onSuccess = {
                                Toast.makeText(context, "টপিক সফলভাবে যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("যুক্ত করুন")
                }
            }
        }

        Text("সেভ করা টপিকসমূহ:", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (topics.isEmpty()) {
            Text(
                "কোনো টপিক যোগ করা হয়নি।",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(topics) { topic ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "প্রশ্ন নং ${toBengaliNum(topic.questionNum)} :",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = topic.title,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.deleteTopic(topic) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewSection(viewModel: McqViewModel, questions: List<Question>, topics: List<Topic>) {
    val context = LocalContext.current
    var startRange by remember { mutableStateOf("") }
    var endRange by remember { mutableStateOf("") }

    var appliedStartRange by remember { mutableStateOf<Int?>(null) }
    var appliedEndRange by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top controllers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "প্রশ্নপত্র প্রিভিউ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    val html = HtmlGenerator.generateMcqHtml(
                        questions = questions,
                        topics = topics,
                        settings = viewModel.settings,
                        startRange = appliedStartRange,
                        endRange = appliedEndRange
                    )
                    PdfPrintHelper.printHtml(context, html)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Print, contentDescription = "Print PDF")
                Spacer(modifier = Modifier.width(4.dp))
                Text("প্রিন্ট PDF / Save")
            }
        }

        // Print Range filter
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("প্রিন্ট রেঞ্জ ফিল্টার:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startRange,
                        onValueChange = { startRange = it },
                        placeholder = { Text("শুরু (যেমন: ১)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Text("থেকে")
                    OutlinedTextField(
                        value = endRange,
                        onValueChange = { endRange = it },
                        placeholder = { Text("শেষ (যেমন: ৫০)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(onClick = {
                        appliedStartRange = startRange.toIntOrNull()
                        appliedEndRange = endRange.toIntOrNull()
                    }) {
                        Text("ফিল্টার")
                    }
                    if (appliedStartRange != null || appliedEndRange != null) {
                        IconButton(onClick = {
                            startRange = ""
                            endRange = ""
                            appliedStartRange = null
                            appliedEndRange = null
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Range", tint = Color.Gray)
                        }
                    }
                }
            }
        }

        if (questions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("কোনো প্রশ্ন নেই! অনুগ্রহ করে নতুন প্রশ্ন যোগ করুন।", color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val startNum = viewModel.settings.startNum
                val filteredList = questions.filterIndexed { idx, _ ->
                    val currentNum = startNum + idx
                    val startOk = appliedStartRange == null || currentNum >= appliedStartRange!!
                    val endOk = appliedEndRange == null || currentNum <= appliedEndRange!!
                    startOk && endOk
                }

                if (filteredList.isEmpty()) {
                    item {
                        Text(
                            "এই রেঞ্জে কোনো প্রশ্ন নেই।",
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        )
                    }
                }

                itemsIndexed(filteredList) { fIdx, question ->
                    val originalIndex = questions.indexOf(question)
                    val currentNum = startNum + originalIndex
                    
                    // Display topic header if matches
                    val matchedTopic = topics.find { it.questionNum == currentNum }
                    if (matchedTopic != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(matchedTopic.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    // Question Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Edit/Delete buttons on screen preview
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${toBengaliNum(currentNum)}. ",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = question.questionText,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(onClick = { viewModel.setEditingQuestion(question) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { viewModel.deleteQuestion(question) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            // Question image
                            question.qImage?.let { qPath ->
                                AsyncImage(
                                    model = File(qPath),
                                    contentDescription = "Question Image Preview",
                                    modifier = Modifier
                                        .height(100.dp)
                                        .padding(vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Options
                            question.options.forEachIndexed { optIdx, option ->
                                val isCorrect = question.correctIndices.contains(optIdx)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .background(
                                            if (isCorrect && !question.isNoAnswer) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${getMarkerLabel(optIdx)}) ",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCorrect && !question.isNoAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Column {
                                        if (option.text.isNotEmpty()) {
                                            Text(option.text)
                                        }
                                        option.image?.let { optPath ->
                                            AsyncImage(
                                                model = File(optPath),
                                                contentDescription = "Option Image",
                                                modifier = Modifier.height(60.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Correct Answers box
                            Spacer(modifier = Modifier.height(8.dp))
                            val correctText = if (question.isNoAnswer) {
                                "উত্তর নেই"
                            } else {
                                question.correctIndices.joinToString(", ") { getMarkerLabel(it) }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "উত্তর: $correctText",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (!question.explanation.isNullOrEmpty() || !question.expImage.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "ব্যাখ্যা: ${question.explanation ?: ""}", fontSize = 12.sp)
                                        question.expImage?.let { expPath ->
                                            AsyncImage(
                                                model = File(expPath),
                                                contentDescription = "Explanation Image",
                                                modifier = Modifier.height(60.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StyleSection(viewModel: McqViewModel) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(viewModel.settings.isHeaderFooterEnabled) }
    var headerLeft by remember { mutableStateOf(viewModel.settings.headerLeft) }
    var headerRight by remember { mutableStateOf(viewModel.settings.headerRight) }
    var footerLeft by remember { mutableStateOf(viewModel.settings.footerLeft) }
    var footerRight by remember { mutableStateOf(viewModel.settings.footerRight) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("হেডার ও ফুটার স্টাইল সেটিংস", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "পিডিএফ এবং প্রিন্টের সময় পেজের উপরে এবং নিচে কাস্টম স্টাইল যুক্ত করতে এটি ব্যবহার করুন।",
            fontSize = 13.sp,
            color = Color.DarkGray
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("হেডার ও ফুটার ডিজাইন অন করুন", fontWeight = FontWeight.Bold)
                        Text("অন করলে প্রিভিউ ও প্রিন্টে এটি শো করবে।", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                HorizontalDivider()

                Text("হেডার (Header)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = headerLeft,
                    onValueChange = { headerLeft = it },
                    label = { Text("বাম পাশের টেক্সট") },
                    placeholder = { Text("যেমন: ঢাবি 'ক' ভর্তি পরীক্ষা ২০২৫-২৬") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = headerRight,
                    onValueChange = { headerRight = it },
                    label = { Text("ডান পাশের টেক্সট") },
                    placeholder = { Text("যেমন: প্রশ্ন-সমাধান") },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text("ফুটার (Footer)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = footerLeft,
                    onValueChange = { footerLeft = it },
                    label = { Text("বাম পাশের টেক্সট") },
                    placeholder = { Text("যেমন: উদ্ভাস একাডেমিক এন্ড এডমিশন কেয়ার") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = footerRight,
                    onValueChange = { footerRight = it },
                    label = { Text("ডান পাশের টেক্সট") },
                    placeholder = { Text("যেমন: পরিবর্তনের প্রত্যয়ে নিরন্তর পথচলা...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.settings.isHeaderFooterEnabled = enabled
                        viewModel.settings.headerLeft = headerLeft
                        viewModel.settings.headerRight = headerRight
                        viewModel.settings.footerLeft = footerLeft
                        viewModel.settings.footerRight = footerRight
                        Toast.makeText(context, "স্টাইল সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        viewModel.switchSection("list")
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("স্টাইল সেভ করুন")
                }
            }
        }
    }
}

@Composable
fun SettingsSection(viewModel: McqViewModel, questions: List<Question>) {
    val context = LocalContext.current
    var brandName by remember { mutableStateOf(viewModel.settings.brandName) }
    var startNum by remember { mutableStateOf(viewModel.settings.startNum.toString()) }
    var fontSize by remember { mutableStateOf(viewModel.settings.fontSize.toString()) }
    val watermarkPath = viewModel.settings.watermarkPath

    val watermarkPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.saveWatermarkImage(it) }
    }

    val csvImportPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            viewModel.importFromCSV(
                uri = it,
                onSuccess = { count ->
                    Toast.makeText(context, "$count টি প্রশ্ন সফলভাবে ইমপোর্ট হয়েছে!", Toast.LENGTH_LONG).show()
                    viewModel.switchSection("list")
                },
                onError = { err ->
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("সেটিংস (Settings)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("আপনার ব্র্যান্ডের নাম (যেমন: TCC)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = startNum,
                        onValueChange = { startNum = it },
                        label = { Text("শুরুর নম্বর") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        label = { Text("ফন্ট সাইজ (pt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        viewModel.settings.brandName = brandName
                        viewModel.settings.startNum = startNum.toIntOrNull() ?: 1
                        viewModel.settings.fontSize = fontSize.toIntOrNull() ?: 14
                        Toast.makeText(context, "সেটিংস সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("সেটিংস সেভ করুন")
                }
            }
        }

        // Watermark Config
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("জলছাপ (Watermark)", fontWeight = FontWeight.Bold)
                Text("পিডিএফ এর প্রতি পেজের মাঝে হালকা জলছাপ যুক্ত করুন।", fontSize = 11.sp, color = Color.Gray)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { watermarkPicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(if (watermarkPath == null) "জলছাপ ছবি নির্বাচন করুন" else "জলছাপ পরিবর্তন করুন")
                    }

                    if (watermarkPath != null) {
                        TextButton(onClick = { viewModel.clearWatermark() }) {
                            Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                watermarkPath?.let { path ->
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Watermark Preview",
                        modifier = Modifier
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        // CSV Import/Export Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CSV এক্সপোর্ট ও ইমপোর্ট", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "একসাথে অনেক প্রশ্ন এক্সেল বা স্প্রেডশিটে তৈরি করে এখানে ইমপোর্ট করতে পারবেন।\n" +
                            "টেবিল ফরম্যাট: Question, Option 1, Option 2..., Correct Answer, Explanation\n" +
                            "একাধিক উত্তর থাকলে Correct Answer কলামে কমা দিয়ে লিখুন (যেমন: ১,৩)। উত্তর না থাকলে ০ লিখুন।",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            val csvContent = CSVHelper.exportToCSV(questions)
                            if (csvContent.isNotEmpty()) {
                                saveCsvToFile(context, csvContent)
                            } else {
                                Toast.makeText(context, "এক্সপোর্ট করার জন্য কোনো প্রশ্ন নেই!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV ডাউনলোড")
                    }

                    Button(
                        onClick = {
                            // Launch CSV file picker.
                            // Android accepts text/* or specific mime type
                            csvImportPicker.launch("text/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV ইমপোর্ট")
                    }
                }
            }
        }

        // Danger zone Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("বিপদজনক অঞ্চল (Danger Zone)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Button(
                    onClick = {
                        viewModel.clearAllQuestions()
                        Toast.makeText(context, "সব প্রশ্নপত্র মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("সব প্রশ্ন মুছুন")
                }
            }
        }
    }
}

private fun getMarkerLabel(index: Int): String {
    val labels = listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ")
    return labels.getOrNull(index) ?: "${index + 1}"
}

private fun toBengaliNum(num: Int): String {
    val engToBng = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
    return num.toString().map { engToBng[it] ?: it }.joinToString("")
}

private fun saveCsvToFile(context: Context, csvContent: String) {
    try {
        val fileName = "MCQ_Questions_${System.currentTimeMillis()}.csv"
        // In Android, we can write it to the Cache or External files directory, and then share it!
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use {
            it.write("\uFEFF".toByteArray()) // Add BOM for excel support
            it.write(csvContent.toByteArray(Charsets.UTF_8))
        }

        // Share the generated CSV file
        val fileUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share CSV file via"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "ফাইল তৈরি করতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
