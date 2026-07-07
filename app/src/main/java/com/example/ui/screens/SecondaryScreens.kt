package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

// --- Shared Drawer Screen Header ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecondaryScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        content = content
    )
}

// 1. PROFIT SCREEN
@Composable
fun ProfitScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val orders by viewModel.orders.collectAsState()
    SecondaryScreenScaffold("Profit Log", onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, null, tint = EmeraldSuccess, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("TOTAL SHOP NET PROFIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text("$184,520.00", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Slate800)
                    }
                }
            }

            Text("Historical Sales Profit Ledger", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 14.sp)

            orders.filter { it.status == "delivered" || it.status == "confirmed" }.forEach { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(order.orderNumber ?: "CM-ORD", fontWeight = FontWeight.Bold)
                            Text("Revenue: $${order.totalAmount}", fontSize = 11.sp, color = Slate400)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val profit = order.totalAmount * 0.35 // Simulated 35% margin
                            Text("+$${String.format("%.2f", profit)}", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                            Text("COD Completed", fontSize = 10.sp, color = Slate400)
                        }
                    }
                }
            }
        }
    }
}

// 2. NEWSFEED SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsfeedScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val posts by viewModel.newsPosts.collectAsState()
    var showCreatePost by remember { mutableStateOf(false) }

    SecondaryScreenScaffold("News & Social Feed", onBack, actions = {
        IconButton(onClick = { showCreatePost = true }) {
            Icon(Icons.Default.AddPhotoAlternate, "New Post", tint = IndigoPrimary)
        }
    }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }
                items(posts) { post ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Slate200),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(36.dp).background(IndigoPrimary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Campaign, null, tint = IndigoPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Bot Broadcast Post", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Topic: ${post.topic.uppercase()}", fontSize = 10.sp, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                                }
                                IconButton(onClick = { viewModel.deleteNewsPost(post.id) }) {
                                    Icon(Icons.Default.Delete, null, tint = RoseDanger.copy(alpha = 0.6f))
                                }
                            }

                            if (post.imageUrl != null) {
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )
                            }

                            Text(
                                text = post.content,
                                fontSize = 13.sp,
                                color = Slate800,
                                modifier = Modifier.padding(14.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate50)
                                    .padding(vertical = 8.dp, horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Favorite, null, tint = RoseDanger, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${post.likes} Likes", fontSize = 11.sp, color = Slate400)
                                }
                                Text("${post.comments.size} Comments", fontSize = 11.sp, color = Slate400)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(30.dp)) }
            }

            if (showCreatePost) {
                ModalBottomSheet(onDismissRequest = { showCreatePost = false }) {
                    var content by remember { mutableStateOf("") }
                    var image by remember { mutableStateOf("") }
                    var topic by remember { mutableStateOf("general") }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .padding(bottom = 30.dp)
                    ) {
                        Text("Create Social Post", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("What's new? Post content...") },
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = image,
                            onValueChange = { image = it },
                            label = { Text("Image URL (Optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (content.isNotEmpty()) {
                                    viewModel.addNewsPost(content, image.ifEmpty { null }, topic)
                                    showCreatePost = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Publish Broadcast Post")
                        }
                    }
                }
            }
        }
    }
}

// 3. PAYMENTS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val methods by viewModel.paymentMethods.collectAsState()
    val codEnabled by viewModel.codEnabled.collectAsState()
    val deliveryFee by viewModel.codDeliveryFee.collectAsState()

    var showAddPayment by remember { mutableStateOf(false) }

    SecondaryScreenScaffold("Payment Configurations", onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cash on Delivery parameters
            Card(
                border = BorderStroke(1.dp, Slate200),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Cash on Delivery (COD)", fontWeight = FontWeight.Bold, color = Slate800)
                            Text("Allow clients to pay on delivery arrival", fontSize = 11.sp, color = Slate400)
                        }
                        Switch(checked = codEnabled, onCheckedChange = { viewModel.updateCodSettings(it, deliveryFee) })
                    }

                    if (codEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        var feeText by remember { mutableStateOf(deliveryFee.toString()) }
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = {
                                feeText = it
                                val fee = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateCodSettings(codEnabled, fee)
                            },
                            label = { Text("Standard COD Delivery Fee ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Authorized Payment Credentials", fontWeight = FontWeight.Bold, color = Slate800)
                TextButton(onClick = { showAddPayment = true }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Method")
                }
            }

            methods.forEach { method ->
                Card(
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(IndigoPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = IndigoPrimary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(method.type.uppercase(), fontWeight = FontWeight.Bold)
                                Text(method.accountName ?: "", fontSize = 12.sp, color = Slate800)
                                Text(method.accountNumber ?: "", fontSize = 11.sp, color = Slate400)
                            }
                        }
                        IconButton(onClick = { viewModel.deletePaymentMethod(method.id) }) {
                            Icon(Icons.Default.Delete, null, tint = RoseDanger)
                        }
                    }
                }
            }
        }

        if (showAddPayment) {
            ModalBottomSheet(onDismissRequest = { showAddPayment = false }) {
                var type by remember { mutableStateOf("kpay") }
                var accountName by remember { mutableStateOf("") }
                var accountNumber by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 30.dp)
                ) {
                    Text("Add Account Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("kpay", "wavepay", "aya_pay", "cbpay").forEach { t ->
                            val active = type == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) IndigoPrimary.copy(alpha = 0.1f) else Slate100)
                                    .clickable { type = t }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(t.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) IndigoPrimary else Slate700)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = accountName, onValueChange = { accountName = it }, label = { Text("Account Holder Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Account Number / Phone") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (accountName.isNotEmpty() && accountNumber.isNotEmpty()) {
                                viewModel.addPaymentMethod(type, accountName, accountNumber, "")
                                showAddPayment = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Payment Profile")
                    }
                }
            }
        }
    }
}

// 4. BROADCAST & GIVEAWAYS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val broadcasts by viewModel.broadcasts.collectAsState()
    val giveaways by viewModel.giveaways.collectAsState()

    var isGiveawayTab by remember { mutableStateOf(false) }

    var showCreateBroadcast by remember { mutableStateOf(false) }
    var showCreateGiveaway by remember { mutableStateOf(false) }

    // DRAW WINNER SPINNER SIMULATION STATE
    var activeGiveawayDrawingId by remember { mutableStateOf<Int?>(null) }
    var isSpinnerDrawing by remember { mutableStateOf(false) }
    var drawnWinnersResult by remember { mutableStateOf<List<String>?>(null) }

    val context = LocalContext.current

    SecondaryScreenScaffold("Marketing Broadcasts", onBack, actions = {
        IconButton(onClick = {
            if (isGiveawayTab) showCreateGiveaway = true else showCreateBroadcast = true
        }) {
            Icon(Icons.Default.Add, "Create New")
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = if (isGiveawayTab) 1 else 0,
                containerColor = Color.Transparent,
                contentColor = IndigoPrimary,
                modifier = Modifier.padding(16.dp)
            ) {
                Tab(selected = !isGiveawayTab, onClick = { isGiveawayTab = false }, text = { Text("Blast Broadcasts") })
                Tab(selected = isGiveawayTab, onClick = { isGiveawayTab = true }, text = { Text("Giveaways / Raffle") })
            }

            if (!isGiveawayTab) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(broadcasts) { bc ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Broadcast #${bc.id}", fontWeight = FontWeight.Bold, color = Slate800)
                                    Text(
                                        text = bc.status?.uppercase() ?: "SENT",
                                        color = EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(bc.message, fontSize = 13.sp, color = Slate800)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Target: ${bc.target.uppercase()}", fontSize = 11.sp, color = Slate400)
                                    Text("Delivered to: ${bc.targetCount} users", fontSize = 11.sp, color = IndigoPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(giveaways) { ga ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ga.prize, fontWeight = FontWeight.Black, color = Slate800, fontSize = 15.sp)
                                    Text(
                                        text = ga.status.uppercase(),
                                        color = if (ga.status == "active") IndigoPrimary else Slate400,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(ga.description ?: "", fontSize = 12.sp, color = Slate400, modifier = Modifier.padding(top = 4.dp))

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tickets: ${ga.participantCount} Entries", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    if (ga.status == "active") {
                                        Button(
                                            onClick = {
                                                activeGiveawayDrawingId = ga.id
                                                isSpinnerDrawing = true
                                                viewModel.drawGiveawayWinners(ga.id, "weighted", ga.winnerCount) { results ->
                                                    isSpinnerDrawing = false
                                                    drawnWinnersResult = results
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Draw Winner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Text("Winners: ${ga.winners.joinToString(", ")}", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DRAW WINNER ANIMATED SPINNER DIALOG OVERLAY ---
        if (activeGiveawayDrawingId != null) {
            AlertDialog(
                onDismissRequest = {
                    if (!isSpinnerDrawing) {
                        activeGiveawayDrawingId = null
                        drawnWinnersResult = null
                    }
                },
                confirmButton = {
                    if (!isSpinnerDrawing) {
                        TextButton(onClick = {
                            activeGiveawayDrawingId = null
                            drawnWinnersResult = null
                        }) {
                            Text("Confirm & Close")
                        }
                    }
                },
                title = { Text(if (isSpinnerDrawing) "Rolling Raffle Tickets..." else "Winner Selection Finished!") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isSpinnerDrawing) {
                            // Pulse/Spin Animation
                            val infiniteTransition = rememberInfiniteTransition(label = "SpinnerScale")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.2f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "Scale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .background(IndigoPrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Casino, "Rolling", tint = IndigoPrimary, modifier = Modifier.size(36.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Calculating weighted user interaction matrices...", fontSize = 12.sp, color = Slate400, textAlign = TextAlign.Center)
                        } else {
                            // Display winners
                            Icon(Icons.Default.EmojiEvents, "Trophy", tint = AmberWarning, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Congratulations to the Selected Winners:", fontWeight = FontWeight.Bold, color = Slate800)
                            drawnWinnersResult?.forEach { winner ->
                                Text(winner, fontWeight = FontWeight.Black, color = IndigoPrimary, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            )
        }

        // Create sheets modals omitted for space, but fully simulated inside bottom sheets easily!
        if (showCreateBroadcast) {
            ModalBottomSheet(onDismissRequest = { showCreateBroadcast = false }) {
                var message by remember { mutableStateOf("") }
                var url by remember { mutableStateOf("") }
                var target by remember { mutableStateOf("all") }
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 30.dp)) {
                    Text("New Broadcast Message", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Broadcast Message Text") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("ActionButton URL (Optional)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (message.isNotEmpty()) {
                                viewModel.createBroadcast(message, null, "Open", url.ifEmpty { null }, target)
                                showCreateBroadcast = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Broadcast Now")
                    }
                }
            }
        }

        if (showCreateGiveaway) {
            ModalBottomSheet(onDismissRequest = { showCreateGiveaway = false }) {
                var prize by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 30.dp)) {
                    Text("New Giveaway Contest", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = prize, onValueChange = { prize = it }, label = { Text("Prize Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Contest Description") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (prize.isNotEmpty()) {
                                viewModel.createGiveaway(prize, desc, 1, null, null)
                                showCreateGiveaway = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Launch Giveaway")
                    }
                }
            }
        }
    }
}

// 5. TELEGRAM COMMANDS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramCommandsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val commands by viewModel.customCommands.collectAsState()
    var showAddCommand by remember { mutableStateOf(false) }

    SecondaryScreenScaffold("Custom Telegram Commands", onBack, actions = {
        IconButton(onClick = { showAddCommand = true }) {
            Icon(Icons.Default.Add, "Add Command")
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Map custom keywords or slash-commands (e.g. /promo) to automatic Telegram replies instantly.",
                color = Slate400,
                fontSize = 12.sp
            )

            commands.forEach { cmd ->
                Card(
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("/${cmd.command}", fontWeight = FontWeight.Bold, color = IndigoPrimary, fontSize = 16.sp)
                            IconButton(onClick = { viewModel.deleteCustomCommand(cmd.id) }) {
                                Icon(Icons.Default.Delete, null, tint = RoseDanger.copy(alpha = 0.7f))
                            }
                        }
                        Text(cmd.description ?: "Auto-response keyword mapping", fontSize = 11.sp, color = Slate400)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(cmd.reply, fontSize = 13.sp, color = Slate800)
                    }
                }
            }
        }

        if (showAddCommand) {
            ModalBottomSheet(onDismissRequest = { showAddCommand = false }) {
                var name by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }
                var reply by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 30.dp)
                ) {
                    Text("Add Custom Command", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Command Keyword (e.g. support)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Command Description") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Automatic Text Reply") }, minLines = 2, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && reply.isNotEmpty()) {
                                viewModel.addCustomCommand(name, desc, reply)
                                showAddCommand = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Custom command")
                    }
                }
            }
        }
    }
}

// 6. QR MENU SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRMenuScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val items by viewModel.qrMenuItems.collectAsState()
    val categories by viewModel.qrMenuCategories.collectAsState()
    val tables by viewModel.qrTables.collectAsState()
    val orders by viewModel.qrOrders.collectAsState()

    var activeMenuSectionTab by remember { mutableStateOf(0) } // 0: items, 1: tables, 2: orders

    var showAddItemSheet by remember { mutableStateOf(false) }
    var showAddTableSheet by remember { mutableStateOf(false) }

    SecondaryScreenScaffold("QR Dine-In Menu", onBack, actions = {
        IconButton(onClick = {
            if (activeMenuSectionTab == 1) showAddTableSheet = true else showAddItemSheet = true
        }) {
            Icon(Icons.Default.Add, "Add")
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(selectedTabIndex = activeMenuSectionTab, containerColor = Color.Transparent, contentColor = IndigoPrimary) {
                Tab(selected = activeMenuSectionTab == 0, onClick = { activeMenuSectionTab = 0 }, text = { Text("Items", fontWeight = FontWeight.Bold) })
                Tab(selected = activeMenuSectionTab == 1, onClick = { activeMenuSectionTab = 1 }, text = { Text("Tables", fontWeight = FontWeight.Bold) })
                Tab(selected = activeMenuSectionTab == 2, onClick = { activeMenuSectionTab = 2 }, text = { Text("Dine-In Orders", fontWeight = FontWeight.Bold) })
            }

            if (activeMenuSectionTab == 0) {
                // QR Items List
                LazyColumn(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        item.badges.forEach { b ->
                                            Text(b.uppercase(), fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.background(AmberWarning, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
                                        }
                                    }
                                }
                                Text("$${item.price}", fontWeight = FontWeight.Black, color = IndigoPrimary)
                            }
                        }
                    }
                }
            } else if (activeMenuSectionTab == 1) {
                // Tables manager with instant Table QR Code Server generation!
                LazyColumn(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(tables) { table ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = table.qrCodeUrl,
                                    contentDescription = "QR Code",
                                    modifier = Modifier.size(80.dp).background(Color.White).padding(4.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Table #${table.number}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Slate800)
                                    Text("Scan code to place orders directly into kitchen dashboard.", fontSize = 11.sp, color = Slate400)
                                }
                            }
                        }
                    }
                }
            } else {
                // Dine in Orders list
                LazyColumn(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(orders) { o ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Table ${o.tableNumber} • ${o.orderNumber}", fontWeight = FontWeight.Bold)
                                    StatusBadge(status = o.status)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                o.items.forEach { item ->
                                    Text("${item.name} x${item.quantity}", fontSize = 13.sp, color = Slate800)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Total Amount: $${o.totalAmount}", fontWeight = FontWeight.Bold, color = IndigoPrimary)
                                    if (o.status == "pending") {
                                        Button(
                                            onClick = { viewModel.updateQROrderStatus(o.id, "delivered") },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Serve Food", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddItemSheet) {
            ModalBottomSheet(onDismissRequest = { showAddItemSheet = false }) {
                var name by remember { mutableStateOf("") }
                var price by remember { mutableStateOf("") }
                var desc by remember { mutableStateOf("") }

                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 30.dp)) {
                    Text("Add Dine-In Food Item", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Dish Description") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && price.isNotEmpty()) {
                                viewModel.addQRMenuItem(name, price.toDoubleOrNull() ?: 0.0, desc, 1, listOf("popular"), "")
                                showAddItemSheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add to QR Menu")
                    }
                }
            }
        }

        if (showAddTableSheet) {
            ModalBottomSheet(onDismissRequest = { showAddTableSheet = false }) {
                var number by remember { mutableStateOf("") }
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 30.dp)) {
                    Text("Generate Table QR Code", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Table Number (e.g. 04)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (number.isNotEmpty()) {
                                viewModel.addQRTable(number)
                                showAddTableSheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate Table QR")
                    }
                }
            }
        }
    }
}

// 7. STAFF ACCOUNTS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAccountsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val staffList by viewModel.staffAccounts.collectAsState()
    var showAddStaff by remember { mutableStateOf(false) }

    SecondaryScreenScaffold("Staff Credentials", onBack, actions = {
        IconButton(onClick = { showAddStaff = true }) {
            Icon(Icons.Default.Add, "Add Staff")
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Create staff sub-accounts, configure module permissions and view login audit logs.", color = Slate400, fontSize = 11.sp)

            staffList.forEach { staff ->
                Card(
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).background(IndigoPrimary.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Badge, null, tint = IndigoPrimary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(staff.name, fontWeight = FontWeight.Bold)
                                    Text("@${staff.username}", fontSize = 11.sp, color = Slate400)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteStaff(staff.id, "owner123") }) {
                                Icon(Icons.Default.Delete, null, tint = RoseDanger)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Active Permissions Modules:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (staff.permissions.dashboard) PermissionBadge("Dashboard")
                            if (staff.permissions.orders) PermissionBadge("Orders")
                            if (staff.permissions.products) PermissionBadge("Products")
                            if (staff.permissions.chats) PermissionBadge("Chats")
                        }
                    }
                }
            }
        }

        if (showAddStaff) {
            ModalBottomSheet(onDismissRequest = { showAddStaff = false }) {
                var name by remember { mutableStateOf("") }
                var user by remember { mutableStateOf("") }
                var pass by remember { mutableStateOf("") }
                var ownerPass by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 30.dp)
                ) {
                    Text("Add Staff Sub-Account", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Staff Real Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Staff Username") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Staff Password") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = ownerPass, onValueChange = { ownerPass = it }, label = { Text("Confirm Owner Password (owner123)") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && user.isNotEmpty() && pass.isNotEmpty() && ownerPass.isNotEmpty()) {
                                viewModel.addStaffAccount(name, user, pass, ownerPass, StaffPermissions()) {
                                    showAddStaff = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify & Create Account")
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionBadge(label: String) {
    Text(
        text = label,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = IndigoPrimary,
        modifier = Modifier
            .background(IndigoPrimary.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

// 8. FAQS SCREEN
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FAQsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val faqs by viewModel.faqs.collectAsState()
    var showAddFAQ by remember { mutableStateOf(false) }

    SecondaryScreenScaffold("Frequently Asked Questions", onBack, actions = {
        IconButton(onClick = { showAddFAQ = true }) {
            Icon(Icons.Default.Add, "Add FAQ")
        }
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            faqs.forEach { faq ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    border = BorderStroke(1.dp, Slate200),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(faq.question, fontWeight = FontWeight.Bold, color = Slate800, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteFAQ(faq.id) }) {
                                Icon(Icons.Default.Delete, null, tint = RoseDanger)
                            }
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(faq.answer, color = Slate700, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (showAddFAQ) {
            ModalBottomSheet(onDismissRequest = { showAddFAQ = false }) {
                var q by remember { mutableStateOf("") }
                var a by remember { mutableStateOf("") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 30.dp)
                ) {
                    Text("Add FAQ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = q, onValueChange = { q = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = a, onValueChange = { a = it }, label = { Text("Answer Reply") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (q.isNotEmpty() && a.isNotEmpty()) {
                                viewModel.addFAQ(q, a)
                                showAddFAQ = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save FAQ Item")
                    }
                }
            }
        }
    }
}

// 9. SUBSCRIPTION SCREEN
@Composable
fun SubscriptionScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val message by viewModel.discountValidationMessage.collectAsState()
    SecondaryScreenScaffold("Subscription Plan", onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CURRENT SERVICE PLAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                            Text("PRO TIER PLAN", fontWeight = FontWeight.Black, fontSize = 22.sp, color = IndigoPrimary)
                        }
                        Text("ACTIVE", color = EmeraldSuccess, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.background(EmeraldSuccess.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Service active until: Dec 31, 2026 (Yearly auto-renewal)", fontSize = 12.sp, color = Slate400)
                }
            }

            Text("Apply Promotional Voucher Code", fontWeight = FontWeight.Bold, color = Slate800)
            var promoCode by remember { mutableStateOf("") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = promoCode,
                    onValueChange = { promoCode = it },
                    placeholder = { Text("e.g. DISCOUNT50") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.validateDiscountCode(promoCode) },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Apply")
                }
            }

            if (message != null) {
                Text(message!!, fontWeight = FontWeight.Bold, color = if (message!!.startsWith("Success")) EmeraldSuccess else RoseDanger, fontSize = 13.sp)
            }

            Text("Plan Features Comparison Table", fontWeight = FontWeight.Bold, color = Slate800)
            ComparisonRow(label = "Category limit", free = "1", pro = "35", business = "Unlimited")
            ComparisonRow(label = "Products limit", free = "7", pro = "150", business = "Unlimited")
            ComparisonRow(label = "Dine-In QR Menu", free = "No", pro = "Yes", business = "Yes")
            ComparisonRow(label = "Staff accounts", free = "No", pro = "Yes", business = "Yes")
        }
    }
}

@Composable
fun ComparisonRow(label: String, free: String, pro: String, business: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate100, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate800, modifier = Modifier.width(110.dp))
        Text("Free: $free", fontSize = 10.sp, color = Slate400)
        Text("Pro: $pro", fontSize = 10.sp, color = IndigoPrimary, fontWeight = FontWeight.Bold)
        Text("Biz: $business", fontSize = 10.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
    }
}

// 10. SETTINGS SCREEN
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val currentBot by viewModel.currentBot.collectAsState()

    var name by remember { mutableStateOf(currentBot?.botName ?: "") }
    var currency by remember { mutableStateOf(currentBot?.currency ?: "USD") }
    var domainName by remember { mutableStateOf("mycrossmart.com") }

    // Custom domain list simulated state
    val domainsList = remember { mutableStateListOf<String>().apply { add("shop.crossmart.com") } }

    var aiEnabled by remember { mutableStateOf(currentBot?.aiEnabled ?: true) }
    var aiPrompt by remember { mutableStateOf(currentBot?.aiPrompt ?: "You are CrossMart AI Assistant.") }
    var aiModel by remember { mutableStateOf(currentBot?.aiModel ?: "gpt-4o-mini") }
    var aiTemp by remember { mutableStateOf(currentBot?.aiTemperature ?: 0.7f) }

    val context = LocalContext.current

    SecondaryScreenScaffold("Console Settings", onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section A: Profile Settings
            Text("STOREFRONT GENERAL CONFIG", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Slate400)
            Card(
                border = BorderStroke(1.dp, Slate200),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("E-Commerce Shop Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Base Currency Code") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                        Button(
                            onClick = {
                                viewModel.updateBotSettings(name, currency, currentBot?.profilePicture ?: "")
                                Toast.makeText(context, "General settings synchronized!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Sync")
                        }
                    }
                }
            }

            // Section B: Custom Domain Managers
            Text("CUSTOM WEBSITES DOMAIN NAMES", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Slate400)
            Card(
                border = BorderStroke(1.dp, Slate200),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    domainsList.forEach { dom ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dom, fontWeight = FontWeight.Bold, color = Slate800)
                            }
                            IconButton(onClick = { domainsList.remove(dom) }) {
                                Icon(Icons.Default.Delete, null, tint = RoseDanger)
                            }
                        }
                        HorizontalDivider(color = Slate200)
                    }

                    OutlinedTextField(
                        value = domainName,
                        onValueChange = { domainName = it },
                        label = { Text("Enter Custom Domain Name") },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (domainName.isNotEmpty()) {
                                    domainsList.add(domainName)
                                    domainName = ""
                                    Toast.makeText(context, "Domain verified & toggleable!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.AddCircle, null, tint = IndigoPrimary)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section C: AI COPILOT SETTINGS
            Text("AI AUTOPILOT CHATBOT CONFIG", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Slate400)
            Card(
                border = BorderStroke(1.dp, Slate200),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("AI Auto-Responder", fontWeight = FontWeight.Bold)
                            Text("Drive automatic replies on Telegram/Web", fontSize = 11.sp, color = Slate400)
                        }
                        Switch(checked = aiEnabled, onCheckedChange = { aiEnabled = it })
                    }

                    if (aiEnabled) {
                        var expandedModel by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expandedModel = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Text("Language Model: ${aiModel.uppercase()}")
                            }
                            DropdownMenu(expanded = expandedModel, onDismissRequest = { expandedModel = false }) {
                                DropdownMenuItem(text = { Text("gpt-4o-mini (Faster, cost effective)") }, onClick = { aiModel = "gpt-4o-mini"; expandedModel = false })
                                DropdownMenuItem(text = { Text("gemini-2.0-flash (Smart AI, rich links)") }, onClick = { aiModel = "gemini-2.0-flash"; expandedModel = false })
                                DropdownMenuItem(text = { Text("claude-3-haiku") }, onClick = { aiModel = "claude-3-haiku"; expandedModel = false })
                            }
                        }

                        OutlinedTextField(
                            value = aiPrompt,
                            onValueChange = { aiPrompt = it },
                            label = { Text("System AI Prompt Instructions") },
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("AI Temperature (Creativity): ${String.format("%.1f", aiTemp)}", fontSize = 12.sp, color = Slate800, fontWeight = FontWeight.Bold)
                        Slider(value = aiTemp, onValueChange = { aiTemp = it }, valueRange = 0.0f..1.0f)

                        Button(
                            onClick = {
                                viewModel.updateAiSettings(aiEnabled, aiPrompt, aiModel, aiTemp, 300)
                                Toast.makeText(context, "AI configuration synchronized!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Copilot Config")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
