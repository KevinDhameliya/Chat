package com.kevin.chatapplication.utils

import android.util.Patterns

object ValidationUtils {

    fun validateEmail(email: String): ValidationResult {

        if (email.isBlank()) {
            return ValidationResult(false, "Email is required")
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult(false, "Enter a valid email")
        }

        return ValidationResult(true)
    }

    fun validatePassword(password: String): ValidationResult {

        if (password.isBlank()) {
            return ValidationResult(false, "Password is required")
        }

        if (password.length < 6) {
            return ValidationResult(false, "Password must be at least 6 characters")
        }

        return ValidationResult(true)
    }

    fun validateConfirmPassword(
        password: String,
        confirmPassword: String
    ): ValidationResult {

        if (confirmPassword.isBlank()) {
            return ValidationResult(false, "Confirm password is required")
        }

        if (password != confirmPassword) {
            return ValidationResult(false, "Passwords do not match")
        }

        return ValidationResult(true)
    }

}