package com.example.presentation.screens

import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.ApiKeyEntity
import com.example.data.database.ChatMessageEntity
import com.example.data.database.SnapshotEntity
import com.example.presentation.viewmodel.ApiKeyViewModel
import com.example.presentation.viewmodel.ChatViewModel
import com.example.presentation.viewmodel.WorkspaceViewModel
import com.example.service.FileNode
import java.text.SimpleDateFormat
import java.util.*

// Theme Colors
val ImmersiveBg = Color(0xFF0E1015)
val JetSlate = ImmersiveBg // Fallback for reference consistency
val Slate700 = Color(0xFF1E293B) // Slate 800 for card backgrounds
val AccentTeal = Color(0xFF6366F1) // Indigo 500 accent color
val CyberPink = Color(0xFFD946EF) // Fuchsia 600 accent color
val TerminalGreen = Color(0xFF10B981) // Emerald / Terminal Green
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val UserBubbleColor = Color(0xFF4F46E5) // Indigo 600
val AssistantBubbleColor = Color(0xFF1A1C23)
val AssistantBorderColor = Color(0x11FFFFFF)

// --- CHAT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    workspaceViewModel: WorkspaceViewModel
) {
    val messages by chatViewModel.messagesState.collectAsStateWithLifecycle()
    val isGenerating by chatViewModel.isGenerating.collectAsStateWithLifecycle()
    val activeStreaming by chatViewModel.activeStreamingResponse.collectAsStateWithLifecycle()
    val provider by chatViewModel.activeProvider.collectAsStateWithLifecycle()
    val currentSessionId by chatViewModel.currentSessionId.collectAsStateWithLifecycle()
    val rootUri by workspaceViewModel.projectRootUri.collectAsStateWithLifecycle()
    val pinnedFiles by workspaceViewModel.pinnedFiles.collectAsStateWithLifecycle()

    var inputMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Voice to text launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let {
                inputMessage += " $it"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF6366F1), Color(0xFFD946EF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "V",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("VibeForge Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981)) // Emerald 500
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (rootUri != null) "Workspace: ${rootUri!!.path?.split("/")?.lastOrNull() ?: "Active"}" else "Active • Gemini 1.5 Pro",
                                    fontSize = 10.sp,
                                    color = Slate400,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { chatViewModel.clearMessages() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = Slate400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JetSlate)
            )
        },
        containerColor = JetSlate
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Provider selector chip row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("GEMINI", "OPENAI", "CLAUDE", "GROQ", "DEEPSEEK").forEach { p ->
                    val isSelected = provider.uppercase() == p
                    FilterChip(
                        selected = isSelected,
                        onClick = { chatViewModel.setProvider(p) },
                        label = { Text(p, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentTeal,
                            selectedLabelColor = Color.Black,
                            containerColor = Slate700,
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("provider_chip_$p")
                    )
                }
            }

            // Pinned files row indicator
            if (pinnedFiles.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pinnedFiles.forEach { pin ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberPink.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SizedIcon(Icons.Default.PushPin, contentDescription = "Pinned", tint = CyberPink, size = 12.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(pin.displayName, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Messages history list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = false
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }

                if (isGenerating && activeStreaming.isNotEmpty()) {
                    item {
                        StreamingBubble(text = activeStreaming)
                    }
                }

                if (isGenerating && activeStreaming.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentTeal)
                        }
                    }
                }
            }

            // Message Input bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate700),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate code prompt...")
                            }
                            try {
                                voiceLauncher.launch(intent)
                            } catch (e: Exception) {
                                // Speech not available
                            }
                        },
                        modifier = Modifier.testTag("voice_button")
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice prompt", tint = AccentTeal)
                    }

                    TextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { Text("Describe & build something...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (inputMessage.trim().isNotEmpty()) {
                                chatViewModel.sendMessage(inputMessage, rootUri)
                                inputMessage = ""
                            }
                        },
                        modifier = Modifier
                            .testTag("submit_button")
                            .clip(CircleShape)
                            .background(AccentTeal),
                        enabled = !isGenerating
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SizedIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, tint: Color, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

// Custom Speech bubble for Chat messages
@Composable
fun MessageBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    val isTool = message.role == "tool"
    
    val bubbleColor = when {
        isUser -> UserBubbleColor
        isTool -> Color(0xFF101216)
        else -> AssistantBubbleColor
    }

    val contentColor = Color.White

    val bubbleAlignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = bubbleAlignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(bubbleShape)
                .then(
                    if (!isUser && !isTool) {
                        Modifier.border(1.dp, Color(0x0FFFFFFF), bubbleShape)
                    } else {
                        Modifier
                    }
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Column {
                // Role Header tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    val tagIcon = when {
                        isUser -> Icons.Default.Person
                        isTool -> Icons.Default.Terminal
                        else -> Icons.Default.SmartToy
                    }
                    val tagName = when {
                        isUser -> "YOU"
                        isTool -> "SYSTEM WORKSPACE"
                        else -> "VIBEFORGE AI"
                    }
                    val tagColor = when {
                        isUser -> Color(0xFF93C5FD) // Light Indigo/Blue for user tag
                        isTool -> CyberPink
                        else -> Color(0xFFA5B4FC) // Indigo 300
                    }
                    SizedIcon(tagIcon, contentDescription = tagName, tint = tagColor, size = 12.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(tagName, color = tagColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                
                // Formatted content (handles code blocks visually!)
                FormattedTextMessage(text = message.content, isCode = isTool, textColor = contentColor)
            }
        }
    }
}

@Composable
fun StreamingBubble(text: String) {
    val bubbleShape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(bubbleShape)
                .border(1.dp, Color(0x0FFFFFFF), bubbleShape)
                .background(AssistantBubbleColor)
                .padding(12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SizedIcon(Icons.Default.SmartToy, contentDescription = "AI", tint = Color(0xFFA5B4FC), size = 12.dp)
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("VIBEFORGE AI (typing...)", color = Color(0xFFA5B4FC), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                FormattedTextMessage(text = text, isCode = false, textColor = Color.White)
            }
        }
    }
}

// Text formatter supporting code block extraction
@Composable
fun FormattedTextMessage(text: String, isCode: Boolean, textColor: Color) {
    val codeBorderShape = RoundedCornerShape(8.dp)
    if (isCode) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(codeBorderShape)
                .border(1.dp, Color(0x11FFFFFF), codeBorderShape)
                .background(Color(0xFF111216))
                .padding(10.dp)
        ) {
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFFF1F5F9) // Slate 100
            )
        }
    } else {
        // Split and identify standard code blocks denoted by triple backticks ```
        val parts = text.split("```")
        Column {
            parts.forEachIndexed { index, part ->
                if (index % 2 == 1) { // Code block
                    val lines = part.trim().split("\n")
                    val lang = lines.firstOrNull() ?: ""
                    val codeContent = if (lines.size > 1) lines.drop(1).joinToString("\n") else part

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(codeBorderShape)
                            .border(1.dp, Color(0x11FFFFFF), codeBorderShape)
                            .background(Color(0xFF111216))
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1C1D24))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = lang.uppercase(),
                                    color = Color(0xFF94A3B8), // Slate 400
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = codeContent.trim(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8), // Cool sky blue for code content
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                } else { // Standard text
                    Text(text = part, color = textColor, fontSize = 13.sp)
                }
            }
        }
    }
}


// --- WORKSPACE SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel
) {
    val rootUri by viewModel.projectRootUri.collectAsStateWithLifecycle()
    val tree by viewModel.workspaceTree.collectAsStateWithLifecycle()
    val activeFilePath by viewModel.selectedFilePath.collectAsStateWithLifecycle()
    val activeFileContent by viewModel.selectedFileContent.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    val pinnedFiles by viewModel.pinnedFiles.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var createFileName by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    // SAF directory picker contract
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            // Persist URI permissions
            val takeFlags: Int = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            viewModel.setProjectRoot(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Workspace", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = { viewModel.refreshWorkspace() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh files", tint = Color.LightGray)
                    }
                    IconButton(onClick = { showCreateDialog = true }, enabled = rootUri != null) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "New File", tint = AccentTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JetSlate)
            )
        },
        containerColor = JetSlate
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (rootUri == null) {
                // Empty state to pick directory
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SizedIcon(Icons.Outlined.FolderOpen, contentDescription = "No project", tint = Slate700, size = 80.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No Workspace Folder Selected",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Authorize VibeForge to access a folder on your device. Once authorized, the AI can read, write, and repair files natively.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { directoryPicker.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        modifier = Modifier.testTag("picker_button")
                    ) {
                        Text("Pick Project Root Folder", color = Color.Black)
                    }
                }
            } else {
                // Files or editor view
                Row(modifier = Modifier.fillMaxSize()) {
                    // Tree List Sidebar (takes full screen if no active file, otherwise 40% width)
                    val sidebarWeight = if (activeFilePath == null) 1f else 0.45f
                    Column(
                        modifier = Modifier
                            .weight(sidebarWeight)
                            .fillMaxHeight()
                            .background(Color(0xFF0B0F19))
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "PROJECT DIRECTORY",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                        )
                        
                        if (isLoading && tree.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AccentTeal)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(tree) { node ->
                                    TreeViewNode(
                                        node = node,
                                        level = 0,
                                        selectedPath = activeFilePath,
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }

                    // File content Editor (takes remaining 55% width)
                    if (activeFilePath != null) {
                        Column(
                            modifier = Modifier
                                .weight(0.55f)
                                .fillMaxHeight()
                                .background(JetSlate)
                                .padding(8.dp)
                        ) {
                            var editedContent by remember(activeFilePath, activeFileContent) {
                                mutableStateOf(activeFileContent ?: "")
                            }
                            val isFilePinned = pinnedFiles.any { it.filePath == activeFilePath }

                            // Editor Header bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate700)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        activeFilePath!!.split("/").last(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        activeFilePath!!,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.togglePinFile(activeFilePath!!, isFilePinned) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    SizedIcon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pin File",
                                        tint = if (isFilePinned) CyberPink else Color.LightGray,
                                        size = 16.dp
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.saveFileContent(activeFilePath!!, editedContent) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    SizedIcon(Icons.Default.Save, contentDescription = "Save file", tint = AccentTeal, size = 16.dp)
                                }

                                IconButton(
                                    onClick = { viewModel.closeFile() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    SizedIcon(Icons.Default.Close, contentDescription = "Close file", tint = Color.Red, size = 16.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Custom Syntax Editor
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .padding(8.dp)
                            ) {
                                BasicTextField(
                                    value = editedContent,
                                    onValueChange = { editedContent = it },
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.fillMaxSize(),
                                    cursorBrush = SolidColor(AccentTeal)
                                )
                            }

                            // Snapshot Rollbacks List
                            if (snapshots.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Slate700.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("BACKUP SNAPSHOTS (Git-Rollback)", color = CyberPink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LazyColumn(modifier = Modifier.height(80.dp)) {
                                            items(snapshots) { snap ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { viewModel.rollback(snap) }
                                                        .padding(vertical = 4.dp, horizontal = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    SizedIcon(Icons.Default.Restore, contentDescription = "Rollback", tint = CyberPink, size = 12.dp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(snap.description, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(snap.timestamp))
                                                        Text("Saved: $dateStr", color = Color.LightGray, fontSize = 8.sp)
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
        }

        // New File Creation Dialog
        if (showCreateDialog) {
            Dialog(onDismissRequest = { showCreateDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate700),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Create New File", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        OutlinedTextField(
                            value = createFileName,
                            onValueChange = { createFileName = it },
                            placeholder = { Text("relative/path/file.kt", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.testTag("new_file_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCreateDialog = false }) {
                                Text("Cancel", color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (createFileName.trim().isNotEmpty()) {
                                        viewModel.createNewFile(createFileName, "")
                                        createFileName = ""
                                        showCreateDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                            ) {
                                Text("Create", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutlinedTextField(value: String, onValueChange: (String) -> Unit, placeholder: @Composable () -> Unit, textStyle: TextStyle, modifier: Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        textStyle = textStyle,
        modifier = modifier
    )
}

// Tree view Node component
@Composable
fun TreeViewNode(
    node: FileNode,
    level: Int,
    selectedPath: String?,
    viewModel: WorkspaceViewModel
) {
    val isExpanded = viewModel.expandedFolders[node.relativePath] ?: false
    val isSelected = selectedPath == node.relativePath

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory) {
                        viewModel.toggleFolderExpanded(node.relativePath)
                    } else {
                        viewModel.selectFile(node.relativePath)
                    }
                }
                .background(if (isSelected) Slate700.copy(alpha = 0.5f) else Color.Transparent)
                .padding(vertical = 6.dp, horizontal = 4.dp)
                .padding(start = (level * 12).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDirectory) {
                SizedIcon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowRight,
                    contentDescription = "Expand/Collapse",
                    tint = Color.Gray,
                    size = 16.dp
                )
                SizedIcon(Icons.Default.Folder, contentDescription = "Folder", tint = AccentTeal, size = 16.dp)
            } else {
                Spacer(modifier = Modifier.width(16.dp))
                SizedIcon(Icons.Default.Description, contentDescription = "File", tint = Color.LightGray, size = 16.dp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                node.name,
                color = if (isSelected) AccentTeal else Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (node.isDirectory && isExpanded) {
            node.children.forEach { child ->
                TreeViewNode(
                    node = child,
                    level = level + 1,
                    selectedPath = selectedPath,
                    viewModel = viewModel
                )
            }
        }
    }
}


// --- API KEY CONFIG / SETTINGS SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    viewModel: ApiKeyViewModel
) {
    val keys by viewModel.apiKeysState.collectAsStateWithLifecycle()
    val testResults by viewModel.testResults.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var currentEditingKey by remember { mutableStateOf<ApiKeyEntity?>(null) }

    // Dialog state variables
    var provider by remember { mutableStateOf("GEMINI") }
    var name by remember { mutableStateOf("") }
    var keyVal by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Configuration", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = JetSlate)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    currentEditingKey = null
                    provider = "GEMINI"
                    name = ""
                    keyVal = ""
                    customUrl = ""
                    isDefault = true
                    showAddDialog = true
                },
                containerColor = AccentTeal,
                modifier = Modifier.testTag("add_key_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Config", tint = Color.Black)
            }
        },
        containerColor = JetSlate
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp)
        ) {
            // Safety warn banner (requested by security/android-secret-management guidelines!)
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberPink.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SizedIcon(Icons.Default.Warning, contentDescription = "Security Alert", tint = CyberPink, size = 20.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Security Information", color = CyberPink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Android APKs can be easily decompiled and static variables extracted. VibeForge stores all keys securely encrypted using AES-256 inside the hardware-backed Android KeyStore system.",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (keys.isEmpty()) {
                // Empty state for keys
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SizedIcon(Icons.Outlined.Key, contentDescription = "No keys", tint = Slate700, size = 80.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Providers Configured", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Configure API credentials to start coding. You can also point to a local Ollama or LM Studio gateway via WiFi address for 100% offline usage.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(keys) { key ->
                        ApiKeyRowItem(
                            key = key,
                            testResult = testResults[key.id],
                            isTesting = isTesting[key.id] ?: false,
                            onTest = { viewModel.testKey(key.id) },
                            onEdit = {
                                currentEditingKey = key
                                provider = key.provider
                                name = key.name
                                keyVal = "" // Do not display raw encrypted key values for safety
                                customUrl = key.baseUrl
                                isDefault = key.isDefault
                                showAddDialog = true
                            },
                            onDelete = { viewModel.deleteApiKey(key) }
                        )
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate700),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (currentEditingKey == null) "Add API Configuration" else "Edit Configuration",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )

                        // Provider dropdown simulated with options row
                        Text("Model Provider", color = Color.LightGray, fontSize = 11.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("GEMINI", "OPENAI", "CLAUDE", "GROQ", "DEEPSEEK", "OLLAMA").forEach { p ->
                                val selected = provider == p
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) AccentTeal else Color.Black)
                                        .clickable { provider = p }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(p, color = if (selected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Nickname (e.g. My Prod Key)", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("key_name_input")
                        )

                        OutlinedTextField(
                            value = keyVal,
                            onValueChange = { keyVal = it },
                            placeholder = { Text(if (currentEditingKey != null) "•••••••• (Leave blank to keep)" else "Paste API Key Here", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("key_value_input")
                        )

                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            placeholder = { Text("Custom Endpoint (e.g. LM Studio proxy)", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("key_endpoint_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isDefault,
                                onCheckedChange = { isDefault = it },
                                colors = CheckboxDefaults.colors(checkedColor = AccentTeal)
                            )
                            Text("Set as Default Provider Key", color = Color.White, fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel", color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (name.trim().isNotEmpty()) {
                                        if (currentEditingKey == null) {
                                            viewModel.addApiKey(provider, name, keyVal, customUrl, isDefault)
                                        } else {
                                            viewModel.editApiKey(
                                                currentEditingKey!!,
                                                name,
                                                keyVal.takeIf { it.isNotEmpty() },
                                                customUrl,
                                                isDefault
                                            )
                                        }
                                        showAddDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                modifier = Modifier.testTag("key_save_button")
                            ) {
                                Text("Save Configuration", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Key Config list item row
@Composable
fun ApiKeyRowItem(
    key: ApiKeyEntity,
    testResult: String?,
    isTesting: Boolean,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (key.status.uppercase()) {
        "ACTIVE" -> TerminalGreen
        "EXHAUSTED" -> CyberPink
        else -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Slate700),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Provider Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentTeal.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(key.provider, color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(key.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    if (key.baseUrl.isNotEmpty()) {
                        Text(key.baseUrl, color = Color.LightGray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                // Default badge
                if (key.isDefault) {
                    SizedIcon(Icons.Default.PushPin, contentDescription = "Default", tint = CyberPink, size = 14.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Status tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(key.status, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lower Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isTesting) {
                    CircularProgressIndicator(color = AccentTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Testing line credentials...", color = Color.LightGray, fontSize = 11.sp)
                } else {
                    Button(
                        onClick = onTest,
                        colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("test_key_${key.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SizedIcon(Icons.Default.Speed, contentDescription = "Test Key", tint = AccentTeal, size = 12.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Connectivity", color = AccentTeal, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    SizedIcon(Icons.Default.Edit, contentDescription = "Edit Key", tint = Color.White, size = 14.dp)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    SizedIcon(Icons.Default.Delete, contentDescription = "Delete Key", tint = Color.Red, size = 14.dp)
                }
            }

            // Connection Test Output
            if (testResult != null && !isTesting) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black)
                        .padding(6.dp)
                ) {
                    Text(testResult, color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
            }
        }
    }
}
