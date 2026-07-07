package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.Customer
import com.example.data.model.Order
import com.example.data.model.WebsiteCustomer
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val tgUsers by viewModel.telegramUsers.collectAsState()
    val webCustomers by viewModel.websiteCustomers.collectAsState()
    val orders by viewModel.orders.collectAsState()

    var isTelegramTab by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("All") } // All, Banned, Active

    // Details Sheet Modal
    var selectedTGUserDetail by remember { mutableStateOf<Customer?>(null) }
    var selectedWebUserDetail by remember { mutableStateOf<WebsiteCustomer?>(null) }

    var showFilterMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Database", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.testTag("drawer_menu_button")) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }, modifier = Modifier.testTag("customers_filter")) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter", tint = IndigoPrimary)
                        }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            listOf("All", "Active", "Banned").forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter) },
                                    onClick = {
                                        statusFilter = filter
                                        showFilterMenu = false
                                    }
                                )
                            }
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
            // Horizontal Selection Switcher
            TabRow(
                selectedTabIndex = if (isTelegramTab) 0 else 1,
                containerColor = Color.Transparent,
                contentColor = IndigoPrimary,
                modifier = Modifier
                    .fillKeepPaddingRow()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
            ) {
                Tab(selected = isTelegramTab, onClick = { isTelegramTab = true }, text = { Text("Telegram Bot Users", fontWeight = FontWeight.Bold) }, modifier = Modifier.testTag("tg_customers_tab"))
                Tab(selected = !isTelegramTab, onClick = { isTelegramTab = false }, text = { Text("Website Clients", fontWeight = FontWeight.Bold) }, modifier = Modifier.testTag("web_customers_tab"))
            }

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customers by name, handle, email...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("customers_search_input")
            )

            // Database items rendering
            if (isTelegramTab) {
                // Render Telegram Users
                val filteredTG = remember(tgUsers, searchQuery, statusFilter) {
                    tgUsers.filter { user ->
                        val matchesStatus = when (statusFilter) {
                            "All" -> true
                            "Banned" -> user.banned
                            "Active" -> !user.banned
                            else -> true
                        }
                        val matchesSearch = if (searchQuery.isEmpty()) true else {
                            val name = user.firstName?.lowercase() ?: ""
                            val handle = user.username?.lowercase() ?: ""
                            val phone = user.phoneNumber?.lowercase() ?: ""
                            name.contains(searchQuery.lowercase()) || handle.contains(searchQuery.lowercase()) || phone.contains(searchQuery.lowercase())
                        }
                        matchesStatus && matchesSearch
                    }
                }

                if (filteredTG.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No Telegram users found.", color = Slate400)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTG) { user ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTGUserDetail = user },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // User Avatar fallback
                                    AsyncImage(
                                        model = user.photoUrl,
                                        contentDescription = user.firstName,
                                        contentScale = ContentScale.Crop,
                                        error = rememberVectorPainter(Icons.Default.AccountCircle),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Slate100)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.firstName ?: "No Name", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                        Text(user.username ?: "No Handle", fontSize = 11.sp, color = Slate400)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("$${user.totalSpent}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = IndigoPrimary)
                                        Text("${user.orderCount} orders", fontSize = 10.sp, color = Slate400)
                                    }

                                    if (user.banned) {
                                        Icon(Icons.Default.Block, "Banned", tint = RoseDanger, modifier = Modifier.padding(start = 8.dp).size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Render Website customers
                val filteredWeb = remember(webCustomers, searchQuery, statusFilter) {
                    webCustomers.filter { user ->
                        val matchesSearch = if (searchQuery.isEmpty()) true else {
                            val name = user.name?.lowercase() ?: ""
                            val email = user.email?.lowercase() ?: ""
                            val phone = user.phone?.lowercase() ?: ""
                            name.contains(searchQuery.lowercase()) || email.contains(searchQuery.lowercase()) || phone.contains(searchQuery.lowercase())
                        }
                        // Website users are currently active by default
                        matchesSearch
                    }
                }

                if (filteredWeb.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No website customers found.", color = Slate400)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredWeb) { user ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWebUserDetail = user },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Slate200)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = user.photoUrl,
                                        contentDescription = user.name,
                                        contentScale = ContentScale.Crop,
                                        error = rememberVectorPainter(Icons.Default.AccountCircle),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Slate100)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(user.name ?: "Unknown Guest", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                                        Text(user.email ?: "No Email", fontSize = 11.sp, color = Slate400, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("$${user.totalSpent}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = IndigoPrimary)
                                        Text("${user.totalOrders} orders", fontSize = 10.sp, color = Slate400)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Telegram Customer Details Bottom Sheet ---
        if (selectedTGUserDetail != null) {
            val user = selectedTGUserDetail!!
            val customerRecentOrders = orders.filter { it.userId == user.id }

            ModalBottomSheet(
                onDismissRequest = { selectedTGUserDetail = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                var notesText by remember { mutableStateOf(user.notes ?: "") }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {
                    // Avatar and Name Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = user.firstName,
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.AccountCircle),
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(user.firstName ?: "No Name", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate800)
                            Text(user.username ?: "No Telegram username", fontSize = 13.sp, color = Slate400)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Metrics row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileMetricItem(label = "Total Spent", value = "$${user.totalSpent}", modifier = Modifier.weight(1f))
                        ProfileMetricItem(label = "Orders Count", value = "${user.orderCount}", modifier = Modifier.weight(1f))
                        ProfileMetricItem(label = "Points Bal.", value = "${user.pointsBalance}", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Core metadata
                    ProfileField(label = "Telegram ID", value = "${user.telegramId ?: "N/A"}")
                    ProfileField(label = "Phone Number", value = user.phoneNumber ?: "N/A")
                    ProfileField(label = "Email Address", value = user.email ?: "N/A")
                    ProfileField(label = "Registered on", value = user.createdAt?.substringBefore("T") ?: "N/A")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Editable notes field
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Staff Admin Notes") },
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Recent Orders List Sub-Section
                    Text("RECENT ORDERS HISTORIC", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400, modifier = Modifier.padding(bottom = 8.dp))
                    if (customerRecentOrders.isEmpty()) {
                        Text("No orders placed yet.", color = Slate400, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        customerRecentOrders.forEach { order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(order.orderNumber ?: "CM-ORD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(order.createdAt.substringBefore("T"), fontSize = 10.sp, color = Slate400)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$${order.totalAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800, modifier = Modifier.padding(end = 8.dp))
                                    StatusBadge(status = order.status)
                                }
                            }
                            HorizontalDivider(color = Slate100)
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action buttons: Ban / Unban Toggle
                    val banBtnText = if (user.banned) "Unban Account" else "Ban Telegram User"
                    Button(
                        onClick = {
                            viewModel.toggleCustomerBan(user.id, isTelegram = true)
                            Toast.makeText(context, "Customer ban status modified!", Toast.LENGTH_SHORT).show()
                            selectedTGUserDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (user.banned) EmeraldSuccess else RoseDanger),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(imageVector = if (user.banned) Icons.Default.Check else Icons.Default.Block, contentDescription = "BanAction")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(banBtnText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Website Customer Details Bottom Sheet ---
        if (selectedWebUserDetail != null) {
            val user = selectedWebUserDetail!!
            val recentWebOrders = orders.filter { it.buyerSnapshot?.firebaseUid == user.firebaseUid }

            ModalBottomSheet(
                onDismissRequest = { selectedWebUserDetail = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = user.name,
                            contentScale = ContentScale.Crop,
                            error = rememberVectorPainter(Icons.Default.AccountCircle),
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(user.name ?: "Unknown Guest", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Slate800)
                            Text(user.email ?: "No Registered Email", fontSize = 13.sp, color = Slate400)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileMetricItem(label = "Total Spent", value = "$${user.totalSpent}", modifier = Modifier.weight(1f))
                        ProfileMetricItem(label = "Orders Count", value = "${user.totalOrders}", modifier = Modifier.weight(1f))
                        ProfileMetricItem(label = "Loyalty Points", value = "${user.pointsBalance}", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ProfileField(label = "Firebase UID", value = user.firebaseUid)
                    ProfileField(label = "Phone Number", value = user.phone ?: "N/A")
                    ProfileField(label = "Shipping Location", value = user.address ?: "N/A")
                    ProfileField(label = "Joined On", value = user.createdAt?.substringBefore("T") ?: "N/A")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("RECENT ORDERS HISTORIC", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400, modifier = Modifier.padding(bottom = 8.dp))
                    if (recentWebOrders.isEmpty()) {
                        Text("No orders placed yet.", color = Slate400, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        recentWebOrders.forEach { order ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(order.orderNumber ?: "CM-ORD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(order.createdAt.substringBefore("T"), fontSize = 10.sp, color = Slate400)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$${order.totalAmount}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800, modifier = Modifier.padding(end = 8.dp))
                                    StatusBadge(status = order.status)
                                }
                            }
                            HorizontalDivider(color = Slate100)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileMetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate100),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Slate800, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// Utility to fix padding issues in modern Compose layouts
fun Modifier.fillKeepPaddingRow(): Modifier = this.fillMaxWidth()
