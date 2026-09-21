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
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.gps.zazor.utils.extensions.loadImage
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

        /** The corner the design rounds the stacked frames to. */
        private const val STACK_RADIUS_DP = 14

        /** How far each card of the pile is offset from the one under it. */
        private const val STACK_STEP_DP = 44

        /** The gap between the pile and the words beside it. */
        private const val STACK_GAP_DP = 10

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
        binding.optMessenger.ivOptionIcon.setImageResource(R.drawable.ic_share_nodes)
        binding.optBundle.tvOptionTitle.setText(R.string.share_bundle)
        binding.optBundle.tvOptionHint.setText(R.string.share_bundle_hint)
        binding.optBundle.ivOptionIcon.setImageResource(R.drawable.ic_archive)
        binding.optReport.tvOptionTitle.setText(R.string.share_report)
        binding.optReport.tvOptionHint.setText(R.string.share_report_hint)
        binding.optReport.ivOptionIcon.setImageResource(R.drawable.ic_document)
        // The first way is the one in hand until another is touched, as the design shows it.
        selectOption(binding.optMessenger.clOption)

        binding.ivClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.optMessenger.clOption.setOnClickListener {
            selectOption(it)
            viewModel.sendEvent(ShareContract.Event.SendPhotos)
        }
        binding.optBundle.clOption.setOnClickListener {
            selectOption(it)
            viewModel.sendEvent(ShareContract.Event.SendBundle)
        }
        binding.optReport.clOption.setOnClickListener {
            selectOption(it)
            viewModel.sendEvent(ShareContract.Event.SendReport)
        }
        binding.swCoordinates.setOnClickListener {
            viewModel.sendEvent(ShareContract.Event.ToggleCoordinates(!it.isSelected))
        }
        binding.swAddress.setOnClickListener {
            viewModel.sendEvent(ShareContract.Event.ToggleAddress(!it.isSelected))
        }
        binding.swTrack.setOnClickListener {
            viewModel.sendEvent(ShareContract.Event.ToggleTrack(!it.isSelected))
        }
        binding.swVoice.setOnClickListener {
            viewModel.sendEvent(ShareContract.Event.ToggleVoiceNotes(!it.isSelected))
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
        binding.tvSummary.text = resources.getQuantityString(
            R.plurals.share_photos_count, state.photos.size, state.photos.size
        )
        // Size, and the tightest fix in the set: the two things worth knowing before sending.
        val best = state.photos.mapNotNull { it.accuracyMeters }.minOrNull()
        binding.tvSummaryDetail.text = listOfNotNull(
            getString(R.string.share_megabytes, bytes / (1024f * 1024f)),
            best?.let { getString(R.string.share_best_accuracy, Math.round(it)) }
        ).joinToString(" \u00b7 ")
        showStack(state.photos)
        listOf(
            binding.swCoordinates to state.options.coordinates,
            binding.swAddress to state.options.address,
            binding.swVoice to state.options.voiceNotes,
            binding.swTrack to state.options.track
        ).forEach { (pill, on) ->
            pill.isSelected = on
            // The tick belongs to what is actually coming along.
            pill.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (on) R.drawable.ic_check_small else 0, 0, 0, 0
            )
        }
        binding.swAddress.isEnabled = state.photos.any { !it.address.isNullOrBlank() }
        // Notes cover both what was typed and what was recorded.
        binding.swVoice.isEnabled = state.photos.any {
            it.voiceNotePath != null || it.name.isNotBlank()
        }
        binding.swTrack.isEnabled = state.photos.any { it.lat != null && it.lng != null }
        binding.pbPreparing.isVisible = state.isPreparing
    }

    /**
     * The first three frames, stacked the way the design piles them.
     *
     * The text beside them starts where the pile actually ends: with one photo to send there is
     * one card, not a gap where the other two would have been.
     */
    private fun showStack(photos: List<Photo>) {
        val slots = listOf(binding.ivStackFirst, binding.ivStackSecond, binding.ivStackThird)
        slots.forEachIndexed { index, view ->
            val photo = photos.getOrNull(index)
            view.isVisible = photo != null
            photo?.let { view.loadImage(it.path, circle = false, cornerRadiusDp = STACK_RADIUS_DP) }
        }
        val shown = minOf(photos.size, slots.size).coerceAtLeast(1)
        val density = resources.displayMetrics.density
        binding.tvSummary.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = (((shown - 1) * STACK_STEP_DP + STACK_GAP_DP) * density).toInt()
        }
    }

    private fun selectOption(chosen: View) {
        listOf(binding.optMessenger, binding.optBundle, binding.optReport).forEach { option ->
            val isChosen = option.clOption === chosen
            option.clOption.isSelected = isChosen
            option.ivOptionIcon.isSelected = isChosen
        }
    }

    private fun observeEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is ShareContract.Effect.SharePhotos -> sharePhotos(effect.photos)
                        is ShareContract.Effect.ShareBundle -> shareFile(effect.file)
                        is ShareContract.Effect.ShareFile -> shareFile(effect.file, effect.mimeType)
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

    private fun shareFile(
        file: File,
        mimeType: String = com.gps.zazor.utils.export.BundleWriter.MIME_ZIP
    ) {
        val uri = uriFor(file) ?: run {
            toast(R.string.share_failed)
            return
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = mimeType
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
