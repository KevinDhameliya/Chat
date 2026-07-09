package com.kevin.chatapplication.viewmodel.signupViewModel.authRepository

import com.google.firebase.auth.FirebaseAuth

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    fun signup(
        email: String,
        password: String,
        callback: (Result<Unit>) -> Unit
    ) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                callback(Result.success(Unit))

            }
            .addOnFailureListener {

                callback(Result.failure(it))

            }

    }

}