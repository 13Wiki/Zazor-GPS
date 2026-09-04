package com.gps.zazor.ui.outings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gps.zazor.BuildConfig
import com.gps.zazor.R
import com.gps.zazor.data.models.Outing
import com.gps.zazor.data.models.Photo
import com.gps.zazor.databinding.FragmentOutingsBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.outings.di.injectViewModel
import com.gps.zazor.utils.Formats
import com.gps.zazor.utils.export.TrackFormat
import com.gps.zazor.utils.viewBinding.viewBinding
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

/**
 * The outings log: a day per card, the chosen day's track drawn above it.
 */
class OutingsFragment : BaseFragment<OutingsContract.State, OutingsContract.Event>(
    R.layout.fragment_outings
) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentOutingsBinding::bind)

    private val adapter by lazy {
        OutingsAdapter { viewModel.sendEvent(OutingsContract.Event.SelectOuting(it)) }
    }

    private var current: Outing? = null

    override fun observeState(state: OutingsContract.State?) {
        when (state) {
            is OutingsContract.State.Content -> render(state)
            else -> Unit
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvOutings.adapter = adapter
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.ivDelete.setOnClickListener { confirmDelete() }
        binding.ivExport.setOnClickListener(::showExportMenu)
        binding.tvOpenInMaps.setOnClickListener { openSelectedPointInMaps() }
        binding.vRoute.onPointSelected = { updateStats() }
        observeEffects()
    }

    override fun onResume() {
        super.onResume()
        viewModel.sendEvent(OutingsContract.Event.Reload)
    }

    private fun render(state: OutingsContract.State.Content) {
        current = state.selected
        adapter.submitList(state.outings)
        adapter.selectedDate = state.selected?.date

        val hasAny = state.outings.isNotEmpty()
        binding.tvEmpty.isVisible = !hasAny
        binding.llStats.isVisible = hasAny
        binding.ivDelete.isVisible = hasAny
        binding.ivExport.isVisible = hasAny
        binding.tvOpenInMaps.isVisible = (state.selected?.pointCount ?: 0) > 0

        binding.vRoute.setPhotos(state.selected?.photos.orEmpty())
        updateStats()
    }

    private fun updateStats() {
        val outing = current ?: return
        val point = binding.vRoute.selectedIndex.takeIf { it >= 0 }?.let { it + 1 }
        binding.tvStats.text = buildString {
            append(
                getString(
                    R.string.outing_summary,
                    outing.pointCount,
                    Formats.distance(requireContext(), outing.distanceMeters)
                )
            )
            if (outing.durationSeconds > 0) {
                append(" · ")
                append(Formats.duration(requireContext(), outing.durationSeconds))
            }
            if (point != null && outing.pointCount > 0) {
                append(" · ")
                append(getString(R.string.route_point_of, point, outing.pointCount))
            }
        }
    }

    private fun confirmDelete() {
        val outing = current ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.outing_delete)
            .setMessage(R.string.outing_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.sendEvent(OutingsContract.Event.DeleteOuting(outing))
            }
            .setNegativeButton(R.string.cancel_action, null)
            .show()
    }

    private fun showExportMenu(anchor: View) {
        val outing = current ?: return
        PopupMenu(requireContext(), anchor).apply {
            menu.add(0, 0, 0, R.string.export_gpx)
            menu.add(0, 1, 1, R.string.export_kml)
            setOnMenuItemClickListener { item ->
                val format = if (item.itemId == 0) TrackFormat.GPX else TrackFormat.KML
                viewModel.sendEvent(OutingsContract.Event.ExportOuting(outing, format))
                true
            }
        }.show()
    }

    /**
     * Hands the selected point to whatever map app is installed, rather than embedding one.
     */
    private fun openSelectedPointInMaps() {
        val photo = binding.vRoute.photoAt(binding.vRoute.selectedIndex) ?: return
        openInMaps(photo)
    }

    private fun openInMaps(photo: Photo) {
        val lat = photo.lat ?: return
        val lng = photo.lng ?: return
        val label = Uri.encode(photo.address?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name))
        val uri = Uri.parse(
            String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)", lat, lng, lat, lng, label)
        )
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), R.string.route_no_maps_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is OutingsContract.Effect.Deleted ->
                            toast(getString(R.string.outing_deleted))
                        is OutingsContract.Effect.Exported ->
                            shareTrack(effect.file, effect.format)
                        is OutingsContract.Effect.ExportFailed ->
                            toast(getString(R.string.export_failed))
                        is OutingsContract.Effect.OpenInMaps -> openInMaps(effect.photo)
                    }
                }
            }
        }
    }

    private fun shareTrack(file: File, format: TrackFormat) {
        val uri = try {
            FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                file
            )
        } catch (e: IllegalArgumentException) {
            toast(getString(R.string.export_failed))
            return
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = format.mimeType
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                getString(R.string.share)
            )
        )
    }

    private fun toast(text: String) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }
}
