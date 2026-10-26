package com.kevin.chatapplication.fragments.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.MessageAdapter
import com.kevin.chatapplication.data.model.Message
import com.kevin.chatapplication.databinding.BottomSheetAttachmentsBinding
import com.kevin.chatapplication.databinding.DialogMessageOptionsBinding
import com.kevin.chatapplication.databinding.FragmentChatBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.CallLauncher
import com.kevin.chatapplication.utils.TimeUtils
import com.kevin.chatapplication.viewmodel.chatViewModel.BlockState
import com.kevin.chatapplication.viewmodel.chatViewModel.ChatViewModel
import java.io.File

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val args: ChatFragmentArgs by navArgs()
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessageAdapter

    /** Where the camera app writes the photo we asked for. */
    private var pendingCameraUri: Uri? = null

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.sendImage(requireContext().contentResolver, uri)
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { saved ->
        val uri = pendingCameraUri
        if (saved && uri != null) viewModel.sendImage(requireContext().contentResolver, uri)
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
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.start(args.otherUid, args.otherName, if (args.isGroup) args.chatId else "")
        bindHeader(args.otherName, status = "")

        adapter = MessageAdapter(
            myUid = viewModel.myUid,
            showSenderNames = args.isGroup,
            onLongPress = { message, _ -> showMessageOptions(message) },
            onImageClick = { message -> openMediaViewer(message) }
        )
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        // Keep the latest message visible when the keyboard opens.
        binding.rvMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom) scrollToBottom()
        }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.headerInfo.setOnClickListener {
            if (args.isGroup) {
                showGroupMembers()
            } else {
                findNavController().navigate(
                    ChatFragmentDirections.actionChatFragmentToContactProfileFragment(
                        otherUid = args.otherUid,
                        otherName = binding.tvName.text.toString()
                    )
                )
            }
        }

        binding.etMessage.doAfterTextChanged { text ->
            val hasText = !text.isNullOrBlank()
            binding.btnSend.setImageResource(if (hasText) R.drawable.ic_send else R.drawable.ic_mic)
            binding.btnCamera.isVisible = !hasText
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (text.isBlank()) {
                comingSoon()
            } else {
                viewModel.sendMessage(text)
                binding.etMessage.text?.clear()
            }
        }

        binding.btnAttach.setOnClickListener { showAttachmentSheet() }
        binding.btnCamera.setOnClickListener { openCamera() }
        binding.btnCancelReply.setOnClickListener { viewModel.setReplyTarget(null) }
        binding.tvBlocked.setOnClickListener {
            if (viewModel.blockState.value == BlockState.BLOCKED_BY_ME) viewModel.unblock()
        }

        binding.btnCall.setOnClickListener { startCall(video = false) }
        binding.btnVideo.setOnClickListener { startCall(video = true) }
        listOf(binding.btnMore, binding.btnEmoji)
            .forEach { it.setOnClickListener { comingSoon() } }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            binding.tvEmpty.isVisible = messages.isEmpty()
            binding.tvEmpty.text = if (args.isGroup) {
                getString(R.string.say_hi_group, binding.tvName.text)
            } else {
                getString(R.string.say_hi, args.otherName.substringBefore(" ").ifBlank { args.otherName })
            }
            val shouldScroll = messages.size > adapter.itemCount
            adapter.submitList(messages) {
                if (shouldScroll) scrollToBottom()
            }
        }

        viewModel.otherUser.observe(viewLifecycleOwner) { user ->
            val hidePresence = viewModel.blockState.value != BlockState.NONE &&
                viewModel.blockState.value != null
            bindHeader(
                user.name,
                if (hidePresence) "" else TimeUtils.formatPresence(user.online, user.lastSeen),
                user.photoUrl
            )
        }

        viewModel.group.observe(viewLifecycleOwner) { group ->
            if (!args.isGroup) return@observe
            if (group == null) {
                // Left the group (or was removed): read-only.
                binding.inputRow.isVisible = false
                binding.tvBlocked.isVisible = true
                binding.tvBlocked.setText(R.string.not_a_member)
                binding.tvStatus.isVisible = false
                viewModel.setReplyTarget(null)
                return@observe
            }
            binding.inputRow.isVisible = true
            binding.tvBlocked.isVisible = false
            // "You, Sarah, Alex, Priya"
            val names = listOf(getString(R.string.you)) + group.participants
                .filter { it != viewModel.myUid }
                .map { group.nameOf(it).substringBefore(" ") }
            bindHeader(group.name.ifBlank { args.otherName }, names.joinToString(", "))
        }

        viewModel.blockState.observe(viewLifecycleOwner) { state ->
            if (args.isGroup) return@observe
            val blocked = state != BlockState.NONE
            binding.inputRow.isVisible = !blocked
            binding.tvBlocked.isVisible = blocked
            binding.tvBlocked.setText(
                if (state == BlockState.BLOCKED_BY_ME) R.string.you_blocked_contact
                else R.string.cannot_reply
            )
            if (blocked) {
                viewModel.setReplyTarget(null)
                binding.tvStatus.isVisible = false
            }
        }

        viewModel.replyTarget.observe(viewLifecycleOwner) { message ->
            binding.replyBar.isVisible = message != null
            if (message != null) {
                binding.tvReplyTitle.text =
                    getString(R.string.replying_to, viewModel.senderNameOf(message))
                binding.tvReplyPreview.text = message.preview()
                binding.ivReplyThumb.isVisible = message.isImage
                if (message.isImage) binding.ivReplyThumb.load(message.imageUrl)
                binding.etMessage.requestFocus()
            }
        }

        viewModel.uploading.observe(viewLifecycleOwner) { uploading ->
            binding.uploadProgress.isVisible = uploading
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.errorShown()
            }
        }
    }

    // ------------------------------------------------------------ attachments (screen 10)

    private fun showAttachmentSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheet = BottomSheetAttachmentsBinding.inflate(layoutInflater)
        dialog.setContentView(sheet.root)

        sheet.attachGallery.setOnClickListener {
            dialog.dismiss()
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        sheet.attachCamera.setOnClickListener {
            dialog.dismiss()
            openCamera()
        }
        listOf(sheet.attachDocument, sheet.attachLocation, sheet.attachContact, sheet.attachAudio)
            .forEach { option ->
                option.setOnClickListener {
                    dialog.dismiss()
                    comingSoon()
                }
            }
        dialog.show()
    }

    private fun openCamera() {
        val context = requireContext()
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
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

    // ------------------------------------------------------------ message options (screen 19)

    private fun showMessageOptions(message: Message) {
        if (message.deleted) return
        val options = DialogMessageOptionsBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(options.root)
            .create()

        // Reaction row
        val myReaction = message.reactions[viewModel.myUid]
        REACTIONS.forEach { emoji ->
            val chip = TextView(requireContext()).apply {
                text = emoji
                textSize = 26f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                setBackgroundResource(
                    if (emoji == myReaction) R.drawable.bg_success_card else 0
                )
                setOnClickListener {
                    viewModel.toggleReaction(message, emoji)
                    dialog.dismiss()
                }
            }
            options.reactionsRow.addView(chip)
        }

        options.actionReply.setOnClickListener {
            viewModel.setReplyTarget(message)
            dialog.dismiss()
        }

        options.actionCopy.isVisible = message.text.isNotBlank()
        options.actionCopy.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("message", message.text))
            Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        options.actionForward.setOnClickListener {
            dialog.dismiss()
            showForwardPicker(message)
        }

        options.actionDelete.isVisible = message.senderId == viewModel.myUid
        options.actionDelete.setOnClickListener {
            dialog.dismiss()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_message_title)
                .setMessage(R.string.delete_message_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_for_everyone) { _, _ ->
                    viewModel.deleteMessage(message)
                }
                .show()
        }

        dialog.show()
    }

    private fun showForwardPicker(message: Message) {
        viewModel.loadContacts { contacts ->
            if (!isAdded) return@loadContacts
            if (contacts.isEmpty()) {
                Toast.makeText(requireContext(), R.string.no_contacts_to_forward, Toast.LENGTH_SHORT)
                    .show()
                return@loadContacts
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.forward_to)
                .setItems(contacts.map { it.name }.toTypedArray()) { _, which ->
                    val target = contacts[which]
                    viewModel.forward(message, target)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.forwarded_to, target.name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    // ------------------------------------------------------------ group members (screen 14)

    private fun showGroupMembers() {
        val group = viewModel.group.value ?: return
        val me = viewModel.myUid
        // Me first, then everyone else by name.
        val ordered = listOf(me).filter { it in group.participants } +
            group.participants.filter { it != me }.sortedBy { group.nameOf(it).lowercase() }
        val labels = ordered.map { uid ->
            val name = if (uid == me) getString(R.string.you) else group.nameOf(uid)
            if (uid in group.admins) getString(R.string.admin_suffix, name) else name
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.group_members_title, group.name, group.participants.size))
            .setItems(labels.toTypedArray()) { _, which ->
                val uid = ordered[which]
                if (uid != me) {
                    findNavController().navigate(
                        ChatFragmentDirections.actionChatFragmentToContactProfileFragment(
                            otherUid = uid,
                            otherName = group.nameOf(uid)
                        )
                    )
                }
            }
            .setNegativeButton(R.string.leave_group) { _, _ -> confirmLeaveGroup(group.name) }
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun confirmLeaveGroup(groupName: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.leave_group)
            .setMessage(getString(R.string.leave_group_confirm, groupName))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.leave) { _, _ ->
                viewModel.leaveGroup { ok ->
                    if (ok && isAdded) findNavController().navigateUp()
                }
            }
            .show()
    }

    // ------------------------------------------------------------ helpers

    private fun openMediaViewer(message: Message) {
        findNavController().navigate(
            ChatFragmentDirections.actionChatFragmentToMediaViewerFragment(
                chatId = viewModel.chatId,
                messageId = message.id
            )
        )
    }

    override fun onResume() {
        super.onResume()
        viewModel.setScreenVisible(true)
    }

    override fun onPause() {
        viewModel.setScreenVisible(false)
        super.onPause()
    }

    private fun bindHeader(name: String, status: String, photoUrl: String = "") {
        AvatarUtils.bind(binding.tvAvatar, binding.ivPhoto, name, photoUrl)
        binding.tvName.text = name
        binding.tvStatus.text = status
        binding.tvStatus.isVisible = status.isNotBlank()
    }

    private fun scrollToBottom() {
        val b = _binding ?: return
        if (adapter.itemCount > 0) {
            b.rvMessages.post { _binding?.rvMessages?.scrollToPosition(adapter.itemCount - 1) }
        }
    }

    /** Direct chats only; group calls aren't supported yet. */
    private fun startCall(video: Boolean) {
        when {
            args.isGroup -> comingSoon()
            viewModel.blockState.value == BlockState.BLOCKED_BY_ME ||
                viewModel.blockState.value == BlockState.BLOCKED_ME ->
                Toast.makeText(requireContext(), R.string.cannot_call_blocked, Toast.LENGTH_SHORT).show()
            else -> CallLauncher.call(requireContext(), args.otherUid, binding.tvName.text.toString(), video)
        }
    }

    private fun comingSoon() {
        Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_CAMERA_URI = "camera_uri"
        private val REACTIONS = listOf("👍", "❤️", "😂", "😮", "😢", "🙏")
    }
}
