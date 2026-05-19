package com.example.bluetooth.subpabase


import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {
    private val auth = FirebaseAuth.getInstance()

    val isLoggedIn: Boolean get() = auth.currentUser != null
    val isGuest: Boolean get() = !isLoggedIn
    val currentUid: String? get() = auth.currentUser?.uid
    val currentEmail: String? get() = auth.currentUser?.email
}