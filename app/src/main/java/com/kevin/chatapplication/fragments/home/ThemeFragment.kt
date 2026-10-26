package com.kevin.chatapplication.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kevin.chatapplication.databinding.FragmentThemeBinding
import com.kevin.chatapplication.utils.ThemePrefs

class ThemeFragment : Fragment() {

    private var _binding: FragmentThemeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        showSelected(ThemePrefs.get(requireContext()))

        binding.cardSystem.setOnClickListener { choose(ThemePrefs.SYSTEM) }
        binding.cardLight.setOnClickListener { choose(ThemePrefs.LIGHT) }
        binding.cardDark.setOnClickListener { choose(ThemePrefs.DARK) }
    }

    private fun choose(mode: Int) {
        if (mode == ThemePrefs.get(requireContext())) return
        showSelected(mode)
        // The activity recreates itself with the new colours; this screen stays open.
        ThemePrefs.set(requireContext(), mode)
    }

    private fun showSelected(mode: Int) {
        binding.radioSystem.isChecked = mode == ThemePrefs.SYSTEM
        binding.radioLight.isChecked = mode == ThemePrefs.LIGHT
        binding.radioDark.isChecked = mode == ThemePrefs.DARK
        binding.cardSystem.isChecked = mode == ThemePrefs.SYSTEM
        binding.cardLight.isChecked = mode == ThemePrefs.LIGHT
        binding.cardDark.isChecked = mode == ThemePrefs.DARK
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
