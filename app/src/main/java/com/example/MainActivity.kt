package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = ViewModelProvider(
                    this,
                    ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                )[MainViewModel::class.java]

                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                AnimatedContent(
                    targetState = isLoggedIn,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "AuthSessionState"
                ) { loggedIn ->
                    if (!loggedIn) {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = { /* No-op, flow binds automatically via isLoggedIn StateFlow */ }
                        )
                    } else {
                        MainAppConsole(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppConsole(viewModel: MainViewModel) {
    val currentBot by viewModel.currentBot.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Active screen navigation route state
    var currentScreenRoute by remember { mutableStateOf("dashboard") }

    // Logout Dialog Confirmation State
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Navigation drawer sections mapping
    val drawerItems = listOf(
        DrawerItem("Overview Dashboard", "dashboard", Icons.Default.Dashboard),
        DrawerItem("Manage Orders", "orders", Icons.Default.Receipt),
        DrawerItem("Products Catalog", "products", Icons.Default.Inventory2),
        DrawerItem("Customers Database", "customers", Icons.Default.Group),
        DrawerItem("Support Chats", "chats", Icons.Default.Chat),
        DrawerItem("Sales Profit Logs", "profit", Icons.Default.TrendingUp),
        DrawerItem("Broadcast Marketing", "broadcast", Icons.Default.Campaign),
        DrawerItem("News & Social Feed", "newsfeed", Icons.Default.Newspaper),
        DrawerItem("Custom Bot Commands", "commands", Icons.Default.Code),
        DrawerItem("QR Menus Tables", "qr_menu", Icons.Default.QrCodeScanner),
        DrawerItem("Staff Credentials", "staff", Icons.Default.Badge),
        DrawerItem("Frequently FAQs", "faqs", Icons.Default.QuestionAnswer),
        DrawerItem("Plan Subscription", "subscription", Icons.Default.CardMembership),
        DrawerItem("Console Settings", "settings", Icons.Default.Settings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Slate900,
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 24.dp)
                ) {
                    // Drawer Header Profile Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = currentBot?.profilePicture ?: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=100",
                                contentDescription = "Shop Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Slate700)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentBot?.botName ?: "Supermarket Bot",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${currentBot?.botUsername ?: "crossmart_bot"}",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Scrollable list of modules
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        drawerItems.forEach { item ->
                            val selected = currentScreenRoute == item.route
                            NavigationDrawerItem(
                                label = { Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                selected = selected,
                                onClick = {
                                    currentScreenRoute = item.route
                                    scope.launch { drawerState.close() }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = IndigoPrimary,
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Slate400,
                                    unselectedTextColor = Slate300
                                ),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("drawer_item_${item.route}")
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Slate800,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                    )

                    // Logout drawer action item
                    NavigationDrawerItem(
                        label = { Text("Sign Out Session", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            showLogoutDialog = true
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = RoseDanger.copy(alpha = 0.8f),
                            unselectedTextColor = RoseDanger
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .height(48.dp)
                            .testTag("drawer_item_logout")
                    )
                }
            }
        }
    ) {
        // Core Layout containing dynamic screen and bottom action shortcuts
        Scaffold(
            bottomBar = {
                // Show bottom bar only for the core 5 tabs
                val isCoreTab = currentScreenRoute in listOf("dashboard", "orders", "products", "customers", "chats")
                if (isCoreTab) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreenRoute == "dashboard",
                            onClick = { currentScreenRoute = "dashboard" },
                            label = { Text("Overview", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Dashboard, "Overview") },
                            modifier = Modifier.testTag("bottom_tab_dashboard")
                        )
                        NavigationBarItem(
                            selected = currentScreenRoute == "orders",
                            onClick = { currentScreenRoute = "orders" },
                            label = { Text("Orders", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Receipt, "Orders") },
                            modifier = Modifier.testTag("bottom_tab_orders")
                        )
                        NavigationBarItem(
                            selected = currentScreenRoute == "products",
                            onClick = { currentScreenRoute = "products" },
                            label = { Text("Catalog", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Inventory2, "Products") },
                            modifier = Modifier.testTag("bottom_tab_products")
                        )
                        NavigationBarItem(
                            selected = currentScreenRoute == "customers",
                            onClick = { currentScreenRoute = "customers" },
                            label = { Text("Database", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Group, "Database") },
                            modifier = Modifier.testTag("bottom_tab_customers")
                        )
                        NavigationBarItem(
                            selected = currentScreenRoute == "chats",
                            onClick = { currentScreenRoute = "chats" },
                            label = { Text("Chats", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Chat, "Chats") },
                            modifier = Modifier.testTag("bottom_tab_chats")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Switch router
                when (currentScreenRoute) {
                    // Core 5 Screens
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    "orders" -> OrdersScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    "products" -> ProductsScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    "customers" -> CustomersScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )
                    "chats" -> ChatsScreen(
                        viewModel = viewModel,
                        onMenuClick = { scope.launch { drawerState.open() } }
                    )

                    // Secondary Screen Modules
                    "profit" -> ProfitScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                    "newsfeed" -> NewsfeedScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                    "broadcast" -> BroadcastScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                    "commands" -> TelegramCommandsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                    "qr_menu" -> QRMenuScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                    "staff" -> StaffAccountsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                    "faqs" -> FAQsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                    "subscription" -> SubscriptionScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                    "settings" -> SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreenRoute = "dashboard" }
                    )
                }
            }
        }
    }

    // --- Sign Out Dialog confirmation ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                    }
                ) {
                    Text("Proceed Sign Out", fontWeight = FontWeight.Bold, color = RoseDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Stay Logged In")
                }
            },
            title = { Text("Sign Out Session") },
            text = { Text("Are you sure you want to end your administration session for @${currentBot?.botUsername ?: "crossmart_bot"}?") }
        )
    }
}

data class DrawerItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)
