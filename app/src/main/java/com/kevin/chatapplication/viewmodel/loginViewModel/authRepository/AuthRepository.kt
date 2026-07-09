package com.kevin.chatapplication.viewmodel.loginViewModel.authRepository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    fun loginUser(
        email: String,
        password: String,
        callback: (Result<Unit>) -> Unit
    ) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                callback(Result.success(Unit))

            }
            .addOnFailureListener {

                callback(Result.failure(it))

            }

    }
}