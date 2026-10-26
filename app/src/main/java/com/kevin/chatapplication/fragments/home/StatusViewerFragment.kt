package com.kevin.chatapplication.fragments.home

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.kevin.chatapplication.R
import com.kevin.chatapplication.data.model.StatusItem
import com.kevin.chatapplication.databinding.FragmentStatusViewerBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.StatusPrefs
import com.kevin.chatapplication.utils.TimeUtils
import com.kevin.chatapplication.viewmodel.statusViewerViewModel.StatusViewerViewModel

class StatusViewerFragment : Fragment() {

    private var _binding: FragmentStatusViewerBinding? = null
    private val binding get() = _binding!!
    private val args: StatusViewerFragmentArgs by navArgs()
    private val viewModel: StatusViewerViewModel by viewModels()

    private val segments = mutableListOf<LinearProgressIndicator>()
    private var animator: ValueAnimator? = null
    private var shownItemId = ""
    private var touchDownAt = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.start(args.ownerUid)
        binding.btnDelete.isVisible = viewModel.isMine

        binding.btnClose.setOnClickListener { findNavController().navigateUp() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.viewersRow.setOnClickListener { showViewers() }

        // Quick tap: left third goes back, the rest goes forward. Holding pauses.
        binding.touchArea.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchDownAt = SystemClock.uptimeMillis()
                    animator?.pause()
                }
                MotionEvent.ACTION_UP -> {
                    val held = SystemClock.uptimeMillis() - touchDownAt
                    if (held < TAP_MAX_MS) {
                        v.performClick()
                        if (event.x < v.width / 3f) previous() else next()
                    } else {
                        animator?.resume()
                    }
                }
                MotionEvent.ACTION_CANCEL -> animator?.resume()
            }
            true
        }

        viewModel.owner.observe(viewLifecycleOwner) { owner ->
            val name = if (viewModel.isMine) getString(R.string.my_status) else owner?.name.orEmpty()
            AvatarUtils.bind(binding.tvAvatar, binding.ivPhoto, owner?.name.orEmpty(), owner?.photoUrl.orEmpty())
            binding.tvName.text = name
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            if (items.isEmpty()) {
                findNavController().navigateUp()
                return@observe
            }
            if (segments.size != items.size) buildSegments(items.size)

            if (viewModel.currentIndex !in items.indices) {
                // Start at the first photo I haven't seen yet.
                val seen = StatusPrefs.seenIds(requireContext())
                viewModel.currentIndex = items.indexOfFirst { it.id !in seen }.takeIf { it >= 0 } ?: 0
            }
            val current = items[viewModel.currentIndex]
            if (current.id != shownItemId) show(viewModel.currentIndex) else updateViewers(current)
        }
    }

    // ------------------------------------------------------------ playback

    private fun buildSegments(count: Int) {
        binding.segments.removeAllViews()
        segments.clear()
        val gap = (3 * resources.displayMetrics.density).toInt()
        repeat(count) { index ->
            val bar = LinearProgressIndicator(requireContext()).apply {
                max = PROGRESS_MAX
                trackThickness = (3 * resources.displayMetrics.density).toInt()
                setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.white))
                trackColor = 0x55FFFFFF
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (index > 0) marginStart = gap
                }
            }
            binding.segments.addView(bar)
            segments += bar
        }
    }

    private fun show(index: Int) {
        val items = viewModel.items.value ?: return
        if (index !in items.indices) return
        val item = items[index]
        viewModel.currentIndex = index
        shownItemId = item.id
        animator?.cancel()

        // Bars before = full, current = empty, after = empty.
        segments.forEachIndexed { i, bar -> bar.progress = if (i < index) PROGRESS_MAX else 0 }

        binding.tvTime.text = TimeUtils.formatAgo(item.createdAt)
        binding.tvCaption.isVisible = item.caption.isNotBlank()
        binding.tvCaption.text = item.caption
        updateViewers(item)

        binding.progressLoading.isVisible = true
        binding.ivStatus.load(item.imageUrl) {
            crossfade(true)
            listener(
                onSuccess = { _, _ -> startTimer(item) },
                onError = { _, _ -> startTimer(item) }
            )
        }
    }

    private fun startTimer(item: StatusItem) {
        val b = _binding ?: return
        b.progressLoading.isVisible = false
        StatusPrefs.markSeen(requireContext(), item.id)
        viewModel.markViewed(item)

        val bar = segments.getOrNull(viewModel.currentIndex) ?: return
        animator?.cancel()
        animator = ValueAnimator.ofInt(0, PROGRESS_MAX).apply {
            duration = DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { bar.progress = it.animatedValue as Int }
            doOnEnd { if (_binding != null && bar.progress >= PROGRESS_MAX) next() }
            start()
        }
    }

    private fun next() {
        val items = viewModel.items.value ?: return
        if (viewModel.currentIndex + 1 < items.size) {
            show(viewModel.currentIndex + 1)
        } else {
            findNavController().navigateUp()
        }
    }

    private fun previous() {
        show((viewModel.currentIndex - 1).coerceAtLeast(0))
    }

    // ------------------------------------------------------------ my status: views + delete

    private fun updateViewers(item: StatusItem) {
        val b = _binding ?: return
        b.viewersRow.isVisible = viewModel.isMine
        b.tvViewers.text =
            if (item.viewers.isEmpty()) getString(R.string.status_no_viewers)
            else getString(R.string.status_viewers, item.viewers.size)
    }

    private fun showViewers() {
        val item = currentItem() ?: return
        if (item.viewers.isEmpty()) return
        animator?.pause()
        viewModel.loadViewers(item) { users ->
            if (!isAdded) return@loadViewers
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.viewed_by)
                .setItems(users.map { it.name }.toTypedArray(), null)
                .setPositiveButton(R.string.ok, null)
                .setOnDismissListener { animator?.resume() }
                .show()
        }
    }

    private fun confirmDelete() {
        val item = currentItem() ?: return
        animator?.pause()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_status_title)
            .setMessage(R.string.delete_status_body)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                // The live item list updates and moves on (or closes) by itself.
                shownItemId = ""
                viewModel.delete(item) { }
            }
            .setOnDismissListener { animator?.resume() }
            .show()
    }

    private fun currentItem(): StatusItem? =
        viewModel.items.value?.getOrNull(viewModel.currentIndex)

    override fun onPause() {
        animator?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        animator?.resume()
    }

    override fun onDestroyView() {
        animator?.cancel()
        animator = null
        segments.clear()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PROGRESS_MAX = 1000
        private const val DURATION_MS = 5000L
        private const val TAP_MAX_MS = 250L
    }
}
