package com.kaushalya.karnataka.data

import android.content.Context
import com.kaushalya.karnataka.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.kaushalya.karnataka.model.AppState
import com.kaushalya.karnataka.model.AppUser
import com.kaushalya.karnataka.model.HireEvent
import com.kaushalya.karnataka.model.PortfolioPhoto
import com.kaushalya.karnataka.model.Review
import com.kaushalya.karnataka.model.UserRole
import com.kaushalya.karnataka.model.WorkerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class KaushalyaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("kaushalya_store", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<AppState> = _state

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun login(email: String, password: String, role: UserRole): Boolean {
        _state.update { it.copy(isLoading = true, message = null) }
        val user = _state.value.users.firstOrNull {
            it.email.equals(email.trim(), true) && it.password == password && it.role == role
        }
        return if (user != null) {
            prefs.edit().putString("sessionUserId", user.id).apply()
            _state.update { it.copy(currentUser = user, isLoading = false, message = "Welcome, ${user.name}") }
            true
        } else if (isFirebaseReady()) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid.orEmpty()
                    FirebaseFirestore.getInstance().collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            val firebaseUser = doc.toAppUser(uid, email.trim(), password, role)
                            upsertLoggedInUser(firebaseUser, "Welcome, ${firebaseUser.name}")
                        }
                        .addOnFailureListener {
                            val firebaseUser = AppUser(uid, email.trim().substringBefore("@").replaceFirstChar { char -> char.uppercase() }, email.trim(), "", password, role)
                            upsertLoggedInUser(firebaseUser, "Welcome, ${firebaseUser.name}")
                        }
                }
                .addOnFailureListener { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            message = error.localizedMessage ?: "No Firebase account found for this email. Register first, then login."
                        )
                    }
                }
            false
        } else {
            _state.update { it.copy(isLoading = false, message = "Firebase is not connected yet. Add app/google-services.json or use demo credentials.") }
            false
        }
    }

    fun register(name: String, email: String, phone: String, password: String, role: UserRole, category: String): Boolean {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.length < 6) {
            _state.update { it.copy(message = "Please complete all fields. Password needs 6+ characters.") }
            return false
        }
        if (role == UserRole.WORKER && category.isBlank()) {
            _state.update { it.copy(message = "Select a trade category") }
            return false
        }
        if (_state.value.users.any { it.email.equals(email.trim(), true) }) {
            _state.update { it.copy(message = "Email is already registered") }
            return false
        }
        val user = AppUser(
            id = "user_${UUID.randomUUID()}",
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            password = password,
            role = role,
            category = category,
            bio = if (role == UserRole.WORKER) "Skilled ${category.lowercase()} available for local jobs" else "",
            location = "Dharwad, Karnataka"
        )
        if (isFirebaseReady()) {
            _state.update { it.copy(isLoading = true, message = null) }
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email.trim(), password)
                .addOnSuccessListener { result ->
                    val firebaseUser = user.copy(id = result.user?.uid ?: user.id)
                    FirebaseFirestore.getInstance().collection("users").document(firebaseUser.id).set(firebaseUser.toFirestoreMap())
                        .addOnSuccessListener { upsertLoggedInUser(firebaseUser, "Account created") }
                        .addOnFailureListener { error ->
                            _state.update { it.copy(isLoading = false, message = error.localizedMessage ?: "Could not save profile") }
                        }
                }
                .addOnFailureListener { error ->
                    _state.update { it.copy(isLoading = false, message = error.localizedMessage ?: "Could not create Firebase account") }
                }
            return false
        } else {
            _state.update { it.copy(users = it.users + user, currentUser = user, message = "Account created") }
            prefs.edit().putString("sessionUserId", user.id).apply()
            saveState()
            return true
        }
    }

    fun logout() {
        prefs.edit().remove("sessionUserId").apply()
        _state.update { it.copy(currentUser = null, message = "Logged out") }
    }

    fun addOrUpdateService(serviceId: String?, title: String, category: String, price: String, priceType: String) {
        val worker = _state.value.currentUser ?: return
        if (title.isBlank() || category.isBlank() || price.isBlank()) {
            _state.update { it.copy(message = "Service title, category and price are required") }
            return
        }
        val service = WorkerService(
            id = serviceId ?: "service_${UUID.randomUUID()}",
            workerId = worker.id,
            title = title.trim(),
            category = category.trim(),
            price = price.trim(),
            priceType = priceType
        )
        _state.update { state ->
            state.copy(
                services = state.services.filterNot { it.id == service.id } + service,
                message = if (serviceId == null) "Service added" else "Service updated"
            )
        }
        saveState()
    }

    fun deleteService(serviceId: String) {
        _state.update { it.copy(services = it.services.filterNot { service -> service.id == serviceId }, message = "Service deleted") }
        saveState()
    }

    fun updateProfilePhoto(uri: String) {
        val user = _state.value.currentUser ?: return
        val updatedUser = user.copy(avatarUrl = uri)
        _state.update { state ->
            state.copy(
                users = state.users.map { if (it.id == user.id) updatedUser else it },
                currentUser = updatedUser,
                message = "Profile photo updated"
            )
        }
        saveState()
    }

    fun updateFcmToken(token: String) {
        val user = _state.value.currentUser ?: return
        if (user.fcmToken == token) return
        val updatedUser = user.copy(fcmToken = token)
        _state.update { state ->
            state.copy(
                users = state.users.map { if (it.id == user.id) updatedUser else it },
                currentUser = updatedUser
            )
        }
        if (isFirebaseReady()) {
            FirebaseFirestore.getInstance().collection("users").document(user.id)
                .update("fcmToken", token)
        }
        saveState()
    }

    fun addPortfolioPhoto(imageRes: Int? = null, imageUri: String = "") {
        val worker = _state.value.currentUser ?: return
        val pool = sampleImages
        val photo = PortfolioPhoto(
            id = "photo_${UUID.randomUUID()}",
            workerId = worker.id,
            imageRes = imageRes ?: pool.random(),
            imageUri = imageUri
        )
        _state.update { it.copy(portfolio = it.portfolio + photo, message = "Portfolio photo uploaded") }
        saveState()
    }

    fun hire(workerId: String) {
        val customer = _state.value.currentUser ?: return
        val worker = _state.value.users.firstOrNull { it.id == workerId } ?: return
        val event = HireEvent("hire_${UUID.randomUUID()}", workerId, customer.id)
        _state.update { it.copy(hireEvents = it.hireEvents + event, message = "Hire request sent to ${worker.name}") }
        saveState()
    }

    fun addReview(workerId: String, rating: Int, comment: String) {
        val reviewer = _state.value.currentUser ?: return
        if (rating == 0 || comment.isBlank()) {
            _state.update { it.copy(message = "Choose a star rating and write a short review") }
            return
        }
        val review = Review("review_${UUID.randomUUID()}", workerId, reviewer.id, reviewer.name, rating, comment.trim())
        _state.update { state ->
            val reviews = state.reviews + review
            val workerReviews = reviews.filter { it.workerId == workerId }
            val average = workerReviews.map { it.rating }.average()
            state.copy(
                reviews = reviews,
                users = state.users.map {
                    if (it.id == workerId) it.copy(averageRating = average, reviewCount = workerReviews.size) else it
                },
                message = "Review submitted"
            )
        }
        saveState()
    }

    private fun loadState(): AppState {
        val seeded = prefs.getBoolean("isMockDataSeeded", false)
        val saved = prefs.getString("state", null)
        val state = applyBundledImages(if (seeded && saved != null) decodeState(saved) else seedState())
        val sessionId = prefs.getString("sessionUserId", null)
        return state.copy(currentUser = state.users.firstOrNull { it.id == sessionId })
    }

    private fun saveState() {
        val state = _state.value.copy(currentUser = null, message = null, isLoading = false)
        prefs.edit()
            .putBoolean("isMockDataSeeded", true)
            .putString("state", encodeState(state))
            .apply()
    }

    private fun isFirebaseReady(): Boolean = try {
        FirebaseApp.getApps(appContext).isNotEmpty()
    } catch (_: Exception) {
        false
    }

    private fun upsertLoggedInUser(user: AppUser, message: String) {
        prefs.edit().putString("sessionUserId", user.id).apply()
        _state.update { state ->
            state.copy(
                users = state.users.filterNot { it.id == user.id || it.email.equals(user.email, true) } + user,
                currentUser = user,
                isLoading = false,
                message = message
            )
        }
        saveState()
    }

    private fun DocumentSnapshot.toAppUser(uid: String, fallbackEmail: String, fallbackPassword: String, fallbackRole: UserRole): AppUser {
        val roleName = getString("role") ?: fallbackRole.name.lowercase()
        return AppUser(
            id = uid,
            name = getString("name") ?: fallbackEmail.substringBefore("@"),
            email = getString("email") ?: fallbackEmail,
            phone = getString("phone") ?: "",
            password = fallbackPassword,
            role = if (roleName.equals("worker", true) || roleName.equals("WORKER", true)) UserRole.WORKER else UserRole.CUSTOMER,
            category = getString("category") ?: "",
            bio = getString("bio") ?: "",
            location = getString("location") ?: "Karnataka",
            avatarUrl = getString("avatarUrl") ?: "",
            averageRating = getDouble("averageRating") ?: 0.0,
            reviewCount = getLong("reviewCount")?.toInt() ?: 0,
            fcmToken = getString("fcmToken") ?: ""
        )
    }

    private fun AppUser.toFirestoreMap(): Map<String, Any> = mapOf(
        "name" to name,
        "email" to email,
        "phone" to phone,
        "role" to role.name.lowercase(),
        "category" to category,
        "bio" to bio,
        "location" to location,
        "avatarUrl" to avatarUrl,
        "averageRating" to averageRating,
        "reviewCount" to reviewCount,
        "fcmToken" to fcmToken
    )

    private fun seedState(): AppState {
        val workers = listOf(
            AppUser("worker_ramesh", "Ramesh Kumar", "ramesh@kk.demo", "9000011111", "password", UserRole.WORKER, "Electrician", "10 years experience in home wiring and repairs", "Dharwad, Karnataka", avatarRes = R.drawable.ramesh_kumar, averageRating = 4.5, reviewCount = 3),
            AppUser("worker_suresh", "Suresh Naik", "suresh@kk.demo", "9000022222", "password", UserRole.WORKER, "Plumber", "Specializes in pipeline repairs and bathroom fitting", "Hubli, Karnataka", avatarRes = R.drawable.suresh_naik, averageRating = 4.2, reviewCount = 3),
            AppUser("worker_mahesh", "Mahesh Patil", "mahesh@kk.demo", "9000033333", "password", UserRole.WORKER, "Carpenter", "Custom furniture and door frame specialist", "Belagavi, Karnataka", avatarRes = R.drawable.mahesh_patil, averageRating = 4.8, reviewCount = 3),
            AppUser("worker_venkatesh", "Venkatesh Rao", "venkatesh@kk.demo", "9000044444", "password", UserRole.WORKER, "Painter", "Interior and exterior painting with 8 years experience", "Mysuru, Karnataka", avatarRes = R.drawable.venkatesh_rao, averageRating = 4.0, reviewCount = 3),
            AppUser("worker_basavaraj", "Basavaraj Metri", "basavaraj@kk.demo", "9000055555", "password", UserRole.WORKER, "Mason", "Brick and cement work, wall construction expert", "Vijayapura, Karnataka", avatarRes = R.drawable.basavaraj_metri, averageRating = 4.6, reviewCount = 3)
        )
        val customer = AppUser("customer_demo", "Anita Rao", "customer@kk.demo", "9000099999", "password", UserRole.CUSTOMER, location = "Dharwad, Karnataka")
        val services = listOf(
            WorkerService("s1", "worker_ramesh", "Fan Installation", "Installation", "₹300"),
            WorkerService("s2", "worker_ramesh", "Board Repair", "Maintenance", "₹500"),
            WorkerService("s3", "worker_suresh", "Pipe Repair", "Maintenance", "₹250"),
            WorkerService("s4", "worker_suresh", "Tap Fitting", "Installation", "₹150"),
            WorkerService("s5", "worker_mahesh", "Door Fitting", "Carpentry", "₹800"),
            WorkerService("s6", "worker_mahesh", "Cabinet Work", "Carpentry", "₹1500", "starting"),
            WorkerService("s7", "worker_venkatesh", "Room Painting", "Painting", "₹2000", "starting"),
            WorkerService("s8", "worker_venkatesh", "Wall Primer", "Painting", "₹800"),
            WorkerService("s9", "worker_basavaraj", "Wall Construction", "Masonry", "₹5000", "starting"),
            WorkerService("s10", "worker_basavaraj", "Tile Fixing", "Masonry", "₹1200")
        )
        val photos = workers.flatMapIndexed { index, worker ->
            val workerImages = when (worker.id) {
                "worker_ramesh" -> listOf(R.drawable.ramesh_work_1, R.drawable.sample_work_1, R.drawable.sample_work_2)
                "worker_suresh" -> listOf(R.drawable.suresh_work_1, R.drawable.sample_work_3, R.drawable.sample_work_4)
                else -> sampleImages.drop(index * 2).take(3)
            }
            workerImages.mapIndexed { photoIndex, res ->
                PortfolioPhoto("photo_${worker.id}_$photoIndex", worker.id, res)
            }
        }
        val reviews = workers.flatMap {
            listOf(
                Review("r_${it.id}_1", it.id, "customer_demo", "Anita R.", 5, "Great work, very professional!"),
                Review("r_${it.id}_2", it.id, "customer_demo", "Manjunath K.", it.averageRating.toInt().coerceAtLeast(4), "Arrived on time and explained the pricing clearly."),
                Review("r_${it.id}_3", it.id, "customer_demo", "Priya S.", 4, "Good local service and polite communication.")
            )
        }
        val seeded = AppState(users = workers + customer, services = services, portfolio = photos, reviews = reviews)
        prefs.edit().putBoolean("isMockDataSeeded", true).putString("state", encodeState(seeded)).apply()
        return seeded
    }

    private fun encodeState(state: AppState): String = JSONObject()
        .put("users", JSONArray(state.users.map { user ->
            JSONObject().put("id", user.id).put("name", user.name).put("email", user.email).put("phone", user.phone)
                .put("password", user.password).put("role", user.role.name).put("category", user.category).put("bio", user.bio)
                .put("location", user.location).put("avatarUrl", user.avatarUrl).put("avatarRes", user.avatarRes).put("averageRating", user.averageRating)
                .put("reviewCount", user.reviewCount).put("fcmToken", user.fcmToken)
        }))
        .put("services", JSONArray(state.services.map { service ->
            JSONObject().put("id", service.id).put("workerId", service.workerId).put("title", service.title)
                .put("category", service.category).put("price", service.price).put("priceType", service.priceType).put("createdAt", service.createdAt)
        }))
        .put("portfolio", JSONArray(state.portfolio.map { photo ->
            JSONObject().put("id", photo.id).put("workerId", photo.workerId).put("imageRes", photo.imageRes).put("imageUri", photo.imageUri).put("uploadedAt", photo.uploadedAt)
        }))
        .put("reviews", JSONArray(state.reviews.map { review ->
            JSONObject().put("id", review.id).put("workerId", review.workerId).put("reviewerId", review.reviewerId)
                .put("reviewerName", review.reviewerName).put("rating", review.rating).put("comment", review.comment).put("timestamp", review.timestamp)
        }))
        .put("hireEvents", JSONArray(state.hireEvents.map { event ->
            JSONObject().put("id", event.id).put("workerId", event.workerId).put("customerId", event.customerId)
                .put("timestamp", event.timestamp).put("status", event.status)
        })).toString()

    private fun decodeState(raw: String): AppState = try {
        val root = JSONObject(raw)
        AppState(
            users = root.getJSONArray("users").mapObjects {
                AppUser(
                    getString("id"), getString("name"), getString("email"), getString("phone"), getString("password"),
                    UserRole.valueOf(getString("role")), optString("category"), optString("bio"), optString("location"),
                    optString("avatarUrl"), optInt("avatarRes"), optDouble("averageRating"), optInt("reviewCount"), optString("fcmToken")
                )
            },
            services = root.getJSONArray("services").mapObjects {
                WorkerService(getString("id"), getString("workerId"), getString("title"), getString("category"), getString("price"), optString("priceType", "fixed"), optLong("createdAt"))
            },
            portfolio = root.getJSONArray("portfolio").mapObjects {
                PortfolioPhoto(getString("id"), getString("workerId"), getInt("imageRes"), optString("imageUri"), optLong("uploadedAt"))
            },
            reviews = root.getJSONArray("reviews").mapObjects {
                Review(getString("id"), getString("workerId"), getString("reviewerId"), getString("reviewerName"), getInt("rating"), getString("comment"), optLong("timestamp"))
            },
            hireEvents = root.optJSONArray("hireEvents")?.mapObjects {
                HireEvent(getString("id"), getString("workerId"), getString("customerId"), optLong("timestamp"), optString("status", "requested"))
            } ?: emptyList()
        )
    } catch (_: Exception) {
        seedState()
    }

    private inline fun <T> JSONArray.mapObjects(block: JSONObject.() -> T): List<T> =
        (0 until length()).map { getJSONObject(it).block() }

    private fun applyBundledImages(state: AppState): AppState {
        val avatarMap = mapOf(
            "worker_ramesh" to R.drawable.ramesh_kumar,
            "worker_suresh" to R.drawable.suresh_naik,
            "worker_mahesh" to R.drawable.mahesh_patil,
            "worker_venkatesh" to R.drawable.venkatesh_rao,
            "worker_basavaraj" to R.drawable.basavaraj_metri
        )
        val updatedUsers = state.users.map { user ->
            val avatarRes = avatarMap[user.id]
            if (avatarRes != null && user.avatarRes == 0 && user.avatarUrl.isBlank()) {
                user.copy(avatarRes = avatarRes)
            } else {
                user
            }
        }
        val existingPhotos = state.portfolio.map { it.workerId to it.imageRes }.toSet()
        val bundledPhotos = listOf(
            PortfolioPhoto("photo_worker_ramesh_real", "worker_ramesh", R.drawable.ramesh_work_1),
            PortfolioPhoto("photo_worker_suresh_real", "worker_suresh", R.drawable.suresh_work_1)
        ).filterNot { it.workerId to it.imageRes in existingPhotos }
        val migrated = state.copy(users = updatedUsers, portfolio = bundledPhotos + state.portfolio)
        if (migrated != state) {
            prefs.edit().putString("state", encodeState(migrated)).apply()
        }
        return migrated
    }

    companion object {
        val sampleImages = listOf(
            R.drawable.sample_work_1, R.drawable.sample_work_2, R.drawable.sample_work_3, R.drawable.sample_work_4,
            R.drawable.sample_work_5, R.drawable.sample_work_6, R.drawable.sample_work_7, R.drawable.sample_work_8,
            R.drawable.sample_work_9, R.drawable.sample_work_10, R.drawable.sample_work_11, R.drawable.sample_work_12
        )
    }
}
