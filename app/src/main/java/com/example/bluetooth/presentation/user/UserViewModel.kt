package com.example.bluetooth.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.subpabase.Order
import com.example.bluetooth.subpabase.SessionManager
import com.example.bluetooth.subpabase.SupabaseRepository
import com.example.bluetooth.subpabase.UserProfile
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserUiState(
    val profile: UserProfile? = null,
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class UserViewModel @Inject constructor(
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(UserUiState())
    val state = _state.asStateFlow()

    val isGuest: Boolean get() = session.isGuest

    init { loadData() }

    fun loadData() {
        if (session.isGuest) return
        val uid = session.currentUid ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            SupabaseRepository.getUserProfile(uid)
                .onSuccess { _state.value = _state.value.copy(profile = it) }
                .onFailure { _state.value = _state.value.copy(error = "Không tải được thông tin") }
            SupabaseRepository.getOrders(uid)
                .onSuccess { _state.value = _state.value.copy(orders = it) }
                .onFailure { _state.value = _state.value.copy(error = "Không tải được lịch sử") }
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun redeemReward(cost: Int, type: String, title: String) {
        val uid = session.currentUid ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, message = null, error = null)
            SupabaseRepository.redeemReward(uid, cost, type)
                .onSuccess {
                    _state.value = _state.value.copy(
                        message = "✅ Đã đổi: $title",
                        isLoading = false
                    )
                    loadData() // cập nhật lại điểm và discount_type
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        error = it.message ?: "Lỗi khi đổi quà",
                        isLoading = false
                    )
                }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun refresh() = loadData()

    fun signOut(onDone: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        _state.value = UserUiState()
        onDone()
    }
}