package com.kaushalya.karnataka.ui

import androidx.lifecycle.ViewModel
import com.kaushalya.karnataka.data.KaushalyaRepository
import com.kaushalya.karnataka.model.UserRole
import kotlinx.coroutines.flow.map

class KaushalyaViewModel(private val repository: KaushalyaRepository) : ViewModel() {
    val state = repository.state
    val sessionRoute = repository.state.map { state ->
        when (state.currentUser?.role) {
            UserRole.WORKER -> Routes.WorkerDashboard
            UserRole.CUSTOMER -> Routes.CustomerHome
            null -> Routes.Login
        }
    }

    fun login(email: String, password: String, role: UserRole) = repository.login(email, password, role)
    fun register(name: String, email: String, phone: String, password: String, role: UserRole, category: String) =
        repository.register(name, email, phone, password, role, category)
    fun logout() = repository.logout()
    fun addOrUpdateService(id: String?, title: String, category: String, price: String, priceType: String) =
        repository.addOrUpdateService(id, title, category, price, priceType)
    fun deleteService(id: String) = repository.deleteService(id)
    fun addPortfolioPhoto() = repository.addPortfolioPhoto()
    fun addPortfolioPhoto(uri: String) = repository.addPortfolioPhoto(imageUri = uri)
    fun updateProfilePhoto(uri: String) = repository.updateProfilePhoto(uri)
    fun updateFcmToken(token: String) = repository.updateFcmToken(token)
    fun hire(workerId: String) = repository.hire(workerId)
    fun addReview(workerId: String, rating: Int, comment: String) = repository.addReview(workerId, rating, comment)
    fun clearMessage() = repository.clearMessage()
}

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Register = "register"
    const val CustomerHome = "customerHome"
    const val WorkerDashboard = "workerDashboard"
    const val WorkerProfile = "workerProfile"
}
