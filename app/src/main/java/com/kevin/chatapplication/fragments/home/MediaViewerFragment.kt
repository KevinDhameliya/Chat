package com.kevin.chatapplication.fragments.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.MediaPagerAdapter
import com.kevin.chatapplication.adapters.MediaThumbAdapter
import com.kevin.chatapplication.databinding.FragmentMediaViewerBinding
import com.kevin.chatapplication.utils.TimeUtils
import com.kevin.chatapplication.viewmodel.mediaViewerViewModel.MediaViewerViewModel

class MediaViewerFragment : Fragment() {

    private var _binding: FragmentMediaViewerBinding? = null
    private val binding get() = _binding!!
    private val args: MediaViewerFragmentArgs by navArgs()
    private val viewModel: MediaViewerViewModel by viewModels()

    private lateinit var pagerAdapter: MediaPagerAdapter
    private lateinit var thumbAdapter: MediaThumbAdapter
    private var barsVisible = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMediaViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.start(args.chatId, args.messageId)

        pagerAdapter = MediaPagerAdapter(onTap = { toggleBars() })
        binding.viewPager.adapter = pagerAdapter

        thumbAdapter = MediaThumbAdapter(sizeDp = 52) { position, _ ->
            binding.viewPager.setCurrentItem(position, true)
        }
        binding.rvThumbs.adapter = thumbAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateForPage(position)
        })

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnShare.setOnClickListener { shareCurrent() }

        viewModel.photos.observe(viewLifecycleOwner) { photos ->
            if (photos.isEmpty()) {
                // The photo was deleted while we were looking at it.
                findNavController().navigateUp()
                return@observe
            }
            pagerAdapter.submitList(photos)
            thumbAdapter.submitList(photos) { updateForPage(binding.viewPager.currentItem) }
            binding.rvThumbs.isVisible = barsVisible && photos.size > 1
        }

        viewModel.startIndex.observe(viewLifecycleOwner) { index ->
            if (index == null) return@observe
            binding.viewPager.post {
                _binding?.viewPager?.setCurrentItem(index, false)
                updateForPage(index)
            }
            viewModel.startIndexUsed()
        }
    }

    private fun updateForPage(position: Int) {
        val b = _binding ?: return
        val photos = viewModel.photos.value.orEmpty()
        if (position !in photos.indices) return
        b.tvCounter.text = getString(R.string.media_counter, position + 1, photos.size)
        b.tvTime.text = TimeUtils.formatChatListTime(photos[position].timestamp)
        thumbAdapter.selectedPosition = position
        b.rvThumbs.scrollToPosition(position)
    }

    private fun toggleBars() {
        barsVisible = !barsVisible
        binding.topBar.isVisible = barsVisible
        binding.rvThumbs.isVisible = barsVisible && thumbAdapter.itemCount > 1
    }

    private fun shareCurrent() {
        val photo = viewModel.photos.value?.getOrNull(binding.viewPager.currentItem) ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, photo.imageUrl)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
