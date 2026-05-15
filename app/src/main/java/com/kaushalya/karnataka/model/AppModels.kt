package com.kaushalya.karnataka.model

data class AppUser(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val role: UserRole,
    val category: String = "",
    val bio: String = "",
    val location: String = "Karnataka",
    val avatarUrl: String = "",
    val avatarRes: Int = 0,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val fcmToken: String = ""
)

enum class UserRole { WORKER, CUSTOMER }

data class WorkerService(
    val id: String,
    val workerId: String,
    val title: String,
    val category: String,
    val price: String,
    val priceType: String = "fixed",
    val createdAt: Long = System.currentTimeMillis()
)

data class PortfolioPhoto(
    val id: String,
    val workerId: String,
    val imageRes: Int,
    val imageUri: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)

data class Review(
    val id: String,
    val workerId: String,
    val reviewerId: String,
    val reviewerName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class HireEvent(
    val id: String,
    val workerId: String,
    val customerId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "requested"
)

data class AppState(
    val users: List<AppUser> = emptyList(),
    val services: List<WorkerService> = emptyList(),
    val portfolio: List<PortfolioPhoto> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val hireEvents: List<HireEvent> = emptyList(),
    val currentUser: AppUser? = null,
    val isLoading: Boolean = false,
    val message: String? = null
) {
    val workers: List<AppUser> = users.filter { it.role == UserRole.WORKER }
}
