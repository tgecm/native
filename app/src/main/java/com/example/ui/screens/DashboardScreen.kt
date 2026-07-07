package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bot
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val stats by viewModel.statsSummary.collectAsState()
    val ordersByDay by viewModel.ordersByDay.collectAsState()
    val topProducts by viewModel.topProducts.collectAsState()
    val botsList by viewModel.botsList.collectAsState()
    val currentBot by viewModel.currentBot.collectAsState()

    var showBotSelector by remember { mutableStateOf(false) }
    var selectedDaysRange by remember { mutableStateOf("30d") }
    var showRangeDropdown by remember { mutableStateOf(false) }

    // Stat Toggle: Revenue vs Stock Value
    var showStockValueStat by remember { mutableStateOf(false) }

    // Chart Configuration States
    var chartMetric by remember { mutableStateOf("revenue") } // revenue, count, profit
    var isLineChart by remember { mutableStateOf(true) }

    // Export Modal Bottom Sheet State
    var showExportSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showBotSelector = true }
                            .testTag("bot_selector_trigger")
                    ) {
                        Text(
                            text = currentBot?.botName ?: "Select Shop",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick, modifier = Modifier.testTag("drawer_menu_button")) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // Date range selection chip
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { showRangeDropdown = true },
                            label = { Text(selectedDaysRange.uppercase()) },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IndigoPrimary.copy(alpha = 0.1f),
                                selectedLabelColor = IndigoPrimary
                            ),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("range_chip")
                        )
                        DropdownMenu(
                            expanded = showRangeDropdown,
                            onDismissRequest = { showRangeDropdown = false }
                        ) {
                            DropdownMenuItem(text = { Text("Last 7 Days") }, onClick = { selectedDaysRange = "7d"; showRangeDropdown = false })
                            DropdownMenuItem(text = { Text("Last 30 Days") }, onClick = { selectedDaysRange = "30d"; showRangeDropdown = false })
                            DropdownMenuItem(text = { Text("Last 90 Days") }, onClick = { selectedDaysRange = "90d"; showRangeDropdown = false })
                        }
                    }

                    IconButton(onClick = { showExportSheet = true }, modifier = Modifier.testTag("export_button")) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Report", tint = IndigoPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome text and quick metrics sync
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Overview Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Real-time commerce insights",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }

                    Text(
                        text = "Auto-sync active",
                        color = EmeraldSuccess,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(EmeraldSuccess.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. 2x2 Grid of Stat Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        // Card 1: Total Revenue (toggles with Total Stock Value)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showStockValueStat = !showStockValueStat }
                                .testTag("revenue_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (showStockValueStat) Icons.Default.Inventory2 else Icons.Default.AttachMoney,
                                        contentDescription = "StatIcon",
                                        tint = if (showStockValueStat) AmberWarning else IndigoPrimary,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(
                                                if (showStockValueStat) AmberWarning.copy(alpha = 0.12f) else IndigoPrimary.copy(alpha = 0.12f),
                                                CircleShape
                                            )
                                            .padding(6.dp)
                                    )
                                    Icon(Icons.Default.SwapHoriz, null, tint = Slate400, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (showStockValueStat) "Stock Value" else "Total Revenue",
                                    color = Slate400,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (showStockValueStat) "$4,850.00" else "$${stats["total_revenue"] ?: "0.0"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Slate800,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Card 2: Total Orders
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("orders_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(EmeraldSuccess.copy(alpha = 0.12f), CircleShape)
                                        .padding(6.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Total Orders", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${stats["total_orders"] ?: 0}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Slate800,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        // Card 3: Total Users
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = IndigoSecondary,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(IndigoSecondary.copy(alpha = 0.12f), CircleShape)
                                        .padding(6.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Total Users", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${stats["total_users"] ?: 0}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Slate800,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Card 4: Pending Orders
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Slate200)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = RoseDanger,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(RoseDanger.copy(alpha = 0.12f), CircleShape)
                                        .padding(6.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Pending Orders", color = Slate400, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${stats["pending_orders"] ?: 0}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Slate800,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Mini Metrics Horizontal Grid Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniMetricItem(title = "Units Sold", value = "${stats["items_sold"] ?: 0}", iconColor = IndigoPrimary)
                    MiniMetricItem(title = "Today's Rev", value = "$${stats["today_revenue"] ?: 0.0}", iconColor = EmeraldSuccess)
                    MiniMetricItem(title = "Monthly Rev", value = "$${stats["monthly_revenue"] ?: 0.0}", iconColor = AmberWarning)
                    MiniMetricItem(title = "Products Count", value = "${stats["products_sold"] ?: 0}", iconColor = IndigoSecondary)
                }
            }

            // 3. Dynamic Canvas Chart (Visual Graph of Sales/Orders over time)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Analytics Trends",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Slate800
                                )
                                Text(
                                    text = "Overview over time period",
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }

                            // Line/Bar Toggle + Metric Selector
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isLineChart = !isLineChart }) {
                                    Icon(
                                        imageVector = if (isLineChart) Icons.Default.BarChart else Icons.Default.ShowChart,
                                        contentDescription = "Toggle Chart",
                                        tint = IndigoPrimary
                                    )
                                }
                                Box {
                                    var showMetricDrop by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = { showMetricDrop = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate800),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(chartMetric.capitalize(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    DropdownMenu(
                                        expanded = showMetricDrop,
                                        onDismissRequest = { showMetricDrop = false }
                                    ) {
                                        DropdownMenuItem(text = { Text("Revenue") }, onClick = { chartMetric = "revenue"; showMetricDrop = false })
                                        DropdownMenuItem(text = { Text("Orders") }, onClick = { chartMetric = "count"; showMetricDrop = false })
                                        DropdownMenuItem(text = { Text("Profit") }, onClick = { chartMetric = "profit"; showMetricDrop = false })
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom drawing graph inside a Canvas block!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Slate50, RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height

                                // Extract numeric values
                                val values = ordersByDay.map {
                                    (it[chartMetric] as? Number)?.toFloat() ?: 0f
                                }
                                val maxVal = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)

                                // Draw chart background lines
                                val steps = 4
                                for (i in 0..steps) {
                                    val y = height * i / steps
                                    drawLine(
                                        color = Slate200,
                                        start = Offset(0f, y),
                                        end = Offset(width, y),
                                        strokeWidth = 1f
                                    )
                                }

                                if (values.isNotEmpty()) {
                                    val xStep = width / (values.size - 1).coerceAtLeast(1)

                                    if (isLineChart) {
                                        // Draw Line Chart
                                        val path = Path()
                                        values.forEachIndexed { idx, valItem ->
                                            val x = idx * xStep
                                            val y = height - (valItem / maxVal * height)
                                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        }
                                        drawPath(
                                            path = path,
                                            color = IndigoPrimary,
                                            style = Stroke(width = 6f)
                                        )

                                        // Draw dots
                                        values.forEachIndexed { idx, valItem ->
                                            val x = idx * xStep
                                            val y = height - (valItem / maxVal * height)
                                            drawCircle(
                                                color = IndigoPrimary,
                                                radius = 6f,
                                                center = Offset(x, y)
                                            )
                                            drawCircle(
                                                color = Color.White,
                                                radius = 3f,
                                                center = Offset(x, y)
                                            )
                                        }
                                    } else {
                                        // Draw Bar Chart
                                        val barWidth = (xStep * 0.5f).coerceAtLeast(12f)
                                        values.forEachIndexed { idx, valItem ->
                                            val x = idx * xStep
                                            val y = height - (valItem / maxVal * height)
                                            drawLine(
                                                color = IndigoSecondary,
                                                start = Offset(x, height),
                                                end = Offset(x, y),
                                                strokeWidth = barWidth
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // X-Axis text
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ordersByDay.forEach {
                                Text(text = it["day"]?.toString() ?: "", fontSize = 9.sp, color = Slate400, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. Top Products Rankings with scaled progress indicators
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Top Selling Products",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Slate800,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val maxRevenue = topProducts.firstOrNull()?.let {
                            (it["total_revenue"] as? Number)?.toFloat() ?: 1f
                        } ?: 1f

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            topProducts.take(4).forEachIndexed { index, product ->
                                val name = product["name"]?.toString() ?: ""
                                val rev = (product["total_revenue"] as? Number)?.toDouble() ?: 0.0
                                val ordersCount = (product["order_count"] as? Number)?.toInt() ?: 0
                                val profit = (product["total_profit"] as? Number)?.toDouble() ?: 0.0
                                val ratio = (rev / maxRevenue).toFloat().coerceIn(0f, 1f)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank Number Badge
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                when (index) {
                                                    0 -> AmberWarning.copy(alpha = 0.15f)
                                                    1 -> Slate300.copy(alpha = 0.15f)
                                                    else -> Slate200.copy(alpha = 0.15f)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = when (index) {
                                                0 -> AmberWarning
                                                1 -> Slate700
                                                else -> Slate400
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Details & Progress
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Slate800,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "$$rev",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = IndigoPrimary
                                            )
                                        }

                                        // Progress bar indicator
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                                .height(6.dp)
                                                .clip(CircleShape)
                                                .background(Slate100)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(ratio)
                                                    .fillMaxHeight()
                                                    .background(IndigoPrimary)
                                            )
                                        }

                                        // Mini info
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "$ordersCount orders", fontSize = 10.sp, color = Slate400)
                                            Text(text = "Profit: +$$profit", fontSize = 10.sp, color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom empty padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // --- Bottom Sheet Shop/Bot Selector Modal ---
        if (showBotSelector) {
            ModalBottomSheet(
                onDismissRequest = { showBotSelector = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
                ) {
                    Text(
                        text = "Select Shop Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate800,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    botsList.forEach { bot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectBot(bot)
                                    showBotSelector = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Slate200),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, null, tint = IndigoPrimary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bot.botName ?: "Supermarket Bot", fontWeight = FontWeight.Bold, color = Slate800)
                                Text("@${bot.botUsername}", fontSize = 12.sp, color = Slate400)
                            }
                            if (currentBot?.id == bot.id) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = EmeraldSuccess)
                            }
                        }
                        HorizontalDivider(color = Slate200)
                    }
                }
            }
        }

        // --- Bottom Sheet Export Configuration Modal ---
        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                var fileName by remember { mutableStateOf("crossmart_report_july") }
                var fileType by remember { mutableStateOf("CSV") } // CSV, PDF
                var exportOrders by remember { mutableStateOf(true) }
                var exportSummary by remember { mutableStateOf(true) }
                var exportProducts by remember { mutableStateOf(false) }
                var exportCustomers by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {
                    Text(
                        text = "Export Shop Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate800,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Customize the details and sections to include in your exported business log.",
                        color = Slate400,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Filename input
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Filename prefix") },
                        trailingIcon = { Text(".$fileType", color = Slate400, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Format Toggle Selector
                    Text("Export File Format", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("CSV", "PDF").forEach { format ->
                            val selected = fileType == format
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) IndigoPrimary.copy(alpha = 0.12f) else Slate100)
                                    .border(1.dp, if (selected) IndigoPrimary else Color.Transparent, RoundedCornerShape(12.dp))
                                    .clickable { fileType = format }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(format, fontWeight = FontWeight.Bold, color = if (selected) IndigoPrimary else Slate700)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checklist options
                    Text("Include Report Sections", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Slate800)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportOrders = !exportOrders }) {
                        Checkbox(checked = exportOrders, onCheckedChange = { exportOrders = it })
                        Text("Order Details", color = Slate700, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportSummary = !exportSummary }) {
                        Checkbox(checked = exportSummary, onCheckedChange = { exportSummary = it })
                        Text("Daily Summary Stats Snapshot", color = Slate700, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportProducts = !exportProducts }) {
                        Checkbox(checked = exportProducts, onCheckedChange = { exportProducts = it })
                        Text("Top Products Metrics", color = Slate700, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { exportCustomers = !exportCustomers }) {
                        Checkbox(checked = exportCustomers, onCheckedChange = { exportCustomers = it })
                        Text("Customers Contact List", color = Slate700, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Download trigger
                    Button(
                        onClick = {
                            showExportSheet = false
                            // Trigger success snackbar simulation in view
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export & Download Report", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MiniMetricItem(title: String, value: String, iconColor: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.width(130.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(iconColor, CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Slate800, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
