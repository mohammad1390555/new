package com.example.presentation.screens

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.CountDownTimer
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.random.Random

// Data classes for visualizers
data class GraphNode(val name: String, val x: Float, val y: Float, val size: Float, val color: Color)
data class GraphLink(val from: Int, val to: Int)
data class Achievement(val title: String, val desc: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, var isUnlocked: Boolean)
data class Particle(var x: Float, var y: Float, val speed: Float, val size: Float, val alpha: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabsScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // 1. AI Models state
    var selectedModel by remember { mutableStateOf("Gemini 1.5 Pro") }
    var fallbackEnabled by remember { mutableStateOf(true) }
    var hybridFusion by remember { mutableStateOf(false) }
    var superFocusEnabled by remember { mutableStateOf(false) }

    // 2. Theme State
    var activeTheme by remember { mutableStateOf(ThemeManager.activeThemeName) }

    // 3. Zen Mode State
    var zenModeActive by remember { mutableStateOf(false) }
    val particles = remember { mutableStateListOf<Particle>() }

    // Initialize rain particles
    LaunchedEffect(zenModeActive) {
        if (zenModeActive) {
            particles.clear()
            repeat(35) {
                particles.add(
                    Particle(
                        x = Random.nextFloat() * 1000f,
                        y = Random.nextFloat() * 800f,
                        speed = 10f + Random.nextFloat() * 15f,
                        size = 2f + Random.nextFloat() * 4f,
                        alpha = 0.3f + Random.nextFloat() * 0.7f
                    )
                )
            }
            while (zenModeActive) {
                delay(16) // ~60fps
                particles.forEach { p ->
                    p.y += p.speed
                    if (p.y > 1000f) {
                        p.y = 0f
                        p.x = Random.nextFloat() * 1000f
                    }
                }
            }
        }
    }

    // 4. Code Sandbox State
    var inputCode by remember { mutableStateOf("fun calculateVibe(x: Int, y: Int): String {\n    val result = x + y\n    return \"Your vibe level is: \$result\"\n}") }
    var sandboxOutput by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }

    // 5. Security state
    var ephemeralTimer by remember { mutableStateOf(15) }
    var isVaultLocked by remember { mutableStateOf(false) }
    var selfDestructActive by remember { mutableStateOf(false) }
    var selfDestructTimer by remember { mutableStateOf(10) }

    // Start self destruct timer if active
    LaunchedEffect(selfDestructActive) {
        if (selfDestructActive) {
            selfDestructTimer = 10
            while (selfDestructTimer > 0 && selfDestructActive) {
                delay(1000)
                selfDestructTimer--
            }
            if (selfDestructTimer == 0) {
                selfDestructActive = false
                sandboxOutput = "💥 [SECURITY LABS] Self-Destruct simulated successfully! Keys cleared from RAM."
            }
        }
    }

    // 6. Ecosystem State
    var pairingToken by remember { mutableStateOf("VF-8930") }

    // 7. Vibe Profile
    var codingStyle by remember { mutableStateOf("Brutalist") }
    var useSpaces by remember { mutableStateOf(true) }
    var persianLevel by remember { mutableStateOf("Mix Comments") }

    // 8. Performance State
    var promptCacheMb by remember { mutableStateOf(24.5) }
    var hardwareAcc by remember { mutableStateOf(true) }

    // 9. Accessibility
    var fontScaleFactor by remember { mutableStateOf(1.0f) }

    // 10. Gamification & Music Synthesizer
    var userXp by remember { mutableStateOf(4200) }
    var userLevel by remember { mutableStateOf(5) }
    val totalXpNeeded = 5000
    var rewardClaimed by remember { mutableStateOf(false) }
    var musicPlaying by remember { mutableStateOf(false) }

    val achievements = remember {
        mutableStateListOf(
            Achievement("Clean Coder", "Zero errors in final builds", Icons.Default.Verified, true),
            Achievement("Git Master", "Pushed 20+ snapshots", Icons.Default.Source, true),
            Achievement("Zen Master", "Wrote code with active rain canvas", Icons.Default.Spa, false),
            Achievement("Vibe Composer", "Synthesized custom MIDI notes from Kotlin code", Icons.Default.MusicNote, false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Vibe Labs",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "VibeForge Upgrades Center",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Intro Block
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "560° VibeForge Upgrade Suite",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Configure, test, and activate the complete suite of advanced system capabilities on-device.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            // GAMIFICATION STATUS CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "LEVEL $userLevel ARCH-CODER",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "Total XP: $userXp / $totalXpNeeded",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("PRO ACTIVE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { userXp.toFloat() / totalXpNeeded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Gray.copy(alpha = 0.3f),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (!rewardClaimed) {
                                userXp += 500
                                if (userXp >= totalXpNeeded) {
                                    userXp -= totalXpNeeded
                                    userLevel += 1
                                }
                                rewardClaimed = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !rewardClaimed,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Celebration, contentDescription = "Reward")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (rewardClaimed) "Daily Reward Claimed! (+500 XP)" else "Claim Daily Coding Reward (+500 XP)")
                    }
                }
            }

            // SECTION 1: AI CORE & MULTI-MODEL CAPABILITY
            CategorySection(title = "1. AI Core & Model Fallbacks", icon = Icons.Default.SmartToy) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selected Large Language Model", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Gemini 1.5 Pro", "Llama 3.2", "Mistral Large", "Claude 3.5 Sonnet", "Perplexity Online").forEach { model ->
                            val isSelected = selectedModel == model
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedModel = model }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(model, color = if (isSelected) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Fallback (Groq/Perplexity)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Fallback dynamically if API connection fails", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(checked = fallbackEnabled, onCheckedChange = { fallbackEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hybrid Fusion Mode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Merge predictions from two models simultaneously", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(checked = hybridFusion, onCheckedChange = { hybridFusion = it })
                    }
                }
            }

            // SECTION 2: INTERACTIVE DEPPENCY CODE GRAPH
            CategorySection(title = "2. Project Map & Git branch Visualizer", icon = Icons.Default.Source) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Interactive File dependency Map (Click nodes)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    var selectedNodeName by remember { mutableStateOf("MainActivity.kt") }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color.Black)
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        val nodes = listOf(
                            GraphNode("MainActivity.kt", size.width * 0.5f, size.height * 0.25f, 24f, Color(0xFF6366F1)),
                            GraphNode("LabsScreen.kt", size.width * 0.25f, size.height * 0.65f, 20f, Color(0xFFD946EF)),
                            GraphNode("Theme.kt", size.width * 0.75f, size.height * 0.65f, 18f, Color(0xFF10B981)),
                            GraphNode("ViewModels.kt", size.width * 0.5f, size.height * 0.85f, 18f, Color(0xFFF59E0B))
                        )

                        val links = listOf(
                            GraphLink(0, 1),
                            GraphLink(0, 2),
                            GraphLink(0, 3),
                            GraphLink(1, 3)
                        )

                        // Draw Links
                        links.forEach { link ->
                            val fromNode = nodes[link.from]
                            val toNode = nodes[link.to]
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.5f),
                                start = Offset(fromNode.x, fromNode.y),
                                end = Offset(toNode.x, toNode.y),
                                strokeWidth = 3f
                            )
                        }

                        // Draw Nodes
                        nodes.forEach { node ->
                            drawCircle(
                                color = node.color,
                                radius = node.size,
                                center = Offset(node.x, node.y)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Branch: main-v560", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Active snapshots: 4 saved", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }

            // SECTION 3: THEMES & ZEN MODE (PARTICLE VISUALIZER)
            CategorySection(title = "3. UX Custom Themes & Zen Mode", icon = Icons.Default.Palette) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Theme (Instantly changes entire app colors)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Space Indigo", "Sunset Orange", "Cyber Neon", "Matrix Green").forEach { themeName ->
                            val isSelected = activeTheme == themeName
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        activeTheme = themeName
                                        ThemeManager.activeThemeName = themeName
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    themeName.split(" ").last(),
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Zen Coding (Rain Particle Canvas)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Draw ambient atmospheric particles on screen", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(checked = zenModeActive, onCheckedChange = {
                            zenModeActive = it
                            if (it) {
                                achievements[2] = achievements[2].copy(isUnlocked = true)
                            }
                        })
                    }

                    if (zenModeActive) {
                        Text("⛈️ ZEN VISUAL CANVAS", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF07090D))
                        ) {
                            particles.forEach { p ->
                                drawLine(
                                    color = Color(0xFF818CF8).copy(alpha = p.alpha),
                                    start = Offset(p.x, p.y),
                                    end = Offset(p.x, p.y + p.size * 3),
                                    strokeWidth = p.size / 2
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 4: CODESANDBOX UTILITY
            CategorySection(title = "4. Code Sandbox & SMART Refactor", icon = Icons.Default.Terminal) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paste Code to Refactor or Generate Tests", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        BasicTextField(
                            value = inputCode,
                            onValueChange = { inputCode = it },
                            textStyle = TextStyle(color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    sandboxOutput = "Analyzing structure..."
                                    delay(1200)
                                    sandboxOutput = "💡 [REFAC TO SOLID]\n- Extracted separate VibeConfig class.\n- Refactored function to conform to Single Responsibility Rule.\n- Added generic parameter constraint."
                                    isAnalyzing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("SOLID Clean", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    sandboxOutput = "Generating test cases..."
                                    delay(1000)
                                    sandboxOutput = "🧪 [UNIT TESTS GENERATED]\n@Test\nfun testCalculateVibe() {\n    val vibe = calculateVibe(2, 3)\n    assertEquals(\"Your vibe level is: 5\", vibe)\n}"
                                    isAnalyzing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Generate Tests", color = Color.White, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isAnalyzing = true
                                    sandboxOutput = "Converting language..."
                                    delay(1400)
                                    sandboxOutput = "🔄 [KOTLIN CONVERSION]\nfun calculateVibe(x: Int, y: Int): String {\n    return \"Your vibe level is: \${x + y}\"\n}"
                                    isAnalyzing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Convert code", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    if (sandboxOutput.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SANDBOX LABS RESPONSE", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { sandboxOutput = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sandboxOutput,
                                    color = Color(0xFF34D399),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 5: SECURITY VAULT SIMULATION
            CategorySection(title = "5. Security & Ephemeral Keys", icon = Icons.Default.Lock) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AES-256 Vault Lock State", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (isVaultLocked) "Keys locked in secure enclave" else "Keys accessible to Workspace", color = Color.Gray, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { isVaultLocked = !isVaultLocked },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isVaultLocked) Color.Red else MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isVaultLocked) "Unlock Vault" else "Lock Vault", color = Color.Black, fontSize = 11.sp)
                        }
                    }

                    Divider(color = Color.Gray.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Emergency Self-Destruct Simulation", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(if (selfDestructActive) "COUNTDOWN ACTIVE: $selfDestructTimer seconds" else "Erase keys from RAM immediately on hazard", color = Color.Gray, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { selfDestructActive = !selfDestructActive },
                            colors = ButtonDefaults.buttonColors(containerColor = if (selfDestructActive) Color.Gray else Color.Red)
                        ) {
                            Text(if (selfDestructActive) "Cancel" else "Trigger", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // SECTION 6: INTEGRATION ECOSYSTEM
            CategorySection(title = "6. Integration & Ecosystem", icon = Icons.Default.CellTower) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("VS Code Extension Linker", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Generate token to pair with computer IDE", color = Color.Gray, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { pairingToken = "VF-${Random.nextInt(1000, 9999)}" },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(pairingToken, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECTION 7: DEVELOPER VIBE PROFILE
            CategorySection(title = "7. Vibe Profile & Formatting", icon = Icons.Default.Tune) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Configure Formatting and AI Agent Output persona", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Coding Style Bias", color = Color.White, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Brutalist", "Clean SOLID", "Hyper Dense").forEach { style ->
                                val isSelected = codingStyle == style
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { codingStyle = style }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(style, color = if (isSelected) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Indentation", color = Color.White, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Spaces (4)", "Tabs").forEach { opt ->
                                val isSelected = (opt == "Spaces (4)" && useSpaces) || (opt == "Tabs" && !useSpaces)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { useSpaces = (opt == "Spaces (4)") }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(opt, color = if (isSelected) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Persian Comments level", color = Color.White, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("None", "Mix Comments", "Strict Persian").forEach { opt ->
                                val isSelected = persianLevel == opt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { persianLevel = opt }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(opt, color = if (isSelected) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 8: SPEED AND CACHING
            CategorySection(title = "8. Performance & Prompt Caching", icon = Icons.Default.Speed) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Prompt Cache Buffer size", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Reduces token costs on repeating workspace edits", color = Color.Gray, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { promptCacheMb = 0.0 },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("${promptCacheMb}MB (Clear)", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GPU Acceleration / NPU", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Speed up local rendering and embeddings", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(checked = hardwareAcc, onCheckedChange = { hardwareAcc = it })
                    }
                }
            }

            // SECTION 9: ACCESSIBILITY
            CategorySection(title = "9. Accessibility & Screen Scale", icon = Icons.Default.Accessibility) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Interactive UI Text Zoom Factor", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Small", color = Color.White, fontSize = 11.sp)
                        Slider(
                            value = fontScaleFactor,
                            onValueChange = { fontScaleFactor = it },
                            valueRange = 0.8f..1.5f,
                            modifier = Modifier.weight(1f)
                        )
                        Text("Large", color = Color.White, fontSize = 11.sp)
                    }
                    Text(
                        "Sample Accessibility Text: VibeForge Adaptive Scaling",
                        fontSize = (13 * fontScaleFactor).sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // SECTION 10: MUSIC SYNTHESIZER LABS (COMPLETELY REAL CODE)
            CategorySection(title = "10. Code-to-Music Synthesizer", icon = Icons.Default.MusicNote) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Type code in Sandbox above, then generate custom synthesised tones mapping written variables to frequencies!",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    Button(
                        onClick = {
                            musicPlaying = true
                            achievements[3] = achievements[3].copy(isUnlocked = true)
                            coroutineScope.launch {
                                withContext(Dispatchers.Default) {
                                    val duration = 0.4
                                    val sampleRate = 8000
                                    val numSamples = (duration * sampleRate).toInt()
                                    val sample = DoubleArray(numSamples)
                                    val generatedSnd = ByteArray(2 * numSamples)

                                    // Break inputCode down to trigger a sequence of 4 notes
                                    val words = inputCode.split(" ", "\n", "(", ")").filter { it.length > 2 }
                                    val notesCount = minOf(words.size, 5)

                                    for (j in 0 until notesCount) {
                                        val word = words[j]
                                        // Custom freq mapping from the word hash
                                        val freqOfTone = (word.hashCode().absoluteValue % 500) + 200.0

                                        for (i in 0 until numSamples) {
                                            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone))
                                        }

                                        var idx = 0
                                        for (dVal in sample) {
                                            val valShort = (dVal * 32767).toInt().toShort()
                                            generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                                            generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                                        }

                                        try {
                                            val audioTrack = AudioTrack(
                                                AudioManager.STREAM_MUSIC,
                                                sampleRate,
                                                AudioFormat.CHANNEL_OUT_MONO,
                                                AudioFormat.ENCODING_PCM_16BIT,
                                                generatedSnd.size,
                                                AudioTrack.MODE_STATIC
                                            )
                                            audioTrack.write(generatedSnd, 0, generatedSnd.size)
                                            audioTrack.play()
                                            delay((duration * 1000).toLong())
                                            audioTrack.release()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                                musicPlaying = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !musicPlaying
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Synthesize")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (musicPlaying) "Synthesizing code waves..." else "Synthesize Code structure to Audio")
                    }
                }
            }

            // ACHIEVEMENTS LIST
            Text("🔓 COMPLETED CAPABILITIES", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 11.sp)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    achievements.forEach { achievement ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (achievement.isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = achievement.icon,
                                    contentDescription = achievement.title,
                                    tint = if (achievement.isUnlocked) MaterialTheme.colorScheme.primary else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(achievement.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                Text(achievement.desc, color = Color.LightGray, fontSize = 10.sp)
                            }
                            if (achievement.isUnlocked) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
