package com.gps.zazor.ui.media.list

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeRecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.listener.OnItemSwipeListener
import com.gps.zazor.BuildConfig
import com.gps.zazor.R
import com.gps.zazor.data.models.Photo
import com.gps.zazor.databinding.FragmentMediaListBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.media.MediaCallback
import com.gps.zazor.ui.media.list.di.injectViewModel
import com.gps.zazor.ui.photo.PhotoCallback
import com.gps.zazor.utils.viewBinding.viewBinding
import java.io.File

class MediaListFragment : BaseFragment<MediaListContract.State, MediaListContract.Event>(R.layout.fragment_media_list) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentMediaListBinding::bind)

    private var mediaCallback: MediaCallback? = null

    private lateinit var adapter: MediaListAdapter

    private val onItemSwipeListener = object : OnItemSwipeListener<Photo> {
        override fun onItemSwiped(position: Int, direction: OnItemSwipeListener.SwipeDirection, item: Photo): Boolean {
            viewModel.sendEvent(MediaListContract.Event.DeletePhoto(item))
            Toast.makeText(requireContext(), R.string.removed, Toast.LENGTH_SHORT).show()
            return true
        }
    }

    override fun observeState(state: MediaListContract.State?) {
        when (state) {
            is MediaListContract.State.Initial -> {
                binding.rvPhotos.run {
                    adapter = MediaListAdapter(state.photos, ::openEditPhoto,
                        ::onMediaSelected, ::shareMedia, ::turnOnSelectionMode).also {
                        this@MediaListFragment.adapter = it
                    }
                    swipeListener = onItemSwipeListener
                    orientation = DragDropSwipeRecyclerView.ListOrientation.VERTICAL_LIST_WITH_VERTICAL_DRAGGING
                    disableSwipeDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.RIGHT)
                    disableDragDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.UP)
                    disableDragDirection(DragDropSwipeRecyclerView.ListOrientation.DirectionFlag.DOWN)
                }
            }
            is MediaListContract.State.ClearSelectedMode -> {
                adapter.run {
                    isSelectableMode = false
                    binding.ivShare.isVisible = false
                    notifyDataSetChanged()
                }
            }
            is MediaListContract.State.ShareSelectedPhotos -> shareMedias(state.photos)
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
            activity?.onBackPressed()
        }
        binding.ivShare.setOnClickListener {
            viewModel.sendEvent(MediaListContract.Event.SharePhotos)
        }
    }

    override fun onDetach() {
        mediaCallback = null
        super.onDetach()
    }

    override fun onBackPressed(): Boolean {
        return viewModel.backPressed()
    }

    private fun openEditPhoto(photo: Photo) {
        mediaCallback?.editPhoto(photoPath = photo.path)
    }

    private fun onMediaSelected(photo: Photo, isSelected: Boolean) {
        viewModel.sendEvent(MediaListContract.Event.SwitchPhotoSelected(photo, isSelected))
    }

    private fun turnOnSelectionMode() {
        viewModel.sendEvent(MediaListContract.Event.TurnOnSelectionMode)
        adapter.run {
            isSelectableMode = true
            notifyDataSetChanged()
        }
        binding.ivShare.isVisible = true
    }

    private fun shareMedia(photo: Photo) {
        requireActivity().startActivity(Intent(ACTION_SEND).apply {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                File(photo.path))
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/*"
        })
    }

    private fun shareMedias(photos: List<Photo>) {
        if (photos.isEmpty()) return
        ArrayList(photos.map { photo ->
            FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                File(photo.path))
        }).let { uris ->
            requireActivity().startActivity(Intent(ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                type = "image/*"
            })
        }
    }
}