package com.gps.zazor.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gps.zazor.BuildConfig
import com.gps.zazor.R
import com.gps.zazor.data.models.Photo
import com.gps.zazor.databinding.FragmentShareBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.share.di.injectViewModel
import com.gps.zazor.utils.viewBinding.viewBinding
import kotlinx.coroutines.launch
import java.io.File

/**
 * Picks what travels with the photos and how they go.
 *
 * Every route here works with no server and no account: straight to a messenger, as an archive, or
 * as text pasted into a chat. Nothing is uploaded anywhere by the app itself.
 */
class ShareFragment : BaseFragment<ShareContract.State, ShareContract.Event>(R.layout.fragment_share) {

    companion object {

        private const val ARG_PATHS = "paths"
        private const val MIME_IMAGE = "image/*"
        private const val MIME_TEXT = "text/plain"

        fun newInstance(paths: List<String>) = ShareFragment().apply {
            arguments = Bundle().apply { putStringArrayList(ARG_PATHS, ArrayList(paths)) }
        }
    }

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentShareBinding::bind)

    override fun observeState(state: ShareContract.State?) {
        when (state) {
            is ShareContract.State.Content -> render(state)
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optMessenger.tvOptionTitle.setText(R.string.share_to_messenger)
        binding.optMessenger.tvOptionHint.setText(R.string.share_to_messenger_hint)
        binding.optBundle.tvOptionTitle.setText(R.string.share_bundle)
        binding.optBundle.tvOptionHint.setText(R.string.share_bundle_hint)
        binding.optReport.tvOptionTitle.setText(R.string.share_report)
        binding.optReport.tvOptionHint.setText(R.string.share_report_hint)

        binding.ivClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.optMessenger.clOption.setOnClickListener {
            viewModel.sendEvent(ShareContract.Event.SendPhotos)
        }
        binding.optBundle.clOption.setOnClickListener {
            viewModel.sendEvent(ShareContract.Event.SendBundle)
        }
        binding.optReport.clOption.setOnClickListener {
            viewModel.sendEvent(ShareContract.Event.SendReport)
        }
        binding.swCoordinates.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) viewModel.sendEvent(ShareContract.Event.ToggleCoordinates(checked))
        }
        binding.swTrack.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) viewModel.sendEvent(ShareContract.Event.ToggleTrack(checked))
        }
        binding.swVoice.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) viewModel.sendEvent(ShareContract.Event.ToggleVoiceNotes(checked))
        }

        observeEffects()
        viewModel.sendEvent(
            ShareContract.Event.Load(arguments?.getStringArrayList(ARG_PATHS).orEmpty())
        )
    }

    private fun render(state: ShareContract.State.Content) {
        val bytes = state.photos.sumOf { photo ->
            File(photo.path).takeIf { it.exists() }?.length() ?: 0L
        }
        binding.tvSummary.text = getString(
            R.string.share_summary,
            state.photos.size,
            bytes / (1024f * 1024f)
        )
        // Only reflect state here; the listeners ignore programmatic changes via isPressed.
        binding.swCoordinates.isChecked = state.options.coordinates
        binding.swTrack.isChecked = state.options.track
        binding.swVoice.isChecked = state.options.voiceNotes
        binding.swVoice.isEnabled = state.photos.any { it.voiceNotePath != null }
        binding.swTrack.isEnabled = state.photos.any { it.lat != null && it.lng != null }
        binding.pbPreparing.isVisible = state.isPreparing
    }

    private fun observeEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is ShareContract.Effect.SharePhotos -> sharePhotos(effect.photos)
                        is ShareContract.Effect.ShareBundle -> shareFile(effect.file)
                        is ShareContract.Effect.ShareText -> shareText(effect.text)
                        is ShareContract.Effect.Empty -> toast(R.string.share_nothing)
                        is ShareContract.Effect.Failed -> toast(R.string.share_failed)
                    }
                }
            }
        }
    }

    private fun sharePhotos(photos: List<Photo>) {
        val uris = ArrayList(photos.mapNotNull { uriFor(File(it.path)) })
        if (uris.isEmpty()) {
            toast(R.string.share_nothing)
            return
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    type = MIME_IMAGE
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share)
            )
        )
    }

    private fun shareFile(file: File) {
        val uri = uriFor(file) ?: run {
            toast(R.string.share_failed)
            return
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = com.gps.zazor.utils.export.BundleWriter.MIME_ZIP
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share)
            )
        )
    }

    private fun shareText(text: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = MIME_TEXT
                },
                getString(R.string.share)
            )
        )
    }

    private fun uriFor(file: File): Uri? =
        try {
            FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun toast(resId: Int) {
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show()
    }
}
