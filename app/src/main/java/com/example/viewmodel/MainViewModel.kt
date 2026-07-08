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
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _token = MutableStateFlow("")
    val token: StateFlow<String> = _token.asStateFlow()

    private val _currentUserEmail = MutableStateFlow("")
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

    // --- 2FA Polling Job ---
    private var pollingJob: Job? = null

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
        initRetrofit()
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

    // --- 2FA Polling ---
    fun stopPollingLogin() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun startPollingLogin(loginToken: String, isStaff: Boolean, onApproved: () -> Unit) {
        stopPollingLogin()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                try {
                    val api = apiService ?: continue
                    val response = api.pollLoginApproval(loginToken)
                    if (response["success"] == true || response["approved"] == true) {
                        val jwt = response["token"] as? String ?: ""
                        _token.value = jwt
                        _isLoggedIn.value = true
                        _isStaffUser.value = isStaff
                        refreshAllData()
                        onApproved()
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Poll 2FA error: ${e.message}")
                }
            }
        }
    }

    // --- Login & Authentication ---
    fun loginOwner(email: String, password: String, onStep2FA: (String) -> Unit, onAutoLogin: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val api = apiService
                if (api != null) {
                    val response = api.loginOwner(mapOf("email" to email, "password" to password))
                    val loginToken = response["login_token"] ?: "temp-token"
                    _currentUserEmail.value = email
                    _isStaffUser.value = false
                    onStep2FA(loginToken)
                    startPollingLogin(loginToken, false, onAutoLogin)
                } else {
                    _errorMessage.value = "Retrofit not initialized"
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
            stopPollingLogin()
            try {
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
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Verification failed"
                Log.e(tag, "verifyOwnerLogin error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loginStaff(username: String, password: String, onStep2FA: (String) -> Unit, onAutoLogin: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val api = apiService
                if (api != null) {
                    val response = api.loginStaff(mapOf("username" to username, "password" to password))
                    val loginToken = response["login_token"] as? String ?: "temp-token"
                    _currentUserEmail.value = "$username@staff.crossmart.com"
                    _isStaffUser.value = true
                    onStep2FA(loginToken)
                    startPollingLogin(loginToken, true, onAutoLogin)
                } else {
                    _errorMessage.value = "Retrofit not initialized"
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
            stopPollingLogin()
            try {
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
                } else {
                    _errorMessage.value = "Retrofit not initialized"
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
        stopPollingLogin()
    }

    fun selectBot(bot: Bot) {
        _currentBot.value = bot
        refreshAllData()
    }

    // --- Live Data Syncing / Pull-to-refresh ---
    fun refreshAllData() {
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
                _errorMessage.value = "Failed to sync remote data."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startAutoRefresh() {
        stopAutoRefresh()

        statsRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30000)
                val bot = _currentBot.value
                if (bot != null) {
                    try {
                        syncStats(bot.id)
                    } catch (e: Exception) {
                        Log.e(tag, "Auto stats refresh fail: ${e.message}")
                    }
                }
            }
        }

        ordersRefreshJob = viewModelScope.launch {
            while (true) {
                delay(15000)
                val bot = _currentBot.value
                if (bot != null) {
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
                if (bot != null) {
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

    // --- CRUD Actions ---

    // 1. Orders
    fun updateOrderStatus(orderId: Int, status: String) {
        viewModelScope.launch {
            try {
                val api = apiService
                if (api != null) {
                    val updated = api.updateOrderStatus(orderId, mapOf("status" to status))
                    _orders.update { current ->
                        current.map { if (it.id == orderId) updated else it }
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
            } catch (e: Exception) {
                _errorMessage.value = "Failed to edit product: ${e.message}"
            }
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            try {
                val api = apiService
                if (api != null) {
                    api.deleteProduct(productId)
                    _products.update { current -> current.filter { it.id != productId } }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete product: ${e.message}"
            }
        }
    }

    fun reorderProducts(items: List<Product>) {
        _products.value = items
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

    // 3. Category CRUD
    fun addCategory(name: String, emoji: String) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
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
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add category: ${e.message}"
            }
        }
    }

    fun updateCategory(categoryId: Int, name: String, emoji: String) {
        viewModelScope.launch {
            try {
                val api = apiService
                if (api != null) {
                    val updated = api.updateCategory(categoryId, mapOf("name" to name, "emoji" to emoji))
                    _categories.update { current ->
                        current.map { if (it.id == categoryId) updated else it }
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
                apiService?.deleteCategory(categoryId)
                _categories.update { current -> current.filter { it.id != categoryId } }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete category: ${e.message}"
            }
        }
    }

    // 4. Chats & Thread management
    fun getMessagesForUser(userId: Int) {
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

                _messages.update { current ->
                    val userMsgs = (current[userId] ?: emptyList()) + newMsg
                    current.toMutableMap().apply { put(userId, userMsgs) }
                }

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
            } catch (e: Exception) {
                Log.e(tag, "sendChatMessage error", e)
            }
        }
    }

    fun markChatRead(userId: Int) {
        _chats.update { list ->
            list.map { if (it.userId == userId) it.copy(unreadCount = 0) else it }
        }
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                apiService?.markChatRead(userId, mapOf("bot_id" to bot.id))
            } catch (e: Exception) {
                Log.e(tag, "markChatRead fail", e)
            }
        }
    }

    fun deleteChat(userId: Int) {
        _chats.update { current -> current.filter { it.userId != userId } }
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                apiService?.deleteChat(userId, bot.id)
            } catch (e: Exception) {
                Log.e(tag, "deleteChat fail", e)
            }
        }
    }

    // 5. Payment Methods & COD
    fun addPaymentMethod(type: String, accountName: String, accountNumber: String, qrImageUrl: String) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
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
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add payment method"
            }
        }
    }

    fun updatePaymentMethodStatus(id: Int, isActive: Boolean) {
        viewModelScope.launch {
            try {
                val updated = apiService?.updatePaymentMethod(id, mapOf("is_active" to isActive))
                if (updated != null) {
                    _paymentMethods.update { current ->
                        current.map { if (it.id == id) updated else it }
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
                apiService?.deletePaymentMethod(id)
                _paymentMethods.update { current -> current.filter { it.id != id } }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete payment"
            }
        }
    }

    fun updateCodSettings(enabled: Boolean, deliveryFee: Double) {
        _codEnabled.value = enabled
        _codDeliveryFee.value = deliveryFee
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
                apiService?.updateCodSettings(bot.id, mapOf("enabled" to enabled, "delivery_fee" to deliveryFee))
            } catch (e: Exception) {
                Log.e(tag, "updateCodSettings fail", e)
            }
        }
    }

    // 6. FAQs
    fun addFAQ(question: String, answer: String) {
        viewModelScope.launch {
            try {
                val created = apiService?.createFAQ(mapOf("question" to question, "answer" to answer))
                if (created != null) {
                    _faqs.update { it + created }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add FAQ"
            }
        }
    }

    fun updateFAQ(id: Int, question: String, answer: String) {
        viewModelScope.launch {
            try {
                val updated = apiService?.updateFAQ(id, mapOf("question" to question, "answer" to answer))
                if (updated != null) {
                    _faqs.update { current -> current.map { if (it.id == id) updated else it } }
                }
            } catch (e: Exception) {
                Log.e(tag, "updateFAQ error", e)
            }
        }
    }

    fun deleteFAQ(id: Int) {
        viewModelScope.launch {
            try {
                apiService?.deleteFAQ(id)
                _faqs.update { current -> current.filter { it.id != id } }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete FAQ"
            }
        }
    }

    fun reorderFAQs(items: List<FAQ>) {
        _faqs.value = items
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

    // 7. Custom Commands
    fun addCustomCommand(command: String, description: String, reply: String, mediaUrl: String? = null, mediaType: String? = null) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
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
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create custom command"
            }
        }
    }

    fun deleteCustomCommand(id: Int) {
        viewModelScope.launch {
            try {
                apiService?.deleteCustomCommand(id)
                _customCommands.update { current -> current.filter { it.id != id } }
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
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create newsfeed post"
            }
        }
    }

    fun deleteNewsPost(id: Int) {
        viewModelScope.launch {
            try {
                apiService?.deleteNewsfeedPost(id)
                _newsPosts.update { current -> current.filter { it.id != id } }
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
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create staff account"
            }
        }
    }

    fun deleteStaff(id: Int, ownerPass: String) {
        viewModelScope.launch {
            try {
                apiService?.deleteStaff(id, mapOf("owner_password" to ownerPass))
                _staffAccounts.update { current -> current.filter { it.id != id } }
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
            } catch (e: Exception) {
                _errorMessage.value = "Failed to send broadcast"
            }
        }
    }

    fun createGiveaway(prize: String, description: String?, winnerCount: Int, endDate: String?, imageUrl: String?) {
        viewModelScope.launch {
            try {
                val bot = _currentBot.value ?: return@launch
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
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create giveaway"
            }
        }
    }

    fun drawGiveawayWinners(giveawayId: Int, method: String, count: Int, onComplete: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService?.drawGiveaway(giveawayId, mapOf("method" to method, "count" to count)) ?: emptyList()
                _giveaways.update { current ->
                    current.map {
                        if (it.id == giveawayId) {
                            it.copy(status = "drawn", winners = response)
                        } else it
                    }
                }
                onComplete(response)
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
                val api = apiService
                if (api != null) {
                    val updated = api.updateBot(bot.id, mapOf("bot_name" to name, "currency" to currency, "profile_picture" to profilePic))
                    _currentBot.value = updated
                    _botsList.update { list ->
                        list.map { if (it.id == bot.id) updated else it }
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
                                apiService?.updateTelegramUser(customerId, mapOf("banned" to state))
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
}
