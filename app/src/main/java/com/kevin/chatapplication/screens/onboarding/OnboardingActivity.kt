package com.kevin.chatapplication.screens.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.OnboardingAdapter
import com.kevin.chatapplication.adapters.OnboardingPage
import com.kevin.chatapplication.databinding.ActivityOnboardingBinding
import com.kevin.chatapplication.screens.authentication.AuthActivity
import com.kevin.chatapplication.utils.OnboardingPrefs

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val pages = listOf(
        OnboardingPage(
            title = R.string.onboarding_title_1,
            description = R.string.onboarding_desc_1,
            mainIcon = R.drawable.ic_forum,
            accentTopIcon = R.drawable.ic_chats,
            accentBottomIcon = R.drawable.ic_call
        ),
        OnboardingPage(
            title = R.string.onboarding_title_2,
            description = R.string.onboarding_desc_2,
            mainIcon = R.drawable.ic_play,
            accentTopIcon = R.drawable.ic_media,
            accentBottomIcon = R.drawable.ic_attach
        ),
        OnboardingPage(
            title = R.string.onboarding_title_3,
            description = R.string.onboarding_desc_3,
            mainIcon = R.drawable.ic_shield,
            accentTopIcon = R.drawable.ic_lock,
            accentBottomIcon = R.drawable.ic_tick
        )
    )

    private val dots = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.viewPager.adapter = OnboardingAdapter(pages)
        createDots()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateUi(position)
        })
        updateUi(0)

        binding.btnNext.setOnClickListener {
            val next = binding.viewPager.currentItem + 1
            if (next < pages.size) binding.viewPager.currentItem = next else finishOnboarding()
        }
        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnGetStarted.setOnClickListener { finishOnboarding() }

        // Back goes to the previous page before leaving the app.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPager.currentItem > 0) {
                    binding.viewPager.currentItem -= 1
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun createDots() {
        val size = resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
        val gap = resources.getDimensionPixelSize(R.dimen.onboarding_dot_gap)
        pages.indices.forEach { _ ->
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = gap
                    marginEnd = gap
                }
                setBackgroundResource(R.drawable.bg_dot_inactive)
            }
            binding.dotsContainer.addView(dot)
            dots.add(dot)
        }
    }

    private fun updateUi(position: Int) {
        val size = resources.getDimensionPixelSize(R.dimen.onboarding_dot_size)
        val activeWidth = resources.getDimensionPixelSize(R.dimen.onboarding_dot_active_width)
        dots.forEachIndexed { index, dot ->
            val active = index == position
            dot.setBackgroundResource(if (active) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dot.layoutParams = dot.layoutParams.apply { width = if (active) activeWidth else size }
        }

        val isLast = position == pages.lastIndex
        binding.btnGetStarted.isVisible = isLast
        binding.btnNext.isVisible = !isLast
        binding.dotsContainer.isVisible = !isLast
    }

    private fun finishOnboarding() {
        OnboardingPrefs.markSeen(this)
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
