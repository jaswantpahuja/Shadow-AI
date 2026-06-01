package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush as BrushIcon
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ChatMessage
import com.example.data.local.ChatSession
import com.example.ui.ChatViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ShadowBlack
import com.example.ui.theme.ShadowCard
import com.example.ui.theme.ShadowCardSelected
import com.example.ui.theme.ShadowDarkBackground
import com.example.ui.theme.ShadowNeonCyan
import com.example.ui.theme.ShadowNeonPink
import com.example.ui.theme.ShadowNeonPurple
import com.example.ui.theme.ShadowTextPrimary
import com.example.ui.theme.ShadowTextSecondary
import com.example.ui.theme.ShadowTextDisabled
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val app = application
                val viewModel: ChatViewModel = viewModel(
                    factory = ChatViewModel.provideFactory(app)
                )
                ShadowMainScreen(viewModel)
            }
        }
    }
}

@Composable
fun ShadowMainScreen(viewModel: ChatViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val selectedSessionId by viewModel.selectedSessionId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val currentPrompt by viewModel.currentPrompt.collectAsState()
    val selectedRatio by viewModel.selectedAspectRatio.collectAsState()
    val selectedSize by viewModel.selectedImageSize.collectAsState()

    var showDrawerOnMobile by remember { mutableStateOf(false) }
    var lightboxImageMessage by remember { mutableStateOf<ChatMessage?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ShadowBlack)
    ) {
        val isExpanded = maxWidth > 720.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar for wide screens (tablets, desktops, landscape foldables)
            if (isExpanded) {
                SidebarContent(
                    sessions = sessions,
                    selectedSessionId = selectedSessionId,
                    selectedRatio = selectedRatio,
                    selectedSize = selectedSize,
                    isGenerating = isGenerating,
                    onSelectSession = { viewModel.selectSession(it) },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onNewSession = { viewModel.startNewSession() },
                    onRatioChanged = { viewModel.updateAspectRatio(it) },
                    onSizeChanged = { viewModel.updateImageSize(it) }
                )
                VerticalDivider(color = ShadowNeonCyan.copy(alpha = 0.15f), thickness = 1.dp)
            }

            // Main chat environment
            ChatPane(
                modifier = Modifier.weight(1f),
                sessions = sessions,
                selectedSessionId = selectedSessionId,
                messages = messages,
                isGenerating = isGenerating,
                currentPrompt = currentPrompt,
                selectedRatio = selectedRatio,
                selectedSize = selectedSize,
                isExpandedLayout = isExpanded,
                onPromptChanged = { viewModel.updatePrompt(it) },
                onSend = { viewModel.sendPrompt() },
                onMenuClicked = { showDrawerOnMobile = true },
                onNewSessionMobile = { viewModel.startNewSession() },
                onRatioChanged = { viewModel.updateAspectRatio(it) },
                onSizeChanged = { viewModel.updateImageSize(it) },
                onImageClicked = { lightboxImageMessage = it }
            )
        }

        // Sliding Sidebar Overlay drawer on Compact Screens (Mobile Portrait)
        if (!isExpanded) {
            AnimatedVisibility(
                visible = showDrawerOnMobile,
                enter = slideInHorizontally { -it } + fadeIn(),
                exit = slideOutHorizontally { -it } + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showDrawerOnMobile = false }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(290.dp)
                            .background(ShadowDarkBackground)
                            .clickable(enabled = false) {}
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SHADOW PANELS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ShadowNeonCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                                IconButton(onClick = { showDrawerOnMobile = false }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Panel",
                                        tint = ShadowTextSecondary
                                    )
                                }
                            }
                            HorizontalDivider(color = ShadowNeonCyan.copy(alpha = 0.15f))
                            SidebarContent(
                                sessions = sessions,
                                selectedSessionId = selectedSessionId,
                                selectedRatio = selectedRatio,
                                selectedSize = selectedSize,
                                isGenerating = isGenerating,
                                onSelectSession = {
                                    viewModel.selectSession(it)
                                    showDrawerOnMobile = false
                                },
                                onDeleteSession = { viewModel.deleteSession(it) },
                                onNewSession = { viewModel.startNewSession() },
                                onRatioChanged = { viewModel.updateAspectRatio(it) },
                                onSizeChanged = { viewModel.updateImageSize(it) }
                            )
                        }
                    }
                }
            }
        }

        // Full Screen Lightbox Modal Overlay
        lightboxImageMessage?.let { originalMessage ->
            LightboxOverlay(
                message = originalMessage,
                onClose = { lightboxImageMessage = null }
            )
        }
    }
}

@Composable
fun SidebarContent(
    sessions: List<ChatSession>,
    selectedSessionId: Long?,
    selectedRatio: String,
    selectedSize: String,
    isGenerating: Boolean,
    onSelectSession: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onNewSession: () -> Unit,
    onRatioChanged: (String) -> Unit,
    onSizeChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(ShadowBlack)
            .padding(vertical = 16.dp)
    ) {
        // App header inside Sidebar (Only show if not already rendered as part of drawer header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.BrushIcon,
                contentDescription = "Shadow AI Logo Icon",
                modifier = Modifier
                    .size(24.dp)
                    .drawBehind {
                        drawCircle(
                            color = ShadowNeonCyan.copy(alpha = 0.4f),
                            radius = size.width * 0.7f,
                            center = center
                        )
                    },
                tint = ShadowNeonCyan
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "SHADOW AI",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 1.5.sp,
                color = ShadowTextPrimary,
                fontFamily = FontFamily.SansSerif
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Create New Canvas Button
        Button(
            onClick = onNewSession,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("create_session_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = ShadowNeonCyan.copy(alpha = 0.08f),
                contentColor = ShadowNeonCyan
            ),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShadowNeonCyan.copy(alpha = 0.4f))
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "New Session Icon")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Dark Canvas",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "RENDER HISTORIES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ShadowNeonPurple,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Sessions list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (sessions.isEmpty()) {
                item {
                    Text(
                        text = "No canvases rendered.",
                        color = ShadowTextSecondary.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    val isSelected = session.id == selectedSessionId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ShadowCardSelected else Color.Transparent)
                            .clickable { onSelectSession(session.id) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("session_item_${session.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = "Session graphic icon",
                            tint = if (isSelected) ShadowNeonCyan else ShadowTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = session.title,
                            color = if (isSelected) ShadowNeonCyan else ShadowTextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        IconButton(
                            onClick = { onDeleteSession(session.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete render session",
                                tint = ShadowTextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = ShadowNeonCyan.copy(alpha = 0.15f), modifier = Modifier.padding(16.dp))

        // Engine Parameters Selection in Sidebar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "ENGINE RATIO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ShadowNeonCyan,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val ratios = listOf("1:1", "16:9", "9:16", "4:3")
                ratios.forEach { ratio ->
                    val isSelected = selectedRatio == ratio
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ShadowNeonCyan else ShadowCard)
                            .border(
                                1.dp,
                                if (isSelected) ShadowNeonCyan else ShadowNeonCyan.copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onRatioChanged(ratio) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ratio,
                            fontSize = 11.sp,
                            color = if (isSelected) ShadowBlack else ShadowTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "RENDER QUALITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ShadowNeonPurple,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val sizes = listOf("512", "1K", "2K")
                sizes.forEach { size ->
                    val isSelected = selectedSize == size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) ShadowNeonPurple else ShadowCard)
                            .border(
                                1.dp,
                                if (isSelected) ShadowNeonPurple else ShadowNeonPurple.copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSizeChanged(size) }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size,
                            fontSize = 11.sp,
                            color = if (isSelected) ShadowBlack else ShadowTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatPane(
    modifier: Modifier,
    sessions: List<ChatSession>,
    selectedSessionId: Long?,
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    currentPrompt: String,
    selectedRatio: String,
    selectedSize: String,
    isExpandedLayout: Boolean,
    onPromptChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMenuClicked: () -> Unit,
    onNewSessionMobile: () -> Unit,
    onRatioChanged: (String) -> Unit,
    onSizeChanged: (String) -> Unit,
    onImageClicked: (ChatMessage) -> Unit
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = ShadowDarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ShadowBlack)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isExpandedLayout) {
                    IconButton(onClick = onMenuClicked) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Collapse menu icon",
                            tint = ShadowNeonCyan
                        )
                    }
                    Text(
                        text = "SHADOW AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ShadowTextPrimary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Banana 2 Image Engine // Active Session",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = ShadowNeonCyan.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (!isExpandedLayout) {
                    IconButton(onClick = onNewSessionMobile) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Session Mobile",
                            tint = ShadowNeonCyan
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        IconLabelIndicator(icon = Icons.Default.Grid3x3, text = "Model: Gemini-3.1-Flash-Image")
                        IconLabelIndicator(icon = Icons.Default.Wallpaper, text = "Aspect: $selectedRatio")
                        IconLabelIndicator(icon = Icons.Default.BrushIcon, text = "Size: $selectedSize")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isGenerating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ShadowNeonCyan,
                    trackColor = ShadowBlack
                )
            } else {
                HorizontalDivider(color = ShadowNeonCyan.copy(alpha = 0.1f))
            }

            // Message list or Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyStateConsole(
                        onSelectPreset = { presetPrompt ->
                            onPromptChanged(presetPrompt)
                        },
                        isExpandedLayout = isExpandedLayout
                    )
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(10.dp)) }

                        items(messages, key = { it.id }) { message ->
                            ChatBubble(
                                message = message,
                                onImageClicked = { onImageClicked(message) }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }
                }
            }

            // Control & Input Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ShadowBlack)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // If on compact screen, show dynamic parameters above input row
                if (!isExpandedLayout) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Aspect Ratio Toggles
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val ratios = listOf("1:1", "16:9", "9:16")
                            ratios.forEach { ratio ->
                                val isSel = selectedRatio == ratio
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) ShadowNeonCyan else ShadowCard)
                                        .clickable { onRatioChanged(ratio) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = ratio,
                                        fontSize = 10.sp,
                                        color = if (isSel) ShadowBlack else ShadowTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Quick Resolution Toggles
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val sizes = listOf("512", "1K")
                            sizes.forEach { size ->
                                val isSel = selectedSize == size
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSel) ShadowNeonPurple else ShadowCard)
                                        .clickable { onSizeChanged(size) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = size,
                                        fontSize = 10.sp,
                                        color = if (isSel) ShadowBlack else ShadowTextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Text Input block
                val keyboardController = LocalSoftwareKeyboardController.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentPrompt,
                        onValueChange = onPromptChanged,
                        placeholder = {
                            Text(
                                text = "Describe your visual imagination here...",
                                color = ShadowTextSecondary.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("prompt_text_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ShadowCard,
                            unfocusedContainerColor = ShadowCard,
                            focusedBorderColor = ShadowNeonCyan,
                            unfocusedBorderColor = ShadowNeonCyan.copy(alpha = 0.15f),
                            focusedTextColor = ShadowTextPrimary,
                            unfocusedTextColor = ShadowTextPrimary,
                            cursorColor = ShadowNeonCyan
                        ),
                        singleLine = false,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                onSend()
                                keyboardController?.hide()
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = {
                            onSend()
                            keyboardController?.hide()
                        },
                        enabled = currentPrompt.trim().isNotEmpty() && !isGenerating,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentPrompt.trim().isNotEmpty() && !isGenerating) {
                                    Brush.linearGradient(listOf(ShadowNeonCyan, ShadowNeonPurple))
                                } else {
                                    Brush.linearGradient(listOf(ShadowCard, ShadowCard))
                                }
                            )
                            .testTag("send_prompt_button"),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = ShadowBlack,
                            disabledContentColor = ShadowTextDisabled
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Send,
                            contentDescription = "Generate Prompt Render Trigger"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IconLabelIndicator(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = ShadowNeonCyan.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 10.sp, color = ShadowTextSecondary, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onImageClicked: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val isUser = message.sender == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .wrapContentHeight()
        ) {
            // Text card
            Card(
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isUser) 12.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 12.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) ShadowCardSelected else ShadowCard
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) ShadowNeonPurple.copy(alpha = 0.3f) else ShadowNeonCyan.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isUser) "PROMPT // CREATOR" else "SHADOW AI // RENDER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) ShadowNeonPurple else ShadowNeonCyan,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = message.text,
                        color = ShadowTextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // Optional parameters info inside bubble
                    if (message.aspectRatio != null && message.aspectRatio.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Canvas Modality: ${message.aspectRatio}",
                            fontSize = 8.sp,
                            color = ShadowTextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Generated Image Card
            if (message.imagePath != null) {
                Spacer(modifier = Modifier.height(8.dp))
                val bitmap = remember(message.imagePath) {
                    try {
                        BitmapFactory.decodeFile(message.imagePath)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (bitmap != null) {
                    val ratioValue = when (message.aspectRatio) {
                        "16:9" -> 16f / 9f
                        "9:16" -> 9f / 16f
                        "4:3" -> 4f / 3f
                        "3:4" -> 3f / 4f
                        else -> 1f
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(ratioValue)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                ShadowNeonCyan.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .background(ShadowBlack)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = message.text,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onImageClicked() },
                            contentScale = ContentScale.Crop
                        )

                        // Top right floating action overlays
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Fullscreen lightbox button
                            CircularIconButton(
                                icon = Icons.Default.Fullscreen,
                                description = "Maximize generated canvas",
                                onClick = onImageClicked
                            )
                            // Native share action
                            CircularIconButton(
                                icon = Icons.Default.Share,
                                description = "Share generated art",
                                onClick = {
                                    shareImageLocal(context, message.imagePath, message.text)
                                }
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ShadowCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Image resource loading error.",
                            color = ShadowNeonPink,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = ShadowNeonCyan,
            modifier = Modifier.size(16.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyStateConsole(
    onSelectPreset: (String) -> Unit,
    isExpandedLayout: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Futuristic Glowing Badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ShadowNeonCyan.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        radius = size.width * 0.9f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrushIcon,
                contentDescription = "Cosmic spark logo",
                modifier = Modifier.size(36.dp),
                tint = ShadowNeonCyan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SHADOW IMAGINATION ENGINE",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = ShadowTextPrimary,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Using Google Nano Banana 2 (Gemini-3.1-Flash-Image) for super resolution high-fidelity direct AI generations.",
            fontSize = 12.sp,
            color = ShadowTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 440.dp),
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "PROMPT SEED PATTERNS // TAP TO INSERT",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = ShadowNeonPurple,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val prompts = listOf(
            "Retro-futuristic cybernetic motorcycle driving under rain-slicked neon highways.",
            "Minimalist glowing teal bonsai tree floating on a meteorite in crystal space.",
            "Ancient mystic stone portal sending a glowing emerald cosmic beam into starry violet skies.",
            "Cyber-mesh shadow fox made of dark light with neon pink cybernetic glowing eyes."
        )

        FlowRow(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = if (isExpandedLayout) 2 else 1
        ) {
            prompts.forEach { item ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectPreset(item) },
                    colors = CardDefaults.cardColors(
                        containerColor = ShadowCard
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ShadowNeonCyan.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrushIcon,
                            contentDescription = null,
                            tint = ShadowNeonPurple,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            color = ShadowTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LightboxOverlay(
    message: ChatMessage,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val bitmap = remember(message.imagePath) {
        try {
            BitmapFactory.decodeFile(message.imagePath)
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap == null) {
        onClose()
        return
    }

    // Zooming state using simple transformable modifier
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 5f)
        offset += offsetChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable { onClose() }
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Large canvas image mapping (clipping prevented via graphicsLayer transformable control deck)
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Full render",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    when (message.aspectRatio) {
                        "16:9" -> 16f / 9f
                        "9:16" -> 9f / 16f
                        "4:3" -> 4f / 3f
                        "3:4" -> 3f / 4f
                        else -> 1f
                    }
                )
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformState)
                .clickable(enabled = false) {}, // prevent closing lightbox on pure image click
            contentScale = ContentScale.Fit
        )

        // Close action top right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Share image
            CircularIconButton(
                icon = Icons.Default.Share,
                description = "Export",
                onClick = {
                    shareImageLocal(context, message.imagePath, message.text)
                }
            )

            // Copy seed prompt
            CircularIconButton(
                icon = Icons.Default.ContentCopy,
                description = "Copy Seed Prompt",
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.text))
                    Toast.makeText(context, "Seed prompt copied to system clipboard!", Toast.LENGTH_SHORT).show()
                }
            )

            // Close Fullcreen
            CircularIconButton(
                icon = Icons.Default.Close,
                description = "Close overlay",
                onClick = onClose
            )
        }

        // Active prompt description at the bottom
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ShadowCard.copy(alpha = 0.85f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShadowNeonCyan.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "SEED PROMPT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ShadowNeonCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    fontSize = 12.sp,
                    color = ShadowTextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Hint: Use two fingers to pinch or drag to zoom/pan active canvas details.",
                    fontSize = 9.sp,
                    color = ShadowTextSecondary.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun shareImageLocal(context: Context, path: String?, promptText: String) {
    if (path == null) return
    val file = File(path)
    if (!file.exists()) {
        Toast.makeText(context, "Saved artifact does not exist on disk.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, "Check out this visual artifact I generated on Shadow AI prompt: '$promptText'")
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Rendered Artifact"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
