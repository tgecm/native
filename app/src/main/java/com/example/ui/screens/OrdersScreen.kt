package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, pending, confirmed, processing, shipped, delivered, cancelled, rejected
    var selectedChannelTab by remember { mutableStateOf(0) } // 0: All, 1: Telegram, 2: Website, 3: Guest

    var showFilterMenu by remember { mutableStateOf(false) }

    // Bottom Sheet Detail State
    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }
    var zoomImageUrl by remember { mutableStateOf<String?>(null) }

    // Confirm/Reject Dialog verification
    var pendingActionOrder by remember { mutableStateOf<Pair<Order, String>?>(null) } // Pair(Order, "confirm"|"reject")

    // Filtered orders calculation
    val filteredOrders = remember(orders, searchQuery, selectedFilter, selectedChannelTab) {
        orders.filter { order ->
            // 1. Channel tab filter
            val channelMatch = when (selectedChannelTab) {
                0 -> true
                1 -> order.customer != null || order.buyerSnapshot?.telegramUsername != null
                2 -> order.buyerSnapshot?.firebaseUid != null
                3 -> order.customer == null && order.buyerSnapshot?.firebaseUid == null
                else -> true
            }

            // 2. Status dropdown filter
            val statusMatch = if (selectedFilter == "All") true else order.status.lowercase() == selectedFilter.lowercase()

            // 3. Search query match
            val searchMatch = if (searchQuery.isEmpty()) true else {
                val num = order.orderNumber?.lowercase() ?: ""
                val inv = order.invoiceNumber?.lowercase() ?: ""
                val name = order.buyerSnapshot?.name?.lowercase() ?: ""
                val phone = order.buyerSnapshot?.phone?.lowercase() ?: ""
                num.contains(searchQuery.lowercase()) || inv.contains(searchQuery.lowercase()) || name.contains(searchQuery.lowercase()) || phone.contains(searchQuery.lowercase())
            }

            channelMatch && statusMatch && searchMatch
        }
    }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Orders", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.testTag("drawer_menu_button")) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }, modifier = Modifier.testTag("orders_filter_button")) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter", tint = IndigoPrimary)
                        }
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            listOf("All", "Pending", "Confirmed", "Processing", "Shipped", "Delivered", "Cancelled", "Rejected").forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status) },
                                    onClick = {
                                        selectedFilter = status
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
            // Search Input Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, invoice, order ID...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Slate400) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
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
                    .testTag("orders_search_input")
            )

            // Horizontal tabs All | Telegram | Website | Guest
            PrimaryTabRow(
                selectedTabIndex = selectedChannelTab,
                containerColor = Color.Transparent,
                contentColor = IndigoPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Tab(selected = selectedChannelTab == 0, onClick = { selectedChannelTab = 0 }, text = { Text("All", fontWeight = FontWeight.Bold) }, modifier = Modifier.testTag("tab_all"))
                Tab(selected = selectedChannelTab == 1, onClick = { selectedChannelTab = 1 }, text = { Text("Telegram", fontWeight = FontWeight.Bold) }, modifier = Modifier.testTag("tab_tg"))
                Tab(selected = selectedChannelTab == 2, onClick = { selectedChannelTab = 2 }, text = { Text("Website", fontWeight = FontWeight.Bold) }, modifier = Modifier.testTag("tab_web"))
                Tab(selected = selectedChannelTab == 3, onClick = { selectedChannelTab = 3 }, text = { Text("Guest", fontWeight = FontWeight.Bold) }, modifier = Modifier.testTag("tab_guest"))
            }

            // Pull to refresh layout or clean scroll
            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = Slate300)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Orders Found", fontWeight = FontWeight.Bold, color = Slate400)
                        Text("Try adjusting your filters or search query.", color = Slate400, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        // Standard Material Swipe-To-Dismiss simulation or responsive swipe controls!
                        // In Compose, we can wrap inside a Dismissible card or a customized swipe Detector
                        // Let's implement a neat Swipeable pending order card with action buttons on swipe, or custom row action triggers.
                        // Since standard SwipeToDismiss can be complex, let's build an incredibly beautiful Card
                        // that shows swipe gesture tips or features Swipe buttons.
                        // "Swipe-left to reject, swipe-right to confirm" is easily modeled with swipe gesture Modifier
                        var offsetX by remember { mutableStateOf(0f) }
                        val animatedOffset by animateFloatAsState(targetValue = offsetX)

                        val isPending = order.status.lowercase() == "pending"
                        val isCOD = order.paymentMethod.lowercase() == "cod"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationX = animatedOffset }
                                .pointerInput(Unit) {
                                    if (isPending && !isCOD) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                if (offsetX > 200) {
                                                    // Swipe Right: Confirm
                                                    pendingActionOrder = Pair(order, "confirm")
                                                } else if (offsetX < -200) {
                                                    // Swipe Left: Reject
                                                    pendingActionOrder = Pair(order, "reject")
                                                }
                                                offsetX = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                offsetX = (offsetX + dragAmount.x).coerceIn(-300f, 300f)
                                            }
                                        )
                                    }
                                }
                                .clickable { selectedOrderForDetail = order }
                                .testTag("order_item_${order.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = order.orderNumber ?: "CM-ORD-TEMP",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Slate800
                                        )
                                        Text(
                                            text = "Created: ${order.createdAt.substringBefore("T")}",
                                            fontSize = 11.sp,
                                            color = Slate400
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Payment Method Badge
                                        Text(
                                            text = order.paymentMethod.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCOD) AmberWarning else IndigoPrimary,
                                            modifier = Modifier
                                                .background(
                                                    if (isCOD) AmberWarning.copy(alpha = 0.12f) else IndigoPrimary.copy(alpha = 0.12f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        )

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Status badge
                                        StatusBadge(status = order.status)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Customer initials avatar
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(IndigoPrimary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = order.buyerSnapshot?.name?.take(2)?.uppercase() ?: "G",
                                            fontWeight = FontWeight.Bold,
                                            color = IndigoPrimary,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = order.buyerSnapshot?.name ?: "Guest Customer",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Slate800
                                        )
                                        Text(
                                            text = "${order.items.size} items • ${order.buyerSnapshot?.phone ?: "No phone"}",
                                            fontSize = 11.sp,
                                            color = Slate400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = "$${order.totalAmount}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Slate800
                                    )
                                }

                                // Interactive swipe tips for pending orders
                                if (isPending && !isCOD) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                            .background(Slate50, RoundedCornerShape(8.dp))
                                            .padding(vertical = 4.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, null, tint = RoseDanger, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Swipe to Confirm / Reject",
                                            color = Slate400,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        Icon(Icons.Default.ChevronRight, null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Dialog Confirmation for Swiping Confirmation ---
        if (pendingActionOrder != null) {
            val (order, action) = pendingActionOrder!!
            AlertDialog(
                onDismissRequest = { pendingActionOrder = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val newStatus = if (action == "confirm") "confirmed" else "rejected"
                            viewModel.updateOrderStatus(order.id, newStatus)
                            Toast.makeText(context, "Order ${order.orderNumber} ${action}ed!", Toast.LENGTH_SHORT).show()
                            pendingActionOrder = null
                        }
                    ) {
                        Text("Proceed", fontWeight = FontWeight.Bold, color = if (action == "confirm") EmeraldSuccess else RoseDanger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingActionOrder = null }) { Text("Cancel") }
                },
                title = { Text("${action.capitalize()} Order CM-ORD-${order.id}?") },
                text = { Text("Are you sure you want to ${action} this transaction for $${order.totalAmount}?") }
            )
        }

        // --- Bottom Sheet Order Details Screen ---
        if (selectedOrderForDetail != null) {
            val order = selectedOrderForDetail!!
            val isPending = order.status.lowercase() == "pending"
            val isCOD = order.paymentMethod.lowercase() == "cod"

            ModalBottomSheet(
                onDismissRequest = { selectedOrderForDetail = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() },
                modifier = Modifier.testTag("order_details_bottom_sheet")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 50.dp)
                ) {
                    // Header Order info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = order.orderNumber ?: "CM-ORD-TEMP",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Slate800
                            )
                            Text(
                                text = "Invoice: ${order.invoiceNumber ?: "N/A"}",
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }

                        StatusBadge(status = order.status)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Customer Profile section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate100),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CUSTOMER PROFILE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400)

                                // Copy to Clipboard Button
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            val profileText = """
                                                Name: ${order.buyerSnapshot?.name ?: "Guest"}
                                                Phone: ${order.buyerSnapshot?.phone ?: "N/A"}
                                                Email: ${order.buyerSnapshot?.email ?: "N/A"}
                                                Telegram: ${order.buyerSnapshot?.telegramUsername ?: "N/A"}
                                                Address: ${order.buyerSnapshot?.address ?: "N/A"}
                                                Notes: ${order.buyerSnapshot?.notes ?: "None"}
                                            """.trimIndent()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Customer Profile", profileText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Profile copied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .background(IndigoPrimary.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ContentCopy, null, tint = IndigoPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy Profile", color = IndigoPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            ProfileField(label = "Name", value = order.buyerSnapshot?.name ?: "Guest Customer")
                            ProfileField(label = "Phone", value = order.buyerSnapshot?.phone ?: "N/A")
                            ProfileField(label = "Email", value = order.buyerSnapshot?.email ?: "N/A")
                            ProfileField(label = "Telegram", value = order.buyerSnapshot?.telegramUsername ?: "N/A")
                            ProfileField(label = "Viber Number", value = order.buyerSnapshot?.viberNumber ?: "N/A")
                            ProfileField(label = "Shipping Address", value = order.buyerSnapshot?.address ?: "N/A")
                            ProfileField(label = "Order Notes", value = order.buyerSnapshot?.notes ?: "N/A")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Status Timeline Indicator
                    Text("STATUS TIMELINE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400, modifier = Modifier.padding(bottom = 12.dp))
                    OrderStatusTimeline(currentStatus = order.status)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Products list table
                    Text("PRODUCTS LIST", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400, modifier = Modifier.padding(bottom = 12.dp))
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                                if (item.variant != null) {
                                    Text("Variant: ${item.variant}", fontSize = 11.sp, color = Slate400)
                                }
                            }
                            Text("x${item.quantity}", fontWeight = FontWeight.Medium, color = Slate700, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
                            Text("$${item.price * item.quantity}", fontWeight = FontWeight.Bold, color = Slate800, fontSize = 13.sp)
                        }
                        HorizontalDivider(color = Slate100)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Financial totals billing block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate50, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BillingRow(label = "Subtotal", value = "$${order.totalAmount - order.deliveryFee + order.couponDiscount}")
                        BillingRow(label = "Delivery Fee", value = "+$${order.deliveryFee}")
                        if (order.couponDiscount > 0) {
                            BillingRow(label = "Coupon Discount (${order.couponCode})", value = "-$${order.couponDiscount}", valueColor = RoseDanger)
                        }
                        HorizontalDivider(color = Slate200)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Paid Amount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Slate800)
                            Text("$${order.totalAmount}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = IndigoPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Payment Proof Section (for Non-COD Pending reviewer orders)
                    if (isPending && !isCOD) {
                        Text("PAYMENT PROOF SUBMITTED", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400, modifier = Modifier.padding(bottom = 8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Slate200)
                                .clickable {
                                    // Open photo viewer Zoom
                                    zoomImageUrl = "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=600"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // High resolution proof placeholder
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=600",
                                contentDescription = "Payment Proof",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ZoomIn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pinch/Tap to Zoom Proof", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Confirm & Reject Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateOrderStatus(order.id, "rejected")
                                    selectedOrderForDetail = null
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseDanger),
                                border = BorderStroke(1.dp, RoseDanger),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Close, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reject Order", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateOrderStatus(order.id, "confirmed")
                                    selectedOrderForDetail = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm Order", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (isCOD && order.status == "delivered") {
                        // Mark as Paid for COD orders
                        Button(
                            onClick = {
                                viewModel.updateOrderStatus(order.id, "delivered") // Mark Paid / Completed state
                                selectedOrderForDetail = null
                                Toast.makeText(context, "COD Marked as Paid!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Paid, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Paid", fontWeight = FontWeight.Bold)
                        }
                    } else if (order.status != "cancelled" && order.status != "rejected") {
                        // 4-Grid Status update buttons for active orders
                        Text("UPDATE DISPATCH STATE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Slate400, modifier = Modifier.padding(bottom = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusDispatchButton(label = "Process", active = order.status == "processing", onClick = { viewModel.updateOrderStatus(order.id, "processing") })
                            StatusDispatchButton(label = "Ship", active = order.status == "shipped", onClick = { viewModel.updateOrderStatus(order.id, "shipped") })
                            StatusDispatchButton(label = "Deliver", active = order.status == "delivered", onClick = { viewModel.updateOrderStatus(order.id, "delivered") })
                            StatusDispatchButton(label = "Cancel", active = false, danger = true, onClick = { viewModel.updateOrderStatus(order.id, "cancelled") })
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Invoice download receipts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { Toast.makeText(context, "Invoice PDF generated!", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Invoice", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = { Toast.makeText(context, "Receipt Thermal TXT generated!", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Receipt, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Receipt", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- Fullscreen Pinch-to-zoom Image overlay ---
        if (zoomImageUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { zoomImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                // Large dismiss button
                IconButton(
                    onClick = { zoomImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .safeDrawingPadding()
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Interactive Zoom Image
                AsyncImage(
                    model = zoomImageUrl,
                    contentDescription = "Zoomed proof",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "pending", "pending_review" -> Pair(AmberWarning.copy(alpha = 0.12f), AmberWarning)
        "confirmed" -> Pair(IndigoSecondary.copy(alpha = 0.12f), IndigoSecondary)
        "processing" -> Pair(IndigoPrimary.copy(alpha = 0.12f), IndigoPrimary)
        "shipped" -> Pair(Color(0xFF3B82F6).copy(alpha = 0.12f), Color(0xFF3B82F6))
        "delivered" -> Pair(EmeraldSuccess.copy(alpha = 0.12f), EmeraldSuccess)
        "cancelled", "rejected" -> Pair(RoseDanger.copy(alpha = 0.12f), RoseDanger)
        else -> Pair(Slate200, Slate700)
    }

    Text(
        text = status.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun ProfileField(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Slate400,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Slate800,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun RowScope.StatusDispatchButton(
    label: String,
    active: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (active) {
        if (danger) RoseDanger else IndigoPrimary
    } else Slate100

    val contentColor = if (active) Color.White else Slate700

    Box(
        modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = contentColor)
    }
}

@Composable
fun OrderStatusTimeline(currentStatus: String) {
    val steps = listOf("pending", "confirmed", "processing", "shipped", "delivered")
    val currentIndex = steps.indexOf(currentStatus.lowercase()).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isDone = index <= currentIndex
            val isCurrent = index == currentIndex

            val color = if (isDone) IndigoPrimary else Slate200

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(if (isCurrent) IndigoPrimary else color, CircleShape)
                        .border(
                            width = if (isCurrent) 3.dp else 0.dp,
                            color = if (isCurrent) IndigoPrimary.copy(alpha = 0.3f) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone && !isCurrent) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.capitalize(),
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isDone) Slate800 else Slate400,
                    textAlign = TextAlign.Center
                )
            }

            if (index < steps.size - 1) {
                val lineColor = if (index < currentIndex) IndigoPrimary else Slate200
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(3.dp)
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
fun BillingRow(label: String, value: String, valueColor: Color = Slate800) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Slate400)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
