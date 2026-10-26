package com.kevin.chatapplication.fragments.home

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kevin.chatapplication.R
import com.kevin.chatapplication.databinding.DialogEditFieldBinding
import com.kevin.chatapplication.databinding.FragmentMyProfileBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.viewmodel.myProfileViewModel.MyProfileViewModel
import java.io.File

class MyProfileFragment : Fragment() {

    private var _binding: FragmentMyProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyProfileViewModel by viewModels()

    private var pendingCameraUri: Uri? = null

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) upload(uri) }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) upload(uri)
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
        _binding = FragmentMyProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.tvEmailHeader.text = viewModel.email
        binding.tvEmailValue.text = viewModel.email

        binding.btnChangePhoto.setOnClickListener { showPhotoOptions() }
        binding.rowName.setOnClickListener {
            editField(R.string.edit_name, R.string.name, viewModel.user.value?.name.orEmpty(), 40) {
                viewModel.updateName(it)
            }
        }
        binding.rowAbout.setOnClickListener {
            editField(R.string.edit_about, R.string.about, viewModel.user.value?.about.orEmpty(), 120) {
                viewModel.updateAbout(it)
            }
        }
        binding.rowPassword.setOnClickListener { confirmPasswordReset() }

        viewModel.user.observe(viewLifecycleOwner) { user ->
            AvatarUtils.bind(binding.tvAvatar, binding.ivPhoto, user.name, user.photoUrl)
            binding.tvName.text = user.name
            binding.tvNameValue.text = user.name
            binding.tvAboutValue.text = user.about
        }

        viewModel.uploading.observe(viewLifecycleOwner) { uploading ->
            binding.progressPhoto.isVisible = uploading
            binding.btnChangePhoto.isEnabled = !uploading
        }

        viewModel.message.observe(viewLifecycleOwner) { text ->
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
                viewModel.messageShown()
            }
        }
    }

    // ------------------------------------------------------------ photo

    private fun showPhotoOptions() {
        val hasPhoto = viewModel.user.value?.photoUrl?.isNotBlank() == true
        val options = mutableListOf(getString(R.string.choose_photo), getString(R.string.take_photo))
        if (hasPhoto) options += getString(R.string.remove_photo)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_photo)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    1 -> openCamera()
                    2 -> viewModel.removePhoto()
                }
            }
            .show()
    }

    private fun openCamera() {
        val context = requireContext()
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "PROFILE_${System.currentTimeMillis()}.jpg")
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

    private fun upload(uri: Uri) {
        viewModel.uploadPhoto(
            requireContext().contentResolver,
            uri,
            successText = getString(R.string.photo_updated),
            failText = { error -> getString(R.string.photo_upload_failed, error) }
        )
    }

    // ------------------------------------------------------------ fields

    private fun editField(
        @StringRes title: Int,
        @StringRes hint: Int,
        current: String,
        maxLength: Int,
        onSave: (String) -> Unit
    ) {
        val dialogBinding = DialogEditFieldBinding.inflate(layoutInflater)
        dialogBinding.tilField.setHint(hint)
        dialogBinding.tilField.isCounterEnabled = true
        dialogBinding.tilField.counterMaxLength = maxLength
        dialogBinding.etField.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        dialogBinding.etField.filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
        dialogBinding.etField.setText(current)
        dialogBinding.etField.setSelection(current.length)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                onSave(dialogBinding.etField.text?.toString().orEmpty())
            }
            .show()
    }

    private fun confirmPasswordReset() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_password)
            .setMessage(getString(R.string.change_password_body, viewModel.email))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.send_link) { _, _ ->
                viewModel.sendPasswordReset { ok, sentTo ->
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_CAMERA_URI = "camera_uri"
    }
}
