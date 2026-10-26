package com.kevin.chatapplication.fragments.home

import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import com.kevin.chatapplication.R
import com.kevin.chatapplication.data.repository.UserRepository
import com.kevin.chatapplication.databinding.FragmentSettingsBinding
import com.kevin.chatapplication.screens.authentication.AuthActivity
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.ThemePrefs

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val userRepository = UserRepository()
    private var registration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRepository.currentUid?.let { uid ->
            registration = userRepository.observeUser(uid) { user ->
                val b = _binding ?: return@observeUser
                AvatarUtils.bind(b.tvAvatar, b.ivPhoto, user.name, user.photoUrl)
                b.tvName.text = user.name
                b.tvAbout.text = user.about
            }
        }

        binding.profileRow.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_myProfileFragment)
        }
        binding.rowAccount.setOnClickListener { confirmPasswordReset() }
        binding.rowNotifications.setOnClickListener {
            Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
        binding.rowStorage.setOnClickListener { confirmClearCache() }
        binding.rowTheme.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_themeFragment)
        }
        binding.rowBlocked.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_blockedUsersFragment)
        }
        binding.rowHelp.setOnClickListener { showAbout() }
        binding.logoutRow.setOnClickListener { confirmLogout() }
    }

    override fun onResume() {
        super.onResume()
        // Refresh values that can change on other screens.
        binding.tvThemeSub.setText(
            when (ThemePrefs.get(requireContext())) {
                ThemePrefs.LIGHT -> R.string.theme_light
                ThemePrefs.DARK -> R.string.theme_dark
                else -> R.string.theme_system
            }
        )
        updateCacheSize()
    }

    // ------------------------------------------------------------ account

    private fun confirmPasswordReset() {
        val email = userRepository.currentEmail
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_password)
            .setMessage(getString(R.string.change_password_body, email))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.send_link) { _, _ ->
                userRepository.sendPasswordReset { ok, sentTo ->
                    if (!isAdded) return@sendPasswordReset
                    Toast.makeText(
                        requireContext(),
                        if (ok) getString(R.string.reset_link_sent, sentTo)
                        else getString(R.string.reset_link_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    // ------------------------------------------------------------ storage

    @OptIn(ExperimentalCoilApi::class)
    private fun updateCacheSize() {
        val bytes = requireContext().imageLoader.diskCache?.size ?: 0L
        binding.tvStorageSub.text = getString(
            R.string.settings_storage_sub,
            Formatter.formatShortFileSize(requireContext(), bytes)
        )
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun confirmClearCache() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.clear_cache_title)
            .setMessage(R.string.clear_cache_body)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_cache) { _, _ ->
                val loader = requireContext().imageLoader
                loader.memoryCache?.clear()
                loader.diskCache?.clear()
                updateCacheSize()
                Toast.makeText(requireContext(), R.string.cache_cleared, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // ------------------------------------------------------------ about / logout

    private fun showAbout() {
        val context = requireContext()
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_help)
            .setMessage(getString(R.string.about_app_body, version))
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                registration?.remove()
                userRepository.logout()
                startActivity(
                    Intent(requireContext(), AuthActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        registration?.remove()
        registration = null
        _binding = null
    }
}
