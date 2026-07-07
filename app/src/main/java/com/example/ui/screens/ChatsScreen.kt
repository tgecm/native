package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Chat
import com.example.data.model.Message
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val messages by viewModel.messages.collectAsState()

    var isTelegramChats by remember { mutableStateOf(true) }
    var activeChatSessionId by remember { mutableStateOf<Int?>(null) } // Opened chat userId

    // Attachments dialog trigger
    var showAttachmentSheet by remember { mutableStateOf(false) }

    // AI Autopilot status mapping (locally tracked for simulation!)
    val aiAutopilotMap = remember { mutableStateMapOf<Int, Boolean>().apply { put(101, true) } }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (activeChatSessionId != null) {
                        val activeChat = chats.find { it.userId == activeChatSessionId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeChatSessionId = null }) {
                                Icon(Icons.Default.ArrowBack, "Back")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(activeChat?.firstName ?: "Support Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Text("Live Support Chats", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    if (activeChatSessionId == null) {
                        IconButton(onClick = onMenuClick, modifier = Modifier.testTag("drawer_menu_button")) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    if (activeChatSessionId != null) {
                        // AI Autopilot switch for active chat in toolbar
                        val activeId = activeChatSessionId!!
                        val autopilotEnabled = aiAutopilotMap[activeId] ?: false
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "AI Autopilot",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (autopilotEnabled) EmeraldSuccess else Slate400,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Switch(
                                checked = autopilotEnabled,
                                onCheckedChange = {
                                    aiAutopilotMap[activeId] = it
                                    Toast.makeText(
                                        context,
                                        if (it) "AI copilot takes the lead!" else "AI autopilot disabled",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldSuccess,
                                    checkedTrackColor = EmeraldSuccess.copy(alpha = 0.4f)
                                )
                            )
                        }
                    } else {
                        // Quick search chats or refresh
                        IconButton(onClick = { viewModel.refreshAllData() }) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = IndigoPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeChatSessionId == null) {
                // --- Tab List of Chats ---
                TabRow(
                    selectedTabIndex = if (isTelegramChats) 0 else 1,
                    containerColor = Color.Transparent,
                    contentColor = IndigoPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                ) {
                    Tab(selected = isTelegramChats, onClick = { isTelegramChats = true }, text = { Text("Telegram Chats", fontWeight = FontWeight.Bold) }, modifier = Modifier.testTag("telegram_chats_tab"))
                    Tab(selected = !isTelegramChats, onClick = { isTelegramChats = false }, text = { Text("Web Visitors", fontWeight = FontWeight.Bold) }, modifier = Modifier.testTag("web_visitors_tab"))
                }

                val displayedChats = remember(chats, isTelegramChats) {
                    chats.filter {
                        // In mock datasets we can group or toggle
                        if (isTelegramChats) it.username != null else it.username == null
                    }
                }

                if (displayedChats.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No support conversations active.", color = Slate400)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedChats) { chat ->
                            val autopilot = aiAutopilotMap[chat.userId] ?: false

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeChatSessionId = chat.userId
                                        viewModel.getMessagesForUser(chat.userId)
                                        viewModel.markChatRead(chat.userId)
                                    }
                                    .testTag("chat_card_${chat.userId}"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Avatar
                                    Box {
                                        AsyncImage(
                                            model = chat.photoUrl,
                                            contentDescription = chat.firstName,
                                            contentScale = ContentScale.Crop,
                                            error = rememberVectorPainter(Icons.Default.AccountCircle),
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Slate100)
                                        )

                                        if (autopilot) {
                                            // AI Active icon badge on avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(EmeraldSuccess, CircleShape)
                                                    .border(2.dp, Color.White, CircleShape)
                                                    .align(Alignment.BottomEnd),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Android, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(chat.firstName ?: "Customer support", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                            Text(
                                                text = "9:42 AM", // Mock timestamp
                                                fontSize = 10.sp,
                                                color = Slate400,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = chat.lastMessage ?: "No messages.",
                                            fontSize = 12.sp,
                                            color = if (chat.unreadCount > 0) Slate900 else Slate400,
                                            fontWeight = if (chat.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (chat.unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .background(IndigoPrimary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${chat.unreadCount}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- Active Conversation Thread View ---
                val activeId = activeChatSessionId!!
                val threadMessages = messages[activeId] ?: emptyList()
                val listState = rememberLazyListState()

                // Scroll to bottom when messages list size changes
                LaunchedEffect(threadMessages.size) {
                    if (threadMessages.isNotEmpty()) {
                        listState.animateScrollToItem(threadMessages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }

                    items(threadMessages) { msg ->
                        val isBot = msg.sender == "bot"
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isBot) Alignment.End else Alignment.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isBot) 16.dp else 4.dp,
                                            bottomEnd = if (isBot) 4.dp else 16.dp
                                        )
                                    )
                                    .background(if (isBot) IndigoPrimary else Slate100)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .widthIn(max = 280.dp)
                            ) {
                                Column {
                                    // Simulated File Attachment Display
                                    if (msg.fileId != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .padding(bottom = 6.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Slate200),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (msg.fileType == "photo") {
                                                AsyncImage(
                                                    model = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400",
                                                    contentDescription = "Attachment Image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.AttachFile, null, tint = Slate700)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Attachment: ${msg.fileType?.uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = msg.text ?: "",
                                        color = if (isBot) Color.White else Slate800,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                text = if (isBot) "Delivered • 9:42 AM" else "John • 9:41 AM",
                                color = Slate400,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // Chat Input box with Actions
                var textInput by remember { mutableStateOf("") }
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Clip Attachment Button
                        IconButton(onClick = { showAttachmentSheet = true }) {
                            Icon(Icons.Default.AddCircleOutline, "Attach", tint = IndigoPrimary)
                        }

                        // Message Text Field
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Reply to customer...") },
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IndigoPrimary,
                                unfocusedContainerColor = Slate50,
                                focusedContainerColor = Slate50
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send Arrow
                        IconButton(
                            onClick = {
                                if (textInput.isNotEmpty()) {
                                    viewModel.sendChatMessage(activeId, textInput)
                                    textInput = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = IndigoPrimary, contentColor = Color.White),
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("chat_send_button")
                        ) {
                            Icon(Icons.Default.Send, "Send", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // --- Custom Bottom Sheet Attachment Simulation Selector ---
        if (showAttachmentSheet) {
            val activeId = activeChatSessionId ?: 101
            ModalBottomSheet(
                onDismissRequest = { showAttachmentSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text("Attach Media File", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Slate800, modifier = Modifier.padding(bottom = 16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AttachmentTypeBtn(label = "Camera Photo", icon = Icons.Default.CameraAlt, modifier = Modifier.weight(1f)) {
                            viewModel.sendChatMessage(activeId, "Sent a camera snapshot.", "camera_proof_id", "photo")
                            showAttachmentSheet = false
                        }
                        AttachmentTypeBtn(label = "Gallery Photo", icon = Icons.Default.Image, modifier = Modifier.weight(1f)) {
                            viewModel.sendChatMessage(activeId, "Attached gallery picture.", "gallery_item_id", "photo")
                            showAttachmentSheet = false
                        }
                        AttachmentTypeBtn(label = "Document PDF", icon = Icons.Default.Description, modifier = Modifier.weight(1f)) {
                            viewModel.sendChatMessage(activeId, "Shared billing document.pdf", "doc_pdf_id", "document")
                            showAttachmentSheet = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentTypeBtn(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate100),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = IndigoPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
        }
    }
}
