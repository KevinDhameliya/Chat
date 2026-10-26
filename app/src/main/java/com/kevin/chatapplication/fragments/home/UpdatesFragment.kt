package com.kevin.chatapplication.fragments.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.StatusAdapter
import com.kevin.chatapplication.databinding.DialogStatusCaptionBinding
import com.kevin.chatapplication.databinding.FragmentUpdatesBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.StatusPrefs
import com.kevin.chatapplication.utils.TimeUtils
import com.kevin.chatapplication.viewmodel.updatesViewModel.UpdatesViewModel
import java.io.File

class UpdatesFragment : Fragment() {

    private var _binding: FragmentUpdatesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UpdatesViewModel by viewModels()
    private lateinit var adapter: StatusAdapter

    private var pendingCameraUri: Uri? = null

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) askCaption(uri) }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) askCaption(uri)
        pendingCameraUri = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingCameraUri = savedInstanceState?.getString(KEY_CAMERA_URI)?.let(Uri::parse)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingCameraUri?.let { outState.putString(KEY_CAMERA_URI, it.toString()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = StatusAdapter { status -> openViewer(status.uid) }
        binding.rvStatuses.adapter = adapter

        // Tap my row: watch my status if I have one, otherwise add one.
        binding.myStatusRow.setOnClickListener {
            if (viewModel.myStatus.value != null) openViewer(viewModel.myUid) else chooseSource()
        }
        binding.btnAddBadge.setOnClickListener { chooseSource() }
        binding.fabAddStatus.setOnClickListener { chooseSource() }

        viewModel.me.observe(viewLifecycleOwner) { me ->
            AvatarUtils.bind(binding.tvMyAvatar, binding.ivMyPhoto, me.name, me.photoUrl)
        }

        viewModel.myStatus.observe(viewLifecycleOwner) { mine ->
            if (mine == null) {
                binding.tvMyStatusSub.setText(R.string.tap_to_add_status)
                binding.myRing.background = null
            } else {
                val ago = TimeUtils.formatAgo(mine.latest)
                binding.tvMyStatusSub.text =
                    if (mine.items.size == 1) getString(R.string.my_status_one, ago)
                    else getString(R.string.my_status_count, mine.items.size, ago)
                binding.myRing.setBackgroundResource(R.drawable.bg_status_ring_seen)
            }
        }

        viewModel.others.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.tvEmpty.isVisible = list.isEmpty()
        }

        viewModel.posting.observe(viewLifecycleOwner) { posting ->
            binding.postingProgress.isVisible = posting
            if (posting) binding.tvMyStatusSub.setText(R.string.posting_status)
        }

        viewModel.message.observe(viewLifecycleOwner) { text ->
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
                viewModel.messageShown()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Rings may have changed after watching statuses.
        adapter.seenIds = StatusPrefs.seenIds(requireContext())
    }

    // ------------------------------------------------------------ posting

    private fun chooseSource() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_status)
            .setItems(arrayOf(getString(R.string.choose_photo), getString(R.string.take_photo))) { _, which ->
                if (which == 0) {
                    pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                } else {
                    openCamera()
                }
            }
            .show()
    }

    private fun openCamera() {
        val context = requireContext()
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "STATUS_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraUri = uri
        try {
            takePhoto.launch(uri)
        } catch (e: Exception) {
            pendingCameraUri = null
            Toast.makeText(context, e.localizedMessage ?: "No camera app found", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun askCaption(uri: Uri) {
        val dialogBinding = DialogStatusCaptionBinding.inflate(layoutInflater)
        dialogBinding.ivPreview.load(uri)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.new_status)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.post) { _, _ ->
                viewModel.post(
                    requireContext().contentResolver,
                    uri,
                    dialogBinding.etCaption.text?.toString().orEmpty(),
                    successText = getString(R.string.status_posted),
                    failText = { error -> getString(R.string.status_post_failed, error) }
                )
            }
            .show()
    }

    private fun openViewer(ownerUid: String) {
        findNavController().navigate(
            UpdatesFragmentDirections.actionUpdatesFragmentToStatusViewerFragment(ownerUid = ownerUid)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_CAMERA_URI = "camera_uri"
    }
}
