package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.CrossMartApiService
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "MainViewModel"

    // --- Core Connection State ---
    private val _isDemoMode = MutableStateFlow(true) // Default to Demo Mode so preview works immediately!
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("owner@crossmart.com")
    val currentUserEmail: StateFlow<String> = _currentUserEmail.asStateFlow()

    private val _isStaffUser = MutableStateFlow(false)
    val isStaffUser: StateFlow<Boolean> = _isStaffUser.asStateFlow()

    private val _currentBot = MutableStateFlow<Bot?>(null)
    val currentBot: StateFlow<Bot?> = _currentBot.asStateFlow()

    private val _botsList = MutableStateFlow<List<Bot>>(emptyList())
    val botsList: StateFlow<List<Bot>> = _botsList.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Dynamic Background Auto-Refresh Jobs ---
    private var statsRefreshJob: Job? = null
    private var ordersRefreshJob: Job? = null
    private var chatsRefreshJob: Job? = null

    // --- Retrofit API Client ---
    private var apiService: CrossMartApiService? = null

    // --- Screen Specific States ---
    private val _statsSummary = MutableStateFlow<Map<String, Any>>(emptyMap())
    val statsSummary: StateFlow<Map<String, Any>> = _statsSummary.asStateFlow()

    private val _ordersByDay = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val ordersByDay: StateFlow<List<Map<String, Any>>> = _ordersByDay.asStateFlow()

    private val _topProducts = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val topProducts: StateFlow<List<Map<String, Any>>> = _topProducts.asStateFlow()

    private val _usersByDay = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val usersByDay: StateFlow<List<Map<String, Any>>> = _usersByDay.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _telegramUsers = MutableStateFlow<List<Customer>>(emptyList())
    val telegramUsers: StateFlow<List<Customer>> = _telegramUsers.asStateFlow()

    private val _websiteCustomers = MutableStateFlow<List<WebsiteCustomer>>(emptyList())
    val websiteCustomers: StateFlow<List<WebsiteCustomer>> = _websiteCustomers.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<Int, List<Message>>>(emptyMap()) // Keyed by userId
    val messages: StateFlow<Map<Int, List<Message>>> = _messages.asStateFlow()

    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods.asStateFlow()

    private val _codEnabled = MutableStateFlow(true)
    val codEnabled: StateFlow<Boolean> = _codEnabled.asStateFlow()

    private val _codDeliveryFee = MutableStateFlow(2000.0)
    val codDeliveryFee: StateFlow<Double> = _codDeliveryFee.asStateFlow()

    private val _faqs = MutableStateFlow<List<FAQ>>(emptyList())
    val faqs: StateFlow<List<FAQ>> = _faqs.asStateFlow()

    private val _customCommands = MutableStateFlow<List<CustomCommand>>(emptyList())
    val customCommands: StateFlow<List<CustomCommand>> = _customCommands.asStateFlow()

    private val _newsPosts = MutableStateFlow<List<NewsPost>>(emptyList())
    val newsPosts: StateFlow<List<NewsPost>> = _newsPosts.asStateFlow()

    private val _broadcasts = MutableStateFlow<List<Broadcast>>(emptyList())
    val broadcasts: StateFlow<List<Broadcast>> = _broadcasts.asStateFlow()

    private val _giveaways = MutableStateFlow<List<Giveaway>>(emptyList())
    val giveaways: StateFlow<List<Giveaway>> = _giveaways.asStateFlow()

    private val _staffAccounts = MutableStateFlow<List<Staff>>(emptyList())
    val staffAccounts: StateFlow<List<Staff>> = _staffAccounts.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    // --- QR Menu Specific States ---
    private val _qrMenuItems = MutableStateFlow<List<QRMenuItem>>(emptyList())
    val qrMenuItems: StateFlow<List<QRMenuItem>> = _qrMenuItems.asStateFlow()

    private val _qrMenuCategories = MutableStateFlow<List<QRMenuCategory>>(emptyList())
    val qrMenuCategories: StateFlow<List<QRMenuCategory>> = _qrMenuCategories.asStateFlow()

    private val _qrTables = MutableStateFlow<List<QRTable>>(emptyList())
    val qrTables: StateFlow<List<QRTable>> = _qrTables.asStateFlow()

    private val _qrOrders = MutableStateFlow<List<QROrder>>(emptyList())
    val qrOrders: StateFlow<List<QROrder>> = _qrOrders.asStateFlow()

    // --- Subscription states ---
    private val _discountValidationMessage = MutableStateFlow<String?>(null)
    val discountValidationMessage: StateFlow<String?> = _discountValidationMessage.asStateFlow()

    init {
        // Build Retrofit Service
        initRetrofit()
        // Load initial mock datasets so that app is beautiful and fully active out of the box
        loadMockData()
    }

    private fun initRetrofit() {
        try {
            val moshi = Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()
                    if (_token.value.isNotEmpty()) {
                        requestBuilder.header("Authorization", "Bearer ${_token.value}")
                    }
                    chain.proceed(requestBuilder.build())
                }
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.telegramecommerce.shop/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(CrossMartApiService::class.java)
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize retrofit: ${e.message}")
        }
    }

    fun setDemoMode(demo: Boolean) {
        _isDemoMode.value = demo
        if (!demo) {
            // Refresh via API
            refreshAllData()
        } else {
            // Reset to mock data
            loadMockData()
        }
    }

    // --- Login & Authentication ---
    fun loginOwner(email: String, password: String, onStep2FA: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (_isDemoMode.value) {
                    delay(800)
                    _currentUserEmail.value = email
                    _isStaffUser.value = false
                    onStep2FA("demo-login-token-uuid")
                } else {
                    val api = apiService
                    if (api != null) {
                        val response = api.loginOwner(mapOf("email" to email, "password" to password))
                        val loginToken = response["login_token"] ?: "temp-token"
                        onStep2FA(loginToken)
                    } else {
                        _errorMessage.value = "Retrofit not initialized"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Authentication failed"
                Log.e(tag, "loginOwner error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyOwnerLogin(loginToken: String, code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (_isDemoMode.value) {
                    delay(800)
                    _token.value = "demo_jwt_token_sample"
                    _isLoggedIn.value = true
                    // Select default demo bot
                    _currentBot.value = _botsList.value.firstOrNull()
                    onSuccess()
                } else {
                    val api = apiService
                    if (api != null) {
                        val response = api.verifyOwnerLogin(mapOf("login_token" to loginToken, "code" to code))
                        val success = response["success"] as? Boolean ?: false
                        if (success) {
                            val tokenValue = response["token"] as? String ?: ""
                            _token.value = tokenValue
                            _isLoggedIn.value = true
                            refreshAllData()
                            onSuccess()
                        } else {
                            _errorMessage.value = "Incorrect verification code"
                        }
                    } else {
                        _errorMessage.value = "Retrofit not initialized"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Verification failed"
                Log.e(tag, "verifyOwnerLogin error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginStaff(username: String, password: String, onStep2FA: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (_isDemoMode.value) {
                    delay(800)
                    _currentUserEmail.value = "$username@staff.crossmart.com"
                    _isStaffUser.value = true
                    onStep2FA("demo-staff-token-uuid")
                } else {
                    val api = apiService
                    if (api != null) {
                        val response = api.loginStaff(mapOf("username" to username, "password" to password))
                        val loginToken = response["login_token"] as? String ?: "temp-token"
                        onStep2FA(loginToken)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Staff Login failed"
                Log.e(tag, "loginStaff error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyStaffLogin(loginToken: String, code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                if (_isDemoMode.value) {
                    delay(800)
                    _token.value = "demo_staff_jwt_token"
                    _isLoggedIn.value = true
                    _currentBot.value = _botsList.value.firstOrNull()
                    onSuccess()
                } else {
                    val api = apiService
                    if (api != null) {
                        val response = api.verifyStaffLogin(mapOf("login_token" to loginToken, "code" to code))
                        val success = response["success"] as? Boolean ?: false
                        if (success) {
                            val tokenValue = response["token"] as? String ?: ""
                            _token.value = tokenValue
                            _isLoggedIn.value = true
                            _isStaffUser.value = true
                            refreshAllData()
                            onSuccess()
                        } else {
                            _errorMessage.value = "Incorrect verification code"
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Verification failed"
                Log.e(tag, "verifyStaffLogin error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        _token.value = ""
        stopAutoRefresh()
    }

    fun selectBot(bot: Bot) {
        _currentBot.value = bot
        refreshAllData()
    }

    // --- Live Data Syncing / Pull-to-refresh ---
    fun refreshAllData() {
        if (_isDemoMode.value) return // In demo mode, no API refresh needed

        viewModelScope.launch {
            _isLoading.value = true
            try {
                syncBots()
                val bot = _currentBot.value
                if (bot != null) {
                    syncStats(bot.id)
                    syncOrders(bot.id)
                    syncProducts(bot.id)
                    syncCategories(bot.id)
                    syncCustomers(bot.id)
                    syncChats(bot.id)
                    syncPaymentMethods(bot.id)
                    syncFAQs()
                    syncStaff(bot.id)
                }
            } catch (e: Exception) {
                Log.e(tag, "refreshAllData error", e)
                _errorMessage.value = "Failed to sync remote data. Offline/Demo mode active."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startAutoRefresh() {
        stopAutoRefresh()

        // 30s Dashboard stats refresh
        statsRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30000)
                val bot = _currentBot.value
                if (bot != null && !_isDemoMode.value) {
                    try {
                        syncStats(bot.id)
                    } catch (e: Exception) {
                        Log.e(tag, "Auto stats refresh fail: ${e.message}")
                    }
                }
            }
        }

        // 15s Orders & Chats refresh
        ordersRefreshJob = viewModelScope.launch {
            while (true) {
                delay(15000)
                val bot = _currentBot.value
                if (bot != null && !_isDemoMode.value) {
                    try {
                        syncOrders(bot.id)
                    } catch (e: Exception) {
                        Log.e(tag, "Auto orders refresh fail: ${e.message}")
                    }
                }
            }
        }

        chatsRefreshJob = viewModelScope.launch {
            while (true) {
                delay(15000)
                val bot = _currentBot.value
                if (bot != null && !_isDemoMode.value) {
                    try {
                        syncChats(bot.id)
                    } catch (e: Exception) {
                        Log.e(tag, "Auto chats refresh fail: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopAutoRefresh() {
        statsRefreshJob?.cancel()
        ordersRefreshJob?.cancel()
        chatsRefreshJob?.cancel()
    }

    // --- Specific API Sync Helpers ---
    private suspend fun syncBots() {
        val api = apiService ?: return
        val list = api.listBots()
        _botsList.value = list
        if (_currentBot.value == null && list.isNotEmpty()) {
            _currentBot.value = list.first()
        }
    }

    private suspend fun syncStats(botId: Int) {
        val api = apiService ?: return
        val summary = api.getStatsSummary(botId, days = 30)
        _statsSummary.value = summary

        val ordersByDayList = api.getOrdersByDay(botId, days = 30)
        _ordersByDay.value = ordersByDayList

        val topProdList = api.getTopProducts(botId, limit = 10)
        _topProducts.value = topProdList

        val usersByDayList = api.getUsersByDay(botId, days = 30)
        _usersByDay.value = usersByDayList
    }

    private suspend fun syncOrders(botId: Int) {
        val api = apiService ?: return
        val list = api.listOrders(botId)
        _orders.value = list
    }

    private suspend fun syncProducts(botId: Int) {
        val api = apiService ?: return
        val list = api.listProducts(botId)
        _products.value = list
    }

    private suspend fun syncCategories(botId: Int) {
        val api = apiService ?: return
        val list = api.listCategories(botId)
        _categories.value = list
    }

    private suspend fun syncCustomers(botId: Int) {
        val api = apiService ?: return
        val tgList = api.listTelegramUsers(botId)
        _telegramUsers.value = tgList

        val webList = api.listWebsiteCustomers(botId)
        _websiteCustomers.value = webList
    }

    private suspend fun syncChats(botId: Int) {
        val api = apiService ?: return
        val list = api.listChats(botId)
        _chats.value = list
    }

    private suspend fun syncPaymentMethods(botId: Int) {
        val api = apiService ?: return
        val list = api.listPaymentMethods(botId)
        _paymentMethods.value = list

        val codSet = api.getCodSettings(botId)
        _codEnabled.value = codSet["enabled"] as? Boolean ?: true
        _codDeliveryFee.value = (codSet["delivery_fee"] as? Number)?.toDouble() ?: 2000.0
    }

    private suspend fun syncFAQs() {
        val api = apiService ?: return
        val list = api.listFAQs()
        _faqs.value = list
    }

    private suspend fun syncStaff(botId: Int) {
        val api = apiService ?: return
        val list = api.listStaff(botId)
        _staffAccounts.value = list
    }

    // --- CRUD Actions (Handles Demo state updates + API calls) ---

    // 1. Orders
    fun updateOrderStatus(orderId: Int, status: String) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _orders.update { current ->
                        current.map {
                            if (it.id == orderId) {
                                // Restore stock on Cancelled, following business triggers!
                                if (status == "cancelled" && it.status != "cancelled") {
                                    restoreStockForOrder(it)
                                }
                                it.copy(status = status)
                            } else it
                        }
                    }
                    delay(300)
                } else {
                    val api = apiService
                    if (api != null) {
                        val updated = api.updateOrderStatus(orderId, mapOf("status" to status))
                        _orders.update { current ->
                            current.map { if (it.id == orderId) updated else it }
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update order status: ${e.message}"
            }
        }
    }

    private fun restoreStockForOrder(order: Order) {
        val items = order.items
        _products.update { current ->
            current.map { prod ->
                val matched = items.find { it.productId == prod.id }
                if (matched != null) {
                    val currentStock = prod.stockQuantity ?: 0
                    prod.copy(stockQuantity = currentStock + matched.quantity)
                } else prod
            }
        }
    }

    // 2. Products CRUD
    fun addProduct(
        name: String,
        description: String,
        price: Double,
        costPrice: Double,
        stock: Int,
        categoryId: Int?,
        variants: List<Variant>,
        imageUrl: String
    ) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val newId = (_products.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newProd = Product(
                        id = newId,
                        botId = bot.id,
                        name = name,
                        description = description,
                        price = price,
                        costPrice = costPrice,
                        stockQuantity = stock,
                        imageUrl = if (imageUrl.isNotEmpty()) imageUrl else "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400",
                        categoryId = categoryId,
                        variants = variants,
                        sortOrder = _products.value.size,
                        isActive = true,
                        createdAt = getTodayString()
                    )
                    _products.update { it + newProd }
                    delay(200)
                } else {
                    val api = apiService
                    if (api != null) {
                        val body = mapOf(
                            "bot_id" to bot.id,
                            "name" to name,
                            "description" to description,
                            "price" to price,
                            "cost_price" to costPrice,
                            "stock_quantity" to stock,
                            "category_id" to categoryId,
                            "variants" to variants,
                            "image_url" to imageUrl
                        )
                        val created = api.createProduct(body)
                        _products.update { it + created }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add product: ${e.message}"
            }
        }
    }

    fun updateProduct(
        productId: Int,
        name: String,
        description: String,
        price: Double,
        costPrice: Double,
        stock: Int,
        categoryId: Int?,
        variants: List<Variant>,
        imageUrl: String,
        isActive: Boolean
    ) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _products.update { current ->
                        current.map {
                            if (it.id == productId) {
                                it.copy(
                                    name = name,
                                    description = description,
                                    price = price,
                                    costPrice = costPrice,
                                    stockQuantity = stock,
                                    categoryId = categoryId,
                                    variants = variants,
                                    imageUrl = if (imageUrl.isNotEmpty()) imageUrl else it.imageUrl,
                                    isActive = isActive
                                )
                            } else it
                        }
                    }
                    delay(200)
                } else {
                    val api = apiService
                    if (api != null) {
                        val body = mapOf(
                            "name" to name,
                            "description" to description,
                            "price" to price,
                            "cost_price" to costPrice,
                            "stock_quantity" to stock,
                            "category_id" to categoryId,
                            "variants" to variants,
                            "image_url" to imageUrl,
                            "is_active" to isActive
                        )
                        val updated = api.updateProduct(productId, body)
                        _products.update { current ->
                            current.map { if (it.id == productId) updated else it }
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to edit product: ${e.message}"
            }
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _products.update { current -> current.filter { it.id != productId } }
                } else {
                    val api = apiService
                    if (api != null) {
                        api.deleteProduct(productId)
                        _products.update { current -> current.filter { it.id != productId } }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete product: ${e.message}"
            }
        }
    }

    fun reorderProducts(items: List<Product>) {
        _products.value = items
        if (!_isDemoMode.value) {
            viewModelScope.launch {
                try {
                    val bot = _currentBot.value ?: return@launch
                    val body = mapOf(
                        "bot_id" to bot.id,
                        "items" to items.mapIndexed { index, product ->
                            mapOf("id" to product.id, "sort_order" to index)
                        }
                    )
                    apiService?.updateProductSortOrder(body)
                } catch (e: Exception) {
                    Log.e(tag, "Failed to persist sort order: ${e.message}")
                }
            }
        }
    }

    // 3. Category CRUD
    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val newId = (_categories.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newCat = Category(
                        id = newId,
                        botId = bot.id,
                        name = name,
                        emoji = emoji,
                        sortOrder = _categories.value.size
                    )
                    _categories.update { it + newCat }
                } else {
                    val api = apiService
                    if (api != null) {
                        val body = mapOf(
                            "bot_id" to bot.id,
                            "name" to name,
                            "emoji" to emoji
                        )
                        val created = api.createCategory(body)
                        _categories.update { it + created }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add category: ${e.message}"
            }
        }
    }

    fun updateCategory(categoryId: Int, name: String, emoji: String) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _categories.update { current ->
                        current.map {
                            if (it.id == categoryId) {
                                it.copy(name = name, emoji = emoji)
                            } else it
                        }
                    }
                } else {
                    val api = apiService
                    if (api != null) {
                        val updated = api.updateCategory(categoryId, mapOf("name" to name, "emoji" to emoji))
                        _categories.update { current ->
                            current.map { if (it.id == categoryId) updated else it }
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update category: ${e.message}"
            }
        }
    }

    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _categories.update { current -> current.filter { it.id != categoryId } }
                } else {
                    apiService?.deleteCategory(categoryId)
                    _categories.update { current -> current.filter { it.id != categoryId } }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete category: ${e.message}"
            }
        }
    }

    // 4. Chats & Thread management
    fun getMessagesForUser(userId: Int) {
        if (_isDemoMode.value) return // Uses local memory
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                val list = apiService?.getChatMessages(userId, bot.id) ?: emptyList()
                _messages.update { current ->
                    current.toMutableMap().apply { put(userId, list) }
                }
            } catch (e: Exception) {
                Log.e(tag, "getMessagesForUser error", e)
            }
        }
    }

    fun sendChatMessage(userId: Int, text: String, fileId: String? = null, fileType: String? = null) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                val newMsg = Message(
                    id = UUID.randomUUID().toString(),
                    text = text,
                    sender = "bot",
                    fileId = fileId,
                    fileType = fileType,
                    date = getTodayString()
                )

                // Update thread locally first (optimistic UI)
                _messages.update { current ->
                    val userMsgs = (current[userId] ?: emptyList()) + newMsg
                    current.toMutableMap().apply { put(userId, userMsgs) }
                }

                // Update last message in chat list
                _chats.update { list ->
                    list.map {
                        if (it.userId == userId) {
                            it.copy(
                                lastMessage = text.ifEmpty { "Attachment: $fileType" },
                                lastMessageType = fileType ?: "text",
                                lastMessageAt = getTodayString(),
                                unreadCount = 0
                            )
                        } else it
                    }
                }

                if (!_isDemoMode.value) {
                    val api = apiService
                    if (api != null) {
                        api.sendChatMessage(
                            userId, mapOf(
                                "bot_id" to bot.id.toString(),
                                "message" to text,
                                "file_id" to fileId,
                                "file_type" to fileType
                            )
                        )
                    }
                } else {
                    // Simulated automatic echo reply from AI or Customer in 1.5 seconds!
                    delay(1200)
                    val replyText = if (bot.aiEnabled) {
                        "🤖 [AI Auto] Thanks for your message! Our shop system is processing your inquiry regarding crossmart."
                    } else {
                        "Hello, our staff will check this shortly. Thanks!"
                    }
                    val replyMsg = Message(
                        id = UUID.randomUUID().toString(),
                        text = replyText,
                        sender = "user",
                        date = getTodayString()
                    )
                    _messages.update { current ->
                        val userMsgs = (current[userId] ?: emptyList()) + replyMsg
                        current.toMutableMap().apply { put(userId, userMsgs) }
                    }
                    _chats.update { list ->
                        list.map {
                            if (it.userId == userId) {
                                it.copy(
                                    lastMessage = replyText,
                                    lastMessageType = "text",
                                    lastMessageAt = getTodayString()
                                )
                            } else it
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "sendChatMessage error", e)
            }
        }
    }

    fun markChatRead(userId: Int) {
        _chats.update { list ->
            list.map { if (it.userId == userId) it.copy(unreadCount = 0) else it }
        }
        if (!_isDemoMode.value) {
            viewModelScope.launch {
                try {
                    val bot = _currentBot.value ?: return@launch
                    apiService?.markChatRead(userId, mapOf("bot_id" to bot.id))
                } catch (e: Exception) {
                    Log.e(tag, "markChatRead fail", e)
                }
            }
        }
    }

    fun deleteChat(userId: Int) {
        _chats.update { current -> current.filter { it.userId != userId } }
        if (!_isDemoMode.value) {
            viewModelScope.launch {
                try {
                    val bot = _currentBot.value ?: return@launch
                    apiService?.deleteChat(userId, bot.id)
                } catch (e: Exception) {
                    Log.e(tag, "deleteChat fail", e)
                }
            }
        }
    }

    // 5. Payment Methods & COD
    fun addPaymentMethod(type: String, accountName: String, accountNumber: String, qrImageUrl: String) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val newId = (_paymentMethods.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newMethod = PaymentMethod(newId, type, accountName, accountNumber, qrImageUrl, true)
                    _paymentMethods.update { it + newMethod }
                } else {
                    val body = mapOf(
                        "bot_id" to bot.id,
                        "type" to type,
                        "account_name" to accountName,
                        "account_number" to accountNumber,
                        "qr_image_url" to qrImageUrl
                    )
                    val created = apiService?.createPaymentMethod(body)
                    if (created != null) {
                        _paymentMethods.update { it + created }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add payment method"
            }
        }
    }

    fun updatePaymentMethodStatus(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _paymentMethods.update { current ->
                        current.map { if (it.id == id) it.copy(isActive = isActive) else it }
                    }
                } else {
                    val updated = apiService?.updatePaymentMethod(id, mapOf("is_active" to isActive))
                    if (updated != null) {
                        _paymentMethods.update { current ->
                            current.map { if (it.id == id) updated else it }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "updatePaymentMethodStatus fail", e)
            }
        }
    }

    fun deletePaymentMethod(id: Int) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _paymentMethods.update { current -> current.filter { it.id != id } }
                } else {
                    apiService?.deletePaymentMethod(id)
                    _paymentMethods.update { current -> current.filter { it.id != id } }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete payment"
            }
        }
    }

    fun updateCodSettings(enabled: Boolean, deliveryFee: Double) {
        _codEnabled.value = enabled
        _codDeliveryFee.value = deliveryFee
        if (!_isDemoMode.value) {
            viewModelScope.launch {
                try {
                    val bot = _currentBot.value ?: return@launch
                    apiService?.updateCodSettings(bot.id, mapOf("enabled" to enabled, "delivery_fee" to deliveryFee))
                } catch (e: Exception) {
                    Log.e(tag, "updateCodSettings fail", e)
                }
            }
        }
    }

    // 6. FAQs
    fun addFAQ(question: String, answer: String) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    val newId = (_faqs.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newFAQ = FAQ(newId, question, answer, _faqs.value.size)
                    _faqs.update { it + newFAQ }
                } else {
                    val created = apiService?.createFAQ(mapOf("question" to question, "answer" to answer))
                    if (created != null) {
                        _faqs.update { it + created }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add FAQ"
            }
        }
    }

    fun updateFAQ(id: Int, question: String, answer: String) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _faqs.update { current ->
                        current.map { if (it.id == id) it.copy(question = question, answer = answer) else it }
                    }
                } else {
                    val updated = apiService?.updateFAQ(id, mapOf("question" to question, "answer" to answer))
                    if (updated != null) {
                        _faqs.update { current -> current.map { if (it.id == id) updated else it } }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "updateFAQ error", e)
            }
        }
    }

    fun deleteFAQ(id: Int) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _faqs.update { current -> current.filter { it.id != id } }
                } else {
                    apiService?.deleteFAQ(id)
                    _faqs.update { current -> current.filter { it.id != id } }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete FAQ"
            }
        }
    }

    fun reorderFAQs(items: List<FAQ>) {
        _faqs.value = items
        if (!_isDemoMode.value) {
            viewModelScope.launch {
                try {
                    val body = mapOf(
                        "items" to items.mapIndexed { index, faq ->
                            mapOf("id" to faq.id, "sort_order" to index)
                        }
                    )
                    apiService?.reorderFAQs(body)
                } catch (e: Exception) {
                    Log.e(tag, "reorderFAQs error", e)
                }
            }
        }
    }

    // 7. Custom Commands
    fun addCustomCommand(command: String, description: String, reply: String, mediaUrl: String? = null, mediaType: String? = null) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val newId = (_customCommands.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newCmd = CustomCommand(newId, command, description, reply, mediaUrl, mediaType, true)
                    _customCommands.update { it + newCmd }
                } else {
                    val body = mapOf(
                        "bot_id" to bot.id,
                        "command" to command,
                        "description" to description,
                        "reply" to reply,
                        "media_url" to (mediaUrl ?: ""),
                        "media_type" to (mediaType ?: "")
                    )
                    val created = apiService?.createCustomCommand(body)
                    if (created != null) {
                        _customCommands.update { it + created }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create custom command"
            }
        }
    }

    fun deleteCustomCommand(id: Int) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _customCommands.update { current -> current.filter { it.id != id } }
                } else {
                    apiService?.deleteCustomCommand(id)
                    _customCommands.update { current -> current.filter { it.id != id } }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete command"
            }
        }
    }

    // 8. Newsfeed Posts
    fun addNewsPost(content: String, imageUrl: String?, topic: String) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val newId = (_newsPosts.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newPost = NewsPost(
                        id = newId,
                        botId = bot.id,
                        content = content,
                        imageUrl = imageUrl,
                        topic = topic,
                        createdAt = getTodayString()
                    )
                    _newsPosts.update { listOf(newPost) + it }
                } else {
                    val body = mapOf(
                        "bot_id" to bot.id,
                        "content" to content,
                        "image_url" to (imageUrl ?: ""),
                        "topic" to topic
                    )
                    val created = apiService?.createNewsfeedPost(body)
                    if (created != null) {
                        _newsPosts.update { listOf(created) + it }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create newsfeed post"
            }
        }
    }

    fun deleteNewsPost(id: Int) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    _newsPosts.update { current -> current.filter { it.id != id } }
                } else {
                    apiService?.deleteNewsfeedPost(id)
                    _newsPosts.update { current -> current.filter { it.id != id } }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete post"
            }
        }
    }

    // 9. Staff Accounts
    fun addStaffAccount(name: String, username: String, pass: String, ownerPass: String, perms: StaffPermissions, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    // Validate owner pass
                    if (ownerPass != "owner123") {
                        _errorMessage.value = "Incorrect owner password verification"
                        return@launch
                    }
                    val newId = (_staffAccounts.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newStaff = Staff(newId, name, username, bot.id, perms)
                    _staffAccounts.update { it + newStaff }

                    // Add activity log
                    val logId = (_activityLogs.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val log = ActivityLog(logId, newId, "CREATE_STAFF", "Created staff account: $username", getTodayString())
                    _activityLogs.update { listOf(log) + it }

                    onSuccess()
                } else {
                    val body = mapOf(
                        "bot_id" to bot.id,
                        "name" to name,
                        "username" to username,
                        "password" to pass,
                        "owner_password" to ownerPass,
                        "permissions" to mapOf(
                            "dashboard" to perms.dashboard,
                            "orders" to perms.orders,
                            "products" to perms.products,
                            "customers" to perms.customers,
                            "chats" to perms.chats,
                            "newsfeed" to perms.newsfeed,
                            "payments" to perms.payments,
                            "settings" to perms.settings,
                            "broadcast" to perms.broadcast,
                            "commands" to perms.commands,
                            "staff" to perms.staff,
                            "faqs" to perms.faqs
                        )
                    )
                    val created = apiService?.createStaff(body)
                    if (created != null) {
                        _staffAccounts.update { it + created }
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create staff account"
            }
        }
    }

    fun deleteStaff(id: Int, ownerPass: String) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    if (ownerPass != "owner123") {
                        _errorMessage.value = "Incorrect owner password"
                        return@launch
                    }
                    _staffAccounts.update { current -> current.filter { it.id != id } }
                } else {
                    apiService?.deleteStaff(id, mapOf("owner_password" to ownerPass))
                    _staffAccounts.update { current -> current.filter { it.id != id } }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete staff account: ${e.message}"
            }
        }
    }

    // 10. Broadcasts & Giveaways
    fun createBroadcast(message: String, imageUrl: String?, btnText: String?, btnUrl: String?, target: String) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val newId = (_broadcasts.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newBc = Broadcast(
                        id = newId,
                        botId = bot.id,
                        message = message,
                        imageUrl = imageUrl,
                        buttonText = btnText,
                        buttonUrl = btnUrl,
                        scheduleAt = getTodayString(),
                        target = target,
                        status = "sent",
                        targetCount = if (target == "all") 150 else 75,
                        createdAt = getTodayString()
                    )
                    _broadcasts.update { listOf(newBc) + it }
                } else {
                    val body = mapOf(
                        "bot_id" to bot.id,
                        "message" to message,
                        "image_url" to (imageUrl ?: ""),
                        "button_text" to (btnText ?: ""),
                        "button_url" to (btnUrl ?: ""),
                        "schedule_at" to getTodayString(),
                        "target" to target
                    )
                    val created = apiService?.createBroadcast(body)
                    if (created != null) {
                        _broadcasts.update { listOf(created) + it }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send broadcast"
            }
        }
    }

    fun createGiveaway(prize: String, description: String?, winnerCount: Int, endDate: String?, imageUrl: String?) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val newId = (_giveaways.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newGiveaway = Giveaway(
                        id = newId,
                        botId = bot.id,
                        prize = prize,
                        description = description,
                        winnerCount = winnerCount,
                        endDate = endDate ?: "In 3 Days",
                        imageUrl = imageUrl ?: "https://images.unsplash.com/photo-1513151233558-d860c5398176?w=400",
                        participantCount = 42,
                        status = "active",
                        winners = emptyList()
                    )
                    _giveaways.update { listOf(newGiveaway) + it }
                } else {
                    val body = mapOf(
                        "bot_id" to bot.id,
                        "prize" to prize,
                        "description" to (description ?: ""),
                        "winner_count" to winnerCount,
                        "end_date" to (endDate ?: ""),
                        "image_url" to (imageUrl ?: "")
                    )
                    val created = apiService?.createGiveaway(body)
                    if (created != null) {
                        _giveaways.update { listOf(created) + it }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create giveaway"
            }
        }
    }

    fun drawGiveawayWinners(giveawayId: Int, method: String, count: Int, onComplete: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                if (_isDemoMode.value) {
                    delay(2000) // Animated spinner delay
                    val mockParticipants = listOf("@alex_tg", "@user_99", "@mgmg_ygn", "@khinthiri", "@john_doe", "@su_su", "@tg_owner", "@staff_1")
                    val winners = mockParticipants.shuffled().take(count)
                    _giveaways.update { current ->
                        current.map {
                            if (it.id == giveawayId) {
                                it.copy(status = "drawn", winners = winners)
                            } else it
                        }
                    }
                    onComplete(winners)
                } else {
                    val response = apiService?.drawGiveaway(giveawayId, mapOf("method" to method, "count" to count)) ?: emptyList()
                    _giveaways.update { current ->
                        current.map {
                            if (it.id == giveawayId) {
                                it.copy(status = "drawn", winners = response)
                            } else it
                        }
                    }
                    onComplete(response)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to draw giveaway: ${e.message}"
            }
        }
    }

    // 11. Bot Settings CRUD
    fun updateBotSettings(name: String, currency: String, profilePic: String) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val updated = bot.copy(botName = name, currency = currency, profilePicture = profilePic)
                    _currentBot.value = updated
                    _botsList.update { list ->
                        list.map { if (it.id == bot.id) updated else it }
                    }
                    delay(200)
                } else {
                    val api = apiService
                    if (api != null) {
                        val updated = api.updateBot(bot.id, mapOf("bot_name" to name, "currency" to currency, "profile_picture" to profilePic))
                        _currentBot.value = updated
                        _botsList.update { list ->
                            list.map { if (it.id == bot.id) updated else it }
                        }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update bot settings: ${e.message}"
            }
        }
    }

    fun updateAiSettings(enabled: Boolean, prompt: String, model: String, temp: Float, maxTokens: Int) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val updated = bot.copy(
                        aiEnabled = enabled,
                        aiPrompt = prompt,
                        aiModel = model,
                        aiTemperature = temp,
                        aiMaxTokens = maxTokens
                    )
                    _currentBot.value = updated
                    _botsList.update { list ->
                        list.map { if (it.id == bot.id) updated else it }
                    }
                    delay(200)
                } else {
                    val body = mapOf(
                        "ai_enabled" to enabled,
                        "ai_prompt" to prompt,
                        "ai_model" to model,
                        "ai_temperature" to temp,
                        "ai_max_tokens" to maxTokens
                    )
                    apiService?.updateAiSettings(bot.id, body)
                    val updated = bot.copy(
                        aiEnabled = enabled,
                        aiPrompt = prompt,
                        aiModel = model,
                        aiTemperature = temp,
                        aiMaxTokens = maxTokens
                    )
                    _currentBot.value = updated
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save AI configuration: ${e.message}"
            }
        }
    }

    // 12. QR Menu Operations
    fun addQRMenuItem(name: String, price: Double, desc: String, categoryId: Int?, badges: List<String>, imageUrl: String) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                if (_isDemoMode.value) {
                    val newId = (_qrMenuItems.value.map { it.id }.maxOrNull() ?: 0) + 1
                    val newItem = QRMenuItem(
                        id = newId,
                        botId = bot.id,
                        name = name,
                        price = price,
                        categoryId = categoryId,
                        imageUrl = imageUrl.ifEmpty { "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400" },
                        description = desc,
                        badges = badges,
                        sortOrder = _qrMenuItems.value.size
                    )
                    _qrMenuItems.update { it + newItem }
                } else {
                    val body = mapOf(
                        "bot_id" to bot.id,
                        "name" to name,
                        "price" to price,
                        "description" to desc,
                        "category_id" to categoryId,
                        "badges" to badges,
                        "image_url" to imageUrl
                    ).filterValues { it != null } as Map<String, Any>
                    val created = apiService?.createQRMenuItem(body)
                    if (created != null) {
                        _qrMenuItems.update { it + created }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add QR Menu item"
            }
        }
    }

    fun addQRTable(number: String) {
        val newId = (_qrTables.value.map { it.id }.maxOrNull() ?: 0) + 1
        val table = QRTable(newId, number, "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=Table_$number")
        _qrTables.update { it + table }
    }

    fun updateQROrderStatus(orderId: Int, status: String) {
        _qrOrders.update { list ->
            list.map { if (it.id == orderId) it.copy(status = status) else it }
        }
    }

    // 13. Customers ban/unban toggle
    fun toggleCustomerBan(customerId: Int, isTelegram: Boolean) {
        viewModelScope.launch {
            try {
                if (isTelegram) {
                    _telegramUsers.update { list ->
                        list.map {
                            if (it.id == customerId) {
                                val state = !it.banned
                                if (!_isDemoMode.value) {
                                    apiService?.updateTelegramUser(customerId, mapOf("banned" to state))
                                }
                                it.copy(banned = state)
                            } else it
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "toggleCustomerBan error", e)
            }
        }
    }

    // 14. Coupon Validation
    fun validateDiscountCode(code: String) {
        viewModelScope.launch {
            if (code.lowercase() == "discount50") {
                _discountValidationMessage.value = "Success! Promo code applied: 50% subscription discount."
            } else {
                _discountValidationMessage.value = "Error: Invalid discount code."
            }
            delay(3000)
            _discountValidationMessage.value = null
        }
    }

    // --- Helpers ---
    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
    }

    private fun loadMockData() {
        val today = getTodayString()

        // 1. Bots List
        val demoBot = Bot(
            id = 1,
            botUsername = "CrossMartBot",
            botName = "CrossMart - Supermarket Shop",
            profilePicture = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400",
            currency = "USD",
            planName = "pro",
            planExpiry = "2026-12-31T00:00:00Z",
            planType = "yearly",
            aiEnabled = true,
            aiPrompt = "You are CrossMart AI, a polite customer support assistant for CrossMart. Answer product questions nicely.",
            aiModel = "gpt-4o-mini",
            aiTemperature = 0.7f,
            aiMaxTokens = 250
        )
        val devBot = Bot(
            id = 2,
            botUsername = "CrossMartVegBot",
            botName = "CrossMart Organics",
            profilePicture = "https://images.unsplash.com/photo-1574316071802-0d684efa7bf5?w=400",
            currency = "MMK",
            planName = "business",
            planExpiry = "2027-06-30T00:00:00Z",
            planType = "yearly"
        )
        _botsList.value = listOf(demoBot, devBot)
        _currentBot.value = demoBot

        // 2. Dashboard Stats Summary
        _statsSummary.value = mapOf(
            "total_revenue" to 582400.0,
            "total_orders" to 48,
            "total_users" to 142,
            "pending_orders" to 4,
            "today_revenue" to 34200.0,
            "monthly_revenue" to 194500.0,
            "items_sold" to 92,
            "products_sold" to 18
        )

        // 3. Orders By Day (for dynamic charts)
        _ordersByDay.value = listOf(
            mapOf("day" to "07/01", "revenue" to 42000.0, "count" to 4, "profit" to 15000.0),
            mapOf("day" to "07/02", "revenue" to 38000.0, "count" to 3, "profit" to 12000.0),
            mapOf("day" to "07/03", "revenue" to 61000.0, "count" to 5, "profit" to 22000.0),
            mapOf("day" to "07/04", "revenue" to 45000.0, "count" to 4, "profit" to 14000.0),
            mapOf("day" to "07/05", "revenue" to 78000.0, "count" to 8, "profit" to 29000.0),
            mapOf("day" to "07/06", "revenue" to 92000.0, "count" to 11, "profit" to 35000.0),
            mapOf("day" to "07/07", "revenue" to 34200.0, "count" to 6, "profit" to 13000.0)
        )

        // 4. Top Products Ranking List
        _topProducts.value = listOf(
            mapOf("name" to "Wireless Headphones Pro", "total_revenue" to 240000.0, "order_count" to 12, "total_profit" to 84000.0, "image_url" to "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400", "price" to 20000.0),
            mapOf("name" to "Premium Leather Wallet", "total_revenue" to 145000.0, "order_count" to 15, "total_profit" to 58000.0, "image_url" to "https://images.unsplash.com/photo-1627123424574-724758594e93?w=400", "price" to 9600.0),
            mapOf("name" to "Mechanical Gaming Keyboard", "total_revenue" to 110000.0, "order_count" to 7, "total_profit" to 33000.0, "image_url" to "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400", "price" to 15700.0),
            mapOf("name" to "Active Sports Water Bottle", "total_revenue" to 87400.0, "order_count" to 22, "total_profit" to 31000.0, "image_url" to "https://images.unsplash.com/photo-1602143407151-7111542de6e8?w=400", "price" to 3900.0)
        )

        // 5. Users registration summary
        _usersByDay.value = listOf(
            mapOf("day" to "07/01", "count" to 3),
            mapOf("day" to "07/02", "count" to 5),
            mapOf("day" to "07/03", "count" to 2),
            mapOf("day" to "07/04", "count" to 6),
            mapOf("day" to "07/05", "count" to 9),
            mapOf("day" to "07/06", "count" to 12),
            mapOf("day" to "07/07", "count" to 4)
        )

        // 6. Categories List (e-commerce catalog)
        _categories.value = listOf(
            Category(id = 1, botId = 1, name = "Electronics", emoji = "🔌", sortOrder = 0),
            Category(id = 2, botId = 1, name = "Fashion", emoji = "👕", sortOrder = 1),
            Category(id = 3, botId = 1, name = "Groceries", emoji = "🥦", sortOrder = 2),
            Category(id = 4, botId = 1, name = "Home Accessories", emoji = "🏠", sortOrder = 3)
        )

        // 7. Products List (pre-populated)
        _products.value = listOf(
            Product(
                id = 1, botId = 1, name = "Wireless Headphones Pro",
                description = "High-fidelity sound headphones with dynamic bass, ANC (Active Noise Cancelling), and 40h battery life.",
                price = 120.0, costPrice = 75.0, stockQuantity = 32,
                imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400",
                categoryId = 1, variants = listOf(Variant("Color", listOf("Black", "Silver", "Navy"))),
                sortOrder = 0, isActive = true
            ),
            Product(
                id = 2, botId = 1, name = "Premium Leather Wallet",
                description = "Handcrafted genuine full-grain leather bifold wallet with 8 card slots and RFID protection.",
                price = 45.0, costPrice = 22.0, stockQuantity = 85,
                imageUrl = "https://images.unsplash.com/photo-1627123424574-724758594e93?w=400",
                categoryId = 2, variants = listOf(Variant("Style", listOf("Classic Brown", "Midnight Black"))),
                sortOrder = 1, isActive = true
            ),
            Product(
                id = 3, botId = 1, name = "Mechanical Keyboard",
                description = "RGB Backlit mechanical keyboard with tactile brown switches, robust aluminum frame, and customizable keys.",
                price = 89.0, costPrice = 48.0, stockQuantity = 14,
                imageUrl = "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=400",
                categoryId = 1, variants = emptyList(), sortOrder = 2, isActive = true
            ),
            Product(
                id = 4, botId = 1, name = "Organic Apple Pack",
                description = "A bag of 6 freshly picked organic red delicious apples from state orchards, pesticide-free.",
                price = 6.99, costPrice = 3.20, stockQuantity = 120,
                imageUrl = "https://images.unsplash.com/photo-1619546813926-a78fa6372cd2?w=400",
                categoryId = 3, variants = emptyList(), sortOrder = 3, isActive = true
            )
        )

        // 8. Orders List (rich simulation entries, pending/processing/COD etc)
        val snapshotJohn = BuyerSnapshot("John Smith", "0945001234", "john@gmail.com", "No 14, Pyay Road, Yangon", "@john_smith", "0945001234", "Deliver after 5 PM", "fb-uid-112")
        val infoJohn = CustomerInfo(101, "John Smith", "@john_smith", 123456789L, "0945001234", "john@gmail.com")

        val order1 = Order(
            id = 2001, orderNumber = "CM-ORD-8821", invoiceNumber = "INV-2026-001",
            status = "pending", totalAmount = 165.0, deliveryFee = 5.0, paymentMethod = "kpay",
            userId = 101, botId = 1,
            items = listOf(
                OrderItem(productId = 1, name = "Wireless Headphones Pro", quantity = 1, price = 120.0, variant = "Black", variantLabel = "Color"),
                OrderItem(productId = 2, name = "Premium Leather Wallet", quantity = 1, price = 45.0, variant = "Classic Brown", variantLabel = "Style")
            ),
            buyerSnapshot = snapshotJohn, customer = infoJohn,
            paymentProofMessages = listOf("proof_msg_id_1092"), couponCode = "WELCOME10", couponDiscount = 5.0,
            createdAt = today
        )

        val snapshotMary = BuyerSnapshot("Mary Su", "0951112223", "mary@website.com", "Apartment 12B, Park Avenue, NY", null, null, "Knock the door", "fb-uid-113")
        val order2 = Order(
            id = 2002, orderNumber = "CM-ORD-8822", invoiceNumber = "INV-2026-002",
            status = "confirmed", totalAmount = 89.0, deliveryFee = 0.0, paymentMethod = "credit_card",
            userId = null, botId = 1,
            items = listOf(OrderItem(productId = 3, name = "Mechanical Keyboard", quantity = 1, price = 89.0)),
            buyerSnapshot = snapshotMary, customer = null,
            createdAt = today
        )

        val snapshotCOD = BuyerSnapshot("Aung Aung", "0978012345", "aung@gmail.com", "Building C, Penthouse, Mandalay", "@aung_aung", null, "Call on arrival", null)
        val order3 = Order(
            id = 2003, orderNumber = "CM-ORD-COD-01", invoiceNumber = null,
            status = "processing", totalAmount = 20.97, deliveryFee = 2.0, paymentMethod = "COD",
            userId = 102, botId = 1,
            items = listOf(OrderItem(productId = 4, name = "Organic Apple Pack", quantity = 3, price = 6.99)),
            buyerSnapshot = snapshotCOD, customer = CustomerInfo(102, "Aung Aung", "@aung_aung", 987654321L, "0978012345", "aung@gmail.com"),
            createdAt = today
        )

        _orders.value = listOf(order1, order2, order3)

        // 9. Customers List
        _telegramUsers.value = listOf(
            Customer(id = 101, telegramId = 123456789L, firstName = "John Smith", username = "@john_smith", phoneNumber = "0945001234", email = "john@gmail.com", photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100", orderCount = 4, totalSpent = 380.0, pointsBalance = 240, banned = false, notes = "Likes discounts"),
            Customer(id = 102, telegramId = 987654321L, firstName = "Aung Aung", username = "@aung_aung", phoneNumber = "0978012345", email = "aung@gmail.com", photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100", orderCount = 2, totalSpent = 41.94, pointsBalance = 40, banned = false, notes = "Loyal Telegram shopper")
        )
        _websiteCustomers.value = listOf(
            WebsiteCustomer(id = 301, botId = 1, firebaseUid = "fb-uid-113", name = "Mary Su", email = "mary@website.com", phone = "0951112223", photoUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100", address = "Apartment 12B, NY", pointsBalance = 150, totalPointsEarned = 150, totalOrders = 3, totalSpent = 178.0)
        )

        // 10. Chats List & Messages (for customer replies)
        _chats.value = listOf(
            Chat(userId = 101, firstName = "John Smith", username = "@john_smith", photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100", lastMessage = "When will my headphone order arrive?", lastMessageType = "text", unreadCount = 2, lastMessageAt = today),
            Chat(userId = 102, firstName = "Aung Aung", username = "@aung_aung", photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100", lastMessage = "Thanks for the fast delivery!", lastMessageType = "text", unreadCount = 0, lastMessageAt = today)
        )

        _messages.value = mapOf(
            101 to listOf(
                Message("m1", "Hello! Is the apple pack in stock?", "user", date = today),
                Message("m2", "Yes, John! We have 100+ packs in stock.", "bot", date = today),
                Message("m3", "When will my headphone order arrive?", "user", date = today)
            ),
            102 to listOf(
                Message("m4", "Your COD Apple delivery is dispatched Aung Aung.", "bot", date = today),
                Message("m5", "Thanks for the fast delivery!", "user", date = today)
            )
        )

        // 11. Custom Commands
        _customCommands.value = listOf(
            CustomCommand(1, "promo", "Show promotional offers", "Check out our shop and use coupon CODE: PROMO20 for 20% discount on entire cart!", null, null, true),
            CustomCommand(2, "hours", "View business hours", "We are open 24/7. Live chat is active from 9 AM to 10 PM daily.", null, null, true)
        )

        // 12. Newsfeed Posts
        _newsPosts.value = listOf(
            NewsPost(
                id = 501, botId = 1, content = "⚡ Super Sales Event starting this weekend! Use the exclusive promo codes inside your Telegram Bot and enjoy massive cashbacks.",
                imageUrl = "https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=400",
                topic = "promotion", createdAt = today, likes = 12,
                comments = listOf(Comment(1, 501, "u-1", "Mg Mg", "Awesome discounts!", today))
            )
        )

        // 13. FAQs List
        _faqs.value = listOf(
            FAQ(1, "How do I make a payment?", "You can pay via KPay, WavePay, AYA Pay, Card, or Cash on Delivery.", 0),
            FAQ(2, "Can I track my order status?", "Yes! Enter your phone number or order number under the 'Track Order' option in your Bot to see real-time status.", 1)
        )

        // 14. Staff Accounts
        _staffAccounts.value = listOf(
            Staff(1, "Zarni Aung", "zarni_admin", 1, StaffPermissions(dashboard = true, orders = true, products = true, customers = true, chats = true, staff = false)),
            Staff(2, "Khin Khin", "khinkhin_staff", 1, StaffPermissions(dashboard = false, orders = true, chats = true, products = false, staff = false))
        )

        // 15. Activity logs (mocked audit logs)
        _activityLogs.value = listOf(
            ActivityLog(1001, 1, "LOGIN", "Staff logged in from Yangon IP", today),
            ActivityLog(1002, 1, "UPDATE_ORDER", "Updated Order CM-ORD-8822 to CONFIRMED", today)
        )

        // 16. Broadcasts & Giveaways
        _broadcasts.value = listOf(
            Broadcast(1, 1, "Big discounts are live! Check the catalogue now.", "https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=400", "Shop Now", "https://t.me/crossmart_bot", today, "all", "sent", 142, today)
        )
        _giveaways.value = listOf(
            Giveaway(1, 1, "iPhone 17 Pro Max Giveaway", "Celebrate our shop launch! Enter the giveaway via the command in bot.", 1, "In 2 Days", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400", 35, "active", emptyList())
        )

        // 17. Payment Methods List
        _paymentMethods.value = listOf(
            PaymentMethod(1, "kpay", "U Aung San", "0945001234", "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=KBZPay_Account", true),
            PaymentMethod(2, "wavepay", "Daw Khin Myint", "0978012345", "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=WavePay_Account", true)
        )

        // 18. QR Menu Items & Orders
        _qrMenuCategories.value = listOf(
            QRMenuCategory(1, 1, "Breakfast", 0),
            QRMenuCategory(2, 1, "Beverages", 1),
            QRMenuCategory(3, 1, "Desserts", 2)
        )
        _qrMenuItems.value = listOf(
            QRMenuItem(1, 1, "Classic Beef Burger", 8.5, 1, "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400", "Juicy prime beef patty with cheddar cheese, lettuce, tomatoes, and home sauce.", listOf("popular"), 0),
            QRMenuItem(2, 1, "Spicy Chicken Wings", 6.0, 1, "https://images.unsplash.com/photo-1567620832903-9fc6debc209f?w=400", "Crispy deep-fried wings coated in fiery hot buffalo sauce.", listOf("spicy", "popular"), 1),
            QRMenuItem(3, 1, "Iced Latte Macchiato", 4.2, 2, "https://images.unsplash.com/photo-1517701604599-bb29b565090c?w=400", "Rich espresso shots layered with creamy chilled milk and sweetener.", listOf("new"), 2)
        )
        _qrTables.value = listOf(
            QRTable(1, "01", "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=Table_01"),
            QRTable(2, "02", "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=Table_02"),
            QRTable(3, "03", "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=Table_03")
        )
        _qrOrders.value = listOf(
            QROrder(5001, "QR-9901", listOf(OrderItem(1, "Classic Beef Burger", 2, 8.5)), "01", "0945009876", 17.0, "pending", today),
            QROrder(5002, "QR-9902", listOf(OrderItem(3, "Iced Latte Macchiato", 1, 4.2)), "03", "0951114444", 4.2, "delivered", today)
        )
    }
}
