package com.kevin.chatapplication.screens.home

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.kevin.chatapplication.R
import com.kevin.chatapplication.data.repository.UserRepository
import com.kevin.chatapplication.databinding.ActivityHomeBinding
import com.kevin.chatapplication.screens.authentication.AuthActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val userRepository = UserRepository()

    /** Screens that show the bottom navigation bar. */
    private val topLevelDestinations = setOf(
        R.id.chatsFragment,
        R.id.updatesFragment,
        R.id.callsFragment,
        R.id.settingsFragment
    )
    private var isTopLevel = true
    private var isKeyboardOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (userRepository.currentUid == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Pad for the system bars and lift the content above the keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            isKeyboardOpen = insets.isVisible(WindowInsetsCompat.Type.ime())
            updateBottomNavVisibility()
            WindowInsetsCompat.CONSUMED
        }

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_home) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            isTopLevel = destination.id in topLevelDestinations
            updateBottomNavVisibility()
            applyMediaViewerLook(
                destination.id == R.id.mediaViewerFragment ||
                    destination.id == R.id.statusViewerFragment
            )
        }

        userRepository.ensureProfile()
    }

    override fun onStart() {
        super.onStart()
        userRepository.setOnline(true)
    }

    override fun onStop() {
        super.onStop()
        userRepository.setOnline(false)
    }

    /** Black background and light status-bar icons while viewing photos. */
    private fun applyMediaViewerLook(isMediaViewer: Boolean) {
        val isNight = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        binding.main.setBackgroundColor(
            ContextCompat.getColor(this, if (isMediaViewer) R.color.black else R.color.screen_background)
        )
        WindowCompat.getInsetsController(window, binding.main).apply {
            isAppearanceLightStatusBars = !isMediaViewer && !isNight
            isAppearanceLightNavigationBars = !isMediaViewer && !isNight
        }
    }

    private fun updateBottomNavVisibility() {
        binding.bottomNav.isVisible = isTopLevel && !isKeyboardOpen
    }
}
