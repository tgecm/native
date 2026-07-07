package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Bot(
    val id: Int,
    @Json(name = "bot_username") val botUsername: String,
    @Json(name = "bot_name") val botName: String? = null,
    @Json(name = "profile_picture") val profilePicture: String? = null,
    val currency: String = "MMK",
    @Json(name = "plan_name") val planName: String = "free",
    @Json(name = "plan_expiry") val planExpiry: String? = null,
    @Json(name = "plan_type") val planType: String? = null,
    @Json(name = "ai_enabled") val aiEnabled: Boolean = false,
    @Json(name = "ai_prompt") val aiPrompt: String? = null,
    @Json(name = "ai_model") val aiModel: String? = null,
    @Json(name = "ai_temperature") val aiTemperature: Float? = null,
    @Json(name = "ai_max_tokens") val aiMaxTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class Order(
    val id: Int,
    @Json(name = "order_number") val orderNumber: String?,
    @Json(name = "invoice_number") val invoiceNumber: String?,
    val status: String, // pending, confirmed, processing, shipped, delivered, cancelled, rejected, etc.
    @Json(name = "total_amount") val totalAmount: Double,
    @Json(name = "delivery_fee") val deliveryFee: Double = 0.0,
    @Json(name = "payment_method") val paymentMethod: String,
    @Json(name = "user_id") val userId: Int?,
    @Json(name = "bot_id") val botId: Int,
    val items: List<OrderItem> = emptyList(),
    @Json(name = "buyer_snapshot") val buyerSnapshot: BuyerSnapshot?,
    val customer: CustomerInfo?,
    @Json(name = "payment_proof_messages") val paymentProofMessages: List<String> = emptyList(),
    @Json(name = "coupon_code") val couponCode: String? = null,
    @Json(name = "coupon_discount") val couponDiscount: Double = 0.0,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class OrderItem(
    @Json(name = "product_id") val productId: Int,
    val name: String,
    val quantity: Int,
    val price: Double,
    val variant: String? = null,
    @Json(name = "variant_label") val variantLabel: String? = null
)

@JsonClass(generateAdapter = true)
data class BuyerSnapshot(
    val name: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    @Json(name = "telegram_username") val telegramUsername: String?,
    @Json(name = "viber_number") val viberNumber: String?,
    val notes: String?,
    @Json(name = "firebase_uid") val firebaseUid: String?
)

@JsonClass(generateAdapter = true)
data class CustomerInfo(
    val id: Int,
    @Json(name = "first_name") val firstName: String?,
    val username: String?,
    @Json(name = "telegram_id") val telegramId: Long?,
    @Json(name = "phone_number") val phoneNumber: String?,
    val email: String?
)

@JsonClass(generateAdapter = true)
data class Product(
    val id: Int,
    @Json(name = "bot_id") val botId: Int,
    val name: String,
    val description: String?,
    val price: Double,
    @Json(name = "cost_price") val costPrice: Double?,
    @Json(name = "stock_quantity") val stockQuantity: Int?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "category_id") val categoryId: Int?,
    val variants: List<Variant> = emptyList(),
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Variant(
    val name: String,
    val options: List<String>
)

@JsonClass(generateAdapter = true)
data class Category(
    val id: Int,
    @Json(name = "bot_id") val botId: Int,
    val name: String,
    val emoji: String?,
    @Json(name = "sort_order") val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class Customer(
    val id: Int,
    @Json(name = "telegram_id") val telegramId: Long?,
    @Json(name = "first_name") val firstName: String?,
    val username: String?,
    @Json(name = "phone_number") val phoneNumber: String?,
    val email: String?,
    @Json(name = "photo_url") val photoUrl: String?,
    @Json(name = "order_count") val orderCount: Int = 0,
    @Json(name = "total_spent") val totalSpent: Double = 0.0,
    @Json(name = "points_balance") val pointsBalance: Int = 0,
    val banned: Boolean = false,
    val notes: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class WebsiteCustomer(
    val id: Int,
    @Json(name = "bot_id") val botId: Int,
    @Json(name = "firebase_uid") val firebaseUid: String,
    val name: String?,
    val email: String?,
    val phone: String?,
    @Json(name = "photo_url") val photoUrl: String?,
    val address: String? = null,
    @Json(name = "points_balance") val pointsBalance: Int = 0,
    @Json(name = "total_points_earned") val totalPointsEarned: Int = 0,
    @Json(name = "total_orders") val totalOrders: Int = 0,
    @Json(name = "total_spent") val totalSpent: Double = 0.0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Chat(
    @Json(name = "user_id") val userId: Int,
    @Json(name = "first_name") val firstName: String?,
    val username: String?,
    @Json(name = "photo_url") val photoUrl: String?,
    @Json(name = "last_message") val lastMessage: String?,
    @Json(name = "last_message_type") val lastMessageType: String?, // text, photo, video, etc.
    @Json(name = "unread_count") val unreadCount: Int = 0,
    @Json(name = "last_message_at") val lastMessageAt: String?
)

@JsonClass(generateAdapter = true)
data class Message(
    val id: String,
    val text: String?,
    val sender: String, // user, bot
    @Json(name = "file_id") val fileId: String? = null,
    @Json(name = "file_type") val fileType: String? = null, // photo, video, etc.
    val date: String
)

@JsonClass(generateAdapter = true)
data class PaymentMethod(
    val id: Int,
    val type: String, // kpay, wavepay, aya_pay, cbpay, credit_card
    @Json(name = "account_name") val accountName: String?,
    @Json(name = "account_number") val accountNumber: String?,
    @Json(name = "qr_image_url") val qrImageUrl: String?,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Coupon(
    val id: Int,
    val code: String,
    @Json(name = "discount_type") val discountType: String, // percentage, fixed
    @Json(name = "discount_value") val discountValue: Double,
    @Json(name = "end_date") val endDate: String?,
    @Json(name = "total_coupons") val totalCoupons: Int?,
    @Json(name = "min_spend") val minSpend: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class Broadcast(
    val id: Int,
    @Json(name = "bot_id") val botId: Int,
    val message: String,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "button_text") val buttonText: String?,
    @Json(name = "button_url") val buttonUrl: String?,
    @Json(name = "schedule_at") val scheduleAt: String?,
    val target: String, // all, active, inactive
    val status: String? = "pending", // pending, sent
    @Json(name = "target_count") val targetCount: Int? = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Giveaway(
    val id: Int,
    @Json(name = "bot_id") val botId: Int,
    val prize: String,
    val description: String?,
    @Json(name = "winner_count") val winnerCount: Int = 1,
    @Json(name = "end_date") val endDate: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "participant_count") val participantCount: Int = 0,
    val status: String = "active", // active, drawn
    val winners: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CustomCommand(
    val id: Int,
    val command: String,
    val description: String?,
    val reply: String,
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "media_type") val mediaType: String? = null, // photo, video
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class NewsPost(
    val id: Int,
    @Json(name = "bot_id") val botId: Int,
    val content: String,
    @Json(name = "image_url") val imageUrl: String?,
    val topic: String, // general, announcement, promotion
    @Json(name = "created_at") val createdAt: String,
    val likes: Int = 0,
    val comments: List<Comment> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Comment(
    val id: Int,
    @Json(name = "post_id") val postId: Int,
    @Json(name = "visitor_id") val visitorId: String,
    @Json(name = "visitor_name") val visitorName: String,
    val content: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class FAQ(
    val id: Int,
    val question: String,
    val answer: String,
    @Json(name = "sort_order") val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class Staff(
    val id: Int,
    val name: String,
    val username: String,
    @Json(name = "bot_id") val botId: Int,
    val permissions: StaffPermissions = StaffPermissions()
)

@JsonClass(generateAdapter = true)
data class StaffPermissions(
    val dashboard: Boolean = true,
    val orders: Boolean = true,
    val products: Boolean = true,
    val customers: Boolean = true,
    val chats: Boolean = true,
    val newsfeed: Boolean = true,
    val payments: Boolean = true,
    val settings: Boolean = true,
    val broadcast: Boolean = true,
    val commands: Boolean = true,
    val staff: Boolean = false,
    val faqs: Boolean = true
)

@JsonClass(generateAdapter = true)
data class ActivityLog(
    val id: Int,
    @Json(name = "staff_id") val staffId: Int,
    val action: String,
    val details: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class QRMenuItem(
    val id: Int,
    @Json(name = "bot_id") val botId: Int,
    val name: String,
    val price: Double,
    @Json(name = "category_id") val categoryId: Int?,
    @Json(name = "image_url") val imageUrl: String?,
    val description: String?,
    val badges: List<String> = emptyList(), // spicy, vegetarian, popular, new
    @Json(name = "sort_order") val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class QRMenuCategory(
    val id: Int,
    @Json(name = "bot_id") val botId: Int,
    val name: String,
    @Json(name = "sort_order") val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class QRTable(
    val id: Int,
    val number: String,
    val qrCodeUrl: String
)

@JsonClass(generateAdapter = true)
data class QROrder(
    val id: Int,
    @Json(name = "order_number") val orderNumber: String,
    val items: List<OrderItem> = emptyList(),
    val tableNumber: String,
    val customerPhone: String,
    val totalAmount: Double,
    val status: String, // pending, confirmed, delivered, etc.
    val createdAt: String
)
