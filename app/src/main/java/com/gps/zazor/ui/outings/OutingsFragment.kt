package com.gps.zazor.ui.outings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gps.zazor.R
import com.gps.zazor.data.models.Outing
import com.gps.zazor.databinding.FragmentOutingsBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.media.MediaCallback
import com.gps.zazor.ui.outings.di.injectViewModel
import com.gps.zazor.utils.export.TrackFormat
import com.gps.zazor.utils.export.shareTrack
import com.gps.zazor.utils.viewBinding.viewBinding
import kotlinx.coroutines.launch

/**
 * The outings log: a card per day walked, newest first. The map of any one day is a screen of its
 * own, opened by tapping its card.
 */
class OutingsFragment : BaseFragment<OutingsContract.State, OutingsContract.Event>(
    R.layout.fragment_outings
) {

    override val viewModel by injectViewModel()

    private val binding by viewBinding(FragmentOutingsBinding::bind)

    private val adapter by lazy {
        OutingsAdapter(
            onClick = { (activity as? MediaCallback)?.openOutingMap(it.date.toEpochDay()) },
            onDelete = ::confirmDelete,
            onShare = { outing -> showExportMenu(outing) }
        )
    }

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
        binding.ivDeleteAll.setOnClickListener { confirmDeleteAll() }
        observeEffects()
    }

    override fun onResume() {
        super.onResume()
        viewModel.sendEvent(OutingsContract.Event.Reload)
    }

    private fun render(state: OutingsContract.State.Content) {
        adapter.submitList(state.outings)
        val hasAny = state.outings.isNotEmpty()
        binding.tvEmpty.isVisible = !hasAny
        binding.rvOutings.isVisible = hasAny
        // With nothing to delete, neither the red button nor the warning about it has a subject.
        binding.llNote.isVisible = hasAny
        binding.ivDeleteAll.isVisible = hasAny
    }

    private fun confirmDelete(outing: Outing) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.outing_delete)
            .setMessage(R.string.outing_delete_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.sendEvent(OutingsContract.Event.DeleteOuting(outing))
            }
            .setNegativeButton(R.string.cancel_action, null)
            .show()
    }

    private fun confirmDeleteAll() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.outings_delete_all)
            .setMessage(R.string.outings_delete_all_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.sendEvent(OutingsContract.Event.DeleteAll)
            }
            .setNegativeButton(R.string.cancel_action, null)
            .show()
    }

    /** Anchored on the card's own button, so it is obvious which day is being sent. */
    private fun showExportMenu(outing: Outing) {
        val anchor = binding.rvOutings
            .findViewHolderForAdapterPosition(adapter.currentList.indexOf(outing))
            ?.itemView ?: binding.rvOutings
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

    private fun observeEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is OutingsContract.Effect.Deleted -> toast(R.string.outing_deleted)
                        is OutingsContract.Effect.Exported ->
                            shareTrack(effect.file, effect.format)
                        is OutingsContract.Effect.ExportFailed -> toast(R.string.export_failed)
                    }
                }
            }
        }
    }

    private fun toast(text: Int) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
    }
}
