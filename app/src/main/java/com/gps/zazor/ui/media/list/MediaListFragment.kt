package com.gps.zazor.ui.media.list

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.gps.zazor.BuildConfig
import com.gps.zazor.R
import com.gps.zazor.data.models.Photo
import com.gps.zazor.databinding.FragmentMediaListBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.media.MediaCallback
import com.gps.zazor.ui.media.list.di.injectViewModel
import com.gps.zazor.utils.audio.VoiceNotePlayer
import com.gps.zazor.utils.export.TrackFormat
import com.gps.zazor.utils.viewBinding.viewBinding
import androidx.appcompat.widget.PopupMenu
import java.io.File

class MediaListFragment : BaseFragment<MediaListContract.State, MediaListContract.Event>(R.layout.fragment_media_list) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentMediaListBinding::bind)

    private var mediaCallback: MediaCallback? = null

    private var adapter: MediaListAdapter? = null

    private val voicePlayer = VoiceNotePlayer()

    private val onItemSwipeListener = object : OnItemSwipeListener<Photo> {
        override fun onItemSwiped(position: Int, direction: OnItemSwipeListener.SwipeDirection, item: Photo): Boolean {
            viewModel.sendEvent(MediaListContract.Event.DeletePhoto(item))
            Toast.makeText(requireContext(), R.string.removed, Toast.LENGTH_SHORT).show()
            return true
        }
    }

    override fun observeState(state: MediaListContract.State?) {
        when (state) {
            is MediaListContract.State.Initial -> showPhotos(state.photos)
            is MediaListContract.State.ClearSelectedMode -> {
                adapter?.clearSelection()
                binding.ivShare.isVisible = false
            }
            is MediaListContract.State.ShareSelectedPhotos -> shareMedias(state.photos)
            else -> Unit
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mediaCallback = context as? MediaCallback
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.ivShare.setOnClickListener {
            viewModel.sendEvent(MediaListContract.Event.SharePhotos)
        }
        binding.ivExport.setOnClickListener(::showExportMenu)
        observeEffects()
    }

    override fun onPause() {
        voicePlayer.stop()
        super.onPause()
    }

    override fun onDestroyView() {
        voicePlayer.stop()
        adapter = null
        super.onDestroyView()
    }

    /** Tapping the playing note stops it; tapping another switches to it. */
    private fun toggleVoiceNote(photo: Photo) {
        val path = photo.voiceNotePath ?: return
        if (voicePlayer.playingPath == path) {
            voicePlayer.stop()
            return
        }
        if (!voicePlayer.play(path)) {
            Toast.makeText(requireContext(), R.string.voice_note_empty, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDetach() {
        mediaCallback = null
        super.onDetach()
    }

    override fun onBackPressed(): Boolean = viewModel.backPressed()

    /**
     * Feeds a new list into the existing adapter instead of building a new one on every state,
     * which used to reset scroll position and selection on each delete.
     */
    private fun showPhotos(photos: List<Photo>) {
        binding.tvEmpty.isVisible = photos.isEmpty()
        adapter?.let {
            it.submit(photos)
            return
        }
        adapter = MediaListAdapter(
            photos, ::openEditPhoto, ::onMediaSelected, ::shareMedia, ::turnOnSelectionMode,
            ::toggleVoiceNote
        ).also {
            binding.rvPhotos.run {
                adapter = it
                swipeListener = onItemSwipeListener
                orientation = DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_VERTICAL_DRAGGING
                disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.RIGHT)
                disableDragDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.UP)
                disableDragDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.DOWN)
            }
        }
    }

    private fun openEditPhoto(photo: Photo) {
        mediaCallback?.editPhoto(photoPath = photo.path)
    }

    private fun onMediaSelected(photo: Photo, isSelected: Boolean) {
        viewModel.sendEvent(MediaListContract.Event.SwitchPhotoSelected(photo, isSelected))
    }

    private fun turnOnSelectionMode() {
        viewModel.sendEvent(MediaListContract.Event.TurnOnSelectionMode)
        adapter?.isSelectableMode = true
        binding.ivShare.isVisible = true
    }

    private fun shareMedia(photo: Photo) {
        uriFor(photo)?.let { uri ->
            startActivity(
                Intent.createChooser(
                    Intent(ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = MIME_IMAGE
                        // Without this flag the receiving app gets a SecurityException.
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    getString(R.string.share)
                )
            )
        }
    }

    private fun shareMedias(photos: List<Photo>) {
        val uris = ArrayList(photos.mapNotNull(::uriFor))
        if (uris.isEmpty()) return
        startActivity(
            Intent.createChooser(
                Intent(ACTION_SEND_MULTIPLE).apply {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    type = MIME_IMAGE
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share)
            )
        )
    }

    /** One-shot results, delivered once each - never replayed when the screen comes back. */
    private fun observeEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is MediaListContract.Effect.TrackExported ->
                            shareTrack(effect.file, effect.format)
                        is MediaListContract.Effect.ExportEmpty ->
                            toast(getString(R.string.export_empty))
                        is MediaListContract.Effect.ExportFailed ->
                            toast(getString(R.string.export_failed))
                        is MediaListContract.Effect.AddressesFilled ->
                            toast(getString(R.string.addresses_filled, effect.count))
                    }
                }
            }
        }
    }

    private fun toast(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    /** GPX opens in navigators, KML in Google Earth - let the user pick rather than guessing. */
    private fun showExportMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, 0, 0, R.string.export_gpx)
            menu.add(0, 1, 1, R.string.export_kml)
            setOnMenuItemClickListener { item ->
                val format = if (item.itemId == 0) TrackFormat.GPX else TrackFormat.KML
                viewModel.sendEvent(MediaListContract.Event.ExportTrack(format))
                true
            }
        }.show()
    }

    private fun shareTrack(file: File, format: TrackFormat) {
        val uri = try {
            FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            Toast.makeText(requireContext(), R.string.export_failed, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent.createChooser(
                Intent(ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = format.mimeType
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share)
            )
        )
    }

    private fun uriFor(photo: Photo): Uri? =
        try {
            FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                File(photo.path)
            )
        } catch (e: IllegalArgumentException) {
            null
        }
}

private const val MIME_IMAGE = "image/*"
