package com.example.data.api

import com.example.data.model.*
import retrofit2.http.*

interface CrossMartApiService {

    // --- Authentication ---
    @POST("auth/login")
    suspend fun loginOwner(@Body body: Map<String, String>): Map<String, String>

    @GET("auth/login/poll")
    suspend fun pollLoginApproval(@Query("login_token") token: String): Map<String, Any>

    @POST("auth/login/verify")
    suspend fun verifyOwnerLogin(@Body body: Map<String, String>): Map<String, Any>

    @POST("auth/staff-login")
    suspend fun loginStaff(@Body body: Map<String, String>): Map<String, Any>

    @POST("auth/staff-login/verify")
    suspend fun verifyStaffLogin(@Body body: Map<String, String>): Map<String, Any>

    @GET("me")
    suspend fun getCurrentUser(): Map<String, Any>

    // --- Bots ---
    @GET("bots")
    suspend fun listBots(): List<Bot>

    @PATCH("bots/{id}")
    suspend fun updateBot(@Path("id") botId: Int, @Body body: Map<String, Any>): Bot

    @GET("bots/{id}/ai-settings")
    suspend fun getAiSettings(@Path("id") botId: Int): Map<String, Any>

    @PUT("bots/{id}/ai-settings")
    suspend fun updateAiSettings(@Path("id") botId: Int, @Body body: Map<String, Any>): Map<String, Any>

    // --- Statistics ---
    @GET("stats")
    suspend fun getStatsSummary(
        @Query("bot_id") botId: Int,
        @Query("days") days: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Map<String, Any>

    @GET("stats/orders-by-day")
    suspend fun getOrdersByDay(
        @Query("bot_id") botId: Int,
        @Query("days") days: Int
    ): List<Map<String, Any>>

    @GET("stats/top-products")
    suspend fun getTopProducts(
        @Query("bot_id") botId: Int,
        @Query("limit") limit: Int = 10
    ): List<Map<String, Any>>

    @GET("stats/users-by-day")
    suspend fun getUsersByDay(
        @Query("bot_id") botId: Int,
        @Query("days") days: Int
    ): List<Map<String, Any>>

    // --- Orders ---
    @GET("orders")
    suspend fun listOrders(
        @Query("bot_id") botId: Int
    ): List<Order>

    @PATCH("orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: Int,
        @Body body: Map<String, String>
    ): Order

    @GET("orders/pending-count")
    suspend fun getPendingOrdersCount(
        @Query("bot_id") botId: Int
    ): Map<String, Int>

    @GET("cod-settings/{botId}")
    suspend fun getCodSettings(@Path("botId") botId: Int): Map<String, Any>

    @POST("cod-settings/{botId}")
    suspend fun updateCodSettings(@Path("botId") botId: Int, @Body body: Map<String, Any>): Map<String, Any>

    // --- Products & Categories ---
    @GET("products")
    suspend fun listProducts(@Query("bot_id") botId: Int): List<Product>

    @POST("products")
    suspend fun createProduct(@Body product: Map<String, Any?>): Product

    @PATCH("products/{id}")
    suspend fun updateProduct(@Path("id") productId: Int, @Body body: Map<String, Any?>): Product

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") productId: Int): Map<String, Any>

    @PUT("products/sort-order")
    suspend fun updateProductSortOrder(@Body body: Map<String, Any>): Map<String, Any>

    @GET("categories")
    suspend fun listCategories(@Query("bot_id") botId: Int): List<Category>

    @POST("categories")
    suspend fun createCategory(@Body body: Map<String, Any>): Category

    @PATCH("categories/{id}")
    suspend fun updateCategory(@Path("id") categoryId: Int, @Body body: Map<String, Any>): Category

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") categoryId: Int): Map<String, Any>

    // --- Customers ---
    @GET("users")
    suspend fun listTelegramUsers(@Query("bot_id") botId: Int): List<Customer>

    @PATCH("users/{id}")
    suspend fun updateTelegramUser(@Path("id") userId: Int, @Body body: Map<String, Any>): Customer

    @GET("website-customers/{botId}")
    suspend fun listWebsiteCustomers(@Path("botId") botId: Int): List<WebsiteCustomer>

    // --- Chats ---
    @GET("chats")
    suspend fun listChats(@Query("bot_id") botId: Int): List<Chat>

    @GET("chats/{userId}/messages")
    suspend fun getChatMessages(
        @Path("userId") userId: Int,
        @Query("bot_id") botId: Int
    ): List<Message>

    @POST("chats/{userId}/send")
    suspend fun sendChatMessage(
        @Path("userId") userId: Int,
        @Body body: Map<String, String?>
    ): Message

    @DELETE("chats/{userId}")
    suspend fun deleteChat(
        @Path("userId") userId: Int,
        @Query("bot_id") botId: Int
    ): Map<String, Any>

    @POST("chats/{userId}/mark-read")
    suspend fun markChatRead(@Path("userId") userId: Int, @Body body: Map<String, Int>): Map<String, Any>

    @POST("chats/{userId}/mark-unread")
    suspend fun markChatUnread(@Path("userId") userId: Int, @Body body: Map<String, Int>): Map<String, Any>

    @GET("chats/unread-count")
    suspend fun getChatsUnreadCount(@Query("bot_id") botId: Int): Map<String, Int>

    // --- Payment Methods ---
    @GET("payment-methods")
    suspend fun listPaymentMethods(@Query("bot_id") botId: Int): List<PaymentMethod>

    @POST("payment-methods")
    suspend fun createPaymentMethod(@Body body: Map<String, Any>): PaymentMethod

    @PATCH("payment-methods/{id}")
    suspend fun updatePaymentMethod(@Path("id") id: Int, @Body body: Map<String, Any>): PaymentMethod

    @DELETE("payment-methods/{id}")
    suspend fun deletePaymentMethod(@Path("id") id: Int): Map<String, Any>

    // --- Broadcast & Giveaways ---
    @GET("broadcasts")
    suspend fun listBroadcasts(@Query("bot_id") botId: Int): List<Broadcast>

    @POST("broadcasts")
    suspend fun createBroadcast(@Body body: Map<String, Any>): Broadcast

    @GET("giveaways")
    suspend fun listGiveaways(@Query("bot_id") botId: Int): List<Giveaway>

    @POST("giveaways")
    suspend fun createGiveaway(@Body body: Map<String, Any>): Giveaway

    @POST("giveaways/{id}/draw")
    suspend fun drawGiveaway(@Path("id") id: Int, @Body body: Map<String, Any>): List<String>

    // --- Custom Commands ---
    @GET("custom-commands")
    suspend fun listCustomCommands(@Query("bot_id") botId: Int): List<CustomCommand>

    @POST("custom-commands")
    suspend fun createCustomCommand(@Body body: Map<String, Any>): CustomCommand

    @DELETE("custom-commands/{id}")
    suspend fun deleteCustomCommand(@Path("id") id: Int): Map<String, Any>

    // --- Newsfeed ---
    @GET("newsfeed/posts")
    suspend fun listNewsfeedPosts(@Query("bot_id") botId: Int): List<NewsPost>

    @POST("newsfeed/posts")
    suspend fun createNewsfeedPost(@Body body: Map<String, Any>): NewsPost

    @DELETE("newsfeed/posts/{id}")
    suspend fun deleteNewsfeedPost(@Path("id") id: Int): Map<String, Any>

    // --- FAQs ---
    @GET("faqs")
    suspend fun listFAQs(): List<FAQ>

    @POST("faqs")
    suspend fun createFAQ(@Body body: Map<String, String>): FAQ

    @PUT("faqs/{id}")
    suspend fun updateFAQ(@Path("id") id: Int, @Body body: Map<String, String>): FAQ

    @DELETE("faqs/{id}")
    suspend fun deleteFAQ(@Path("id") id: Int): Map<String, Any>

    @PUT("faqs/reorder")
    suspend fun reorderFAQs(@Body body: Map<String, Any>): Map<String, Any>

    // --- QR Menu System ---
    @GET("qr-menu/{botId}")
    suspend fun listQRMenuItems(@Path("botId") botId: Int): List<QRMenuItem>

    @POST("qr-menu")
    suspend fun createQRMenuItem(@Body body: Map<String, Any>): QRMenuItem

    @GET("qr-menu/{botId}/categories")
    suspend fun listQRMenuCategories(@Path("botId") botId: Int): List<QRMenuCategory>

    @GET("qr-menu/{botId}/orders")
    suspend fun listQROrders(@Path("botId") botId: Int): List<QROrder>

    // --- Staff Accounts ---
    @GET("api/admin/staff")
    suspend fun listStaff(@Query("bot_id") botId: Int): List<Staff>

    @POST("api/admin/staff/create")
    suspend fun createStaff(@Body body: Map<String, Any>): Staff

    @DELETE("api/admin/staff/{id}")
    suspend fun deleteStaff(@Path("id") id: Int, @Body body: Map<String, String>): Map<String, Any>
}
