package com.kevin.chatapplication.fragments.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kevin.chatapplication.R
import com.kevin.chatapplication.adapters.MediaThumbAdapter
import com.kevin.chatapplication.databinding.FragmentContactProfileBinding
import com.kevin.chatapplication.utils.AvatarUtils
import com.kevin.chatapplication.utils.CallLauncher
import com.kevin.chatapplication.utils.ChatPrefs
import com.kevin.chatapplication.utils.TimeUtils
import com.kevin.chatapplication.viewmodel.contactProfileViewModel.ContactProfileViewModel

class ContactProfileFragment : Fragment() {

    private var _binding: FragmentContactProfileBinding? = null
    private val binding get() = _binding!!
    private val args: ContactProfileFragmentArgs by navArgs()
    private val viewModel: ContactProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.start(args.otherUid)
        AvatarUtils.bind(binding.tvAvatar, args.otherName)
        binding.tvName.text = args.otherName

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnMessage.setOnClickListener { openChat() }
        binding.btnCall.setOnClickListener { startCall(video = false) }
        binding.btnVideo.setOnClickListener { startCall(video = true) }

        // Shared photos
        val mediaAdapter = MediaThumbAdapter(sizeDp = 76) { _, message ->
            findNavController().navigate(
                ContactProfileFragmentDirections.actionContactProfileFragmentToMediaViewerFragment(
                    chatId = viewModel.chatId,
                    messageId = message.id
                )
            )
        }
        binding.rvMedia.adapter = mediaAdapter
        binding.mediaHeader.setOnClickListener {
            mediaAdapter.currentList.firstOrNull()?.let { latest ->
                findNavController().navigate(
                    ContactProfileFragmentDirections.actionContactProfileFragmentToMediaViewerFragment(
                        chatId = viewModel.chatId,
                        messageId = latest.id
                    )
                )
            }
        }

        // Mute (stored on this device; used once notifications are added)
        binding.switchMute.isChecked = ChatPrefs.isMuted(requireContext(), viewModel.chatId)
        binding.switchMute.setOnCheckedChangeListener { _, checked ->
            ChatPrefs.setMuted(requireContext(), viewModel.chatId, checked)
        }

        binding.blockRow.setOnClickListener { confirmBlockToggle() }

        viewModel.contact.observe(viewLifecycleOwner) { user ->
            AvatarUtils.bind(binding.tvAvatar, binding.ivPhoto, user.name, user.photoUrl)
            binding.tvName.text = user.name
            binding.tvAbout.text = user.about
            val blocked = viewModel.isBlocked.value == true
            binding.tvStatus.text =
                if (blocked) "" else TimeUtils.formatPresence(user.online, user.lastSeen)
        }

        viewModel.media.observe(viewLifecycleOwner) { photos ->
            mediaAdapter.submitList(photos)
            binding.tvMediaCount.text = if (photos.isEmpty()) "" else photos.size.toString()
            binding.rvMedia.isVisible = photos.isNotEmpty()
            binding.tvNoMedia.isVisible = photos.isEmpty()
        }

        viewModel.isBlocked.observe(viewLifecycleOwner) { blocked ->
            binding.tvBlock.setText(if (blocked) R.string.unblock_contact else R.string.block_contact)
            if (blocked) binding.tvStatus.text = ""
        }
    }

    private fun confirmBlockToggle() {
        val name = binding.tvName.text.toString()
        if (viewModel.isBlocked.value == true) {
            viewModel.setBlocked(false)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.block_confirm_title, name))
            .setMessage(R.string.block_confirm_body)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.block) { _, _ -> viewModel.setBlocked(true) }
            .show()
    }

    /** Back to the chat if we came from it; otherwise open a chat with this person. */
    private fun openChat() {
        val nav = findNavController()
        val previous = nav.previousBackStackEntry
        val cameFromThisChat = previous?.destination?.id == R.id.chatFragment &&
            previous.arguments?.getString("otherUid") == args.otherUid &&
            previous.arguments?.getBoolean("isGroup") != true
        if (cameFromThisChat) {
            nav.navigateUp()
        } else {
            nav.navigate(
                ContactProfileFragmentDirections.actionContactProfileFragmentToChatFragment(
                    otherUid = args.otherUid,
                    otherName = binding.tvName.text.toString()
                )
            )
        }
    }

    private fun startCall(video: Boolean) {
        if (viewModel.isBlocked.value == true) {
            Toast.makeText(requireContext(), R.string.cannot_call_blocked, Toast.LENGTH_SHORT).show()
            return
        }
        CallLauncher.call(requireContext(), args.otherUid, binding.tvName.text.toString(), video)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
