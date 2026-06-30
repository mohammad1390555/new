package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import com.example.presentation.screens.ApiKeySettingsScreen
import com.example.presentation.screens.ChatScreen
import com.example.presentation.screens.WorkspaceScreen
import com.example.presentation.screens.LabsScreen
import com.example.presentation.viewmodel.ApiKeyViewModel
import com.example.presentation.viewmodel.ChatViewModel
import com.example.presentation.viewmodel.ViewModelFactory
import com.example.presentation.viewmodel.WorkspaceViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var apiKeyViewModel: ApiKeyViewModel
    private lateinit var workspaceViewModel: WorkspaceViewModel
    private lateinit var chatViewModel: ChatViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Manual DI & Retrieve ViewModels
        val app = application as VibeForgeApp
        val factory = ViewModelFactory(
            manageApiKeysUseCase = app.manageApiKeysUseCase,
            workspaceUseCase = app.workspaceFileUseCase,
            chatUseCase = app.aiCodingChatUseCase,
            apiKeyRepository = app.apiKeyRepository
        )

        apiKeyViewModel = ViewModelProvider(this, factory)[ApiKeyViewModel::class.java]
        workspaceViewModel = ViewModelProvider(this, factory)[WorkspaceViewModel::class.java]
        chatViewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }

    @Composable
    fun MainAppLayout() {
        var currentTab by remember { mutableStateOf("chat") }
        val ImmersiveBg = Color(0xFF0E1015)
        val ImmersiveAccent = Color(0xFF6366F1)
        val Slate500 = Color(0xFF64748B)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(containerColor = ImmersiveBg) {
                    NavigationBarItem(
                        selected = currentTab == "chat",
                        onClick = { currentTab = "chat" },
                        icon = { Icon(Icons.Default.ChatBubble, contentDescription = "AI Chat") },
                        label = { Text("Chat") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF818CF8), // Indigo 400
                            selectedTextColor = Color(0xFF93C5FD), // Indigo 300 / slate accent
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500,
                            indicatorColor = ImmersiveAccent.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "workspace",
                        onClick = { currentTab = "workspace" },
                        icon = { Icon(Icons.Default.Code, contentDescription = "Workspace") },
                        label = { Text("Files") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF818CF8),
                            selectedTextColor = Color(0xFF93C5FD),
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500,
                            indicatorColor = ImmersiveAccent.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "settings",
                        onClick = { currentTab = "settings" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Keys") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF818CF8),
                            selectedTextColor = Color(0xFF93C5FD),
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500,
                            indicatorColor = ImmersiveAccent.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == "labs",
                        onClick = { currentTab = "labs" },
                        icon = { Icon(Icons.Default.Science, contentDescription = "Labs") },
                        label = { Text("Labs") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF818CF8),
                            selectedTextColor = Color(0xFF93C5FD),
                            unselectedIconColor = Slate500,
                            unselectedTextColor = Slate500,
                            indicatorColor = ImmersiveAccent.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    "chat" -> ChatScreen(chatViewModel, workspaceViewModel)
                    "workspace" -> WorkspaceScreen(workspaceViewModel)
                    "settings" -> ApiKeySettingsScreen(apiKeyViewModel)
                    "labs" -> LabsScreen()
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
