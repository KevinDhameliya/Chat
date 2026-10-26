package com.kevin.chatapplication.screens.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.kevin.chatapplication.screens.authentication.AuthActivity
import com.kevin.chatapplication.screens.home.HomeActivity
import com.kevin.chatapplication.screens.onboarding.OnboardingActivity
import com.kevin.chatapplication.utils.OnboardingPrefs
import com.kevin.chatapplication.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Only pad the centred content, so the waves run under the navigation bar.
        val content = findViewById<View>(R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            content.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        animateIn(content, findViewById(R.id.logo))

        Handler(mainLooper).postDelayed({
            // Signed in -> Home. First launch -> Onboarding. Otherwise -> Login.
            val next = when {
                FirebaseAuth.getInstance().currentUser != null -> HomeActivity::class.java
                !OnboardingPrefs.isSeen(this) -> OnboardingActivity::class.java
                else -> AuthActivity::class.java
            }
            startActivity(Intent(this, next))
            finish()
        }, SPLASH_DURATION_MS)
    }

    /** Logo pops in, then the brand name and tagline fade up. */
    private fun animateIn(content: View, logo: View) {
        logo.scaleX = 0.6f
        logo.scaleY = 0.6f
        logo.alpha = 0f
        logo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator(1.6f))
            .start()

        listOf(R.id.tvBrand, R.id.tvTagline).forEachIndexed { index, id ->
            content.findViewById<View>(id).apply {
                alpha = 0f
                translationY = 24f * resources.displayMetrics.density
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(350L + index * 150L)
                    .setDuration(500)
                    .start()
            }
        }
    }

    companion object {
        private const val SPLASH_DURATION_MS = 2200L
    }
}