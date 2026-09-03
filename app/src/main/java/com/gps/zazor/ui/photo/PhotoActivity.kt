package com.gps.zazor.ui.photo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.tabs.TabLayoutMediator
import com.gps.zazor.R
import com.gps.zazor.databinding.ActivityPhotoBinding
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.ui.base.BaseActivity
import com.gps.zazor.ui.media.MediaActivity
import com.gps.zazor.ui.photo.collage.container.CollageContainerListener
import com.gps.zazor.ui.photo.collage.photo.CollagePhotoActivity
import com.gps.zazor.ui.photo.editPhoto.EditPhotoBottomSheet
import com.gps.zazor.ui.photo.di.injectViewModel
import com.gps.zazor.ui.settings.SettingsActivity
import com.gps.zazor.utils.extensions.gone
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.extensions.loadImage
import com.gps.zazor.utils.extensions.show
import com.gps.zazor.utils.viewBinding.viewBinding

class PhotoActivity : BaseActivity<PhotoContract.State, PhotoContract.Event>(R.layout.activity_photo),
    PhotoCallback {

    companion object {

        private const val BASIC_PHOTO_ITEM = 1

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        fun newIntent(context: Context) = Intent(context, PhotoActivity::class.java)
    }

    override val viewModel by injectViewModel()

    private var adapter: PhotoPagerAdapter? = null

    private val binding by viewBinding(ActivityPhotoBinding::bind)

    private val sheetBinding by viewBinding(BottomSheetAddNoteBinding::bind, R.id.clRoot)

    private val addNoteSheet by lazy { EditPhotoBottomSheet(sheetBinding) }

    /** Set once the dialog has been shown so it is not re-launched on every return to the screen. */
    private var permissionsRequested = false

    private var permissionRationaleShown = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            // The camera is what the screen cannot work without; a denied location only drops the
            // coordinates from the stamp. The old check was `granted.values.all { true }`, which is
            // the constant `true` - permissions were reported granted even when the user refused.
            val cameraGranted = result[Manifest.permission.CAMERA] ?: hasPermission(Manifest.permission.CAMERA)
            viewModel.sendEvent(PhotoContract.Event.PermissionResult(cameraGranted))
        }

    override fun observeState(state: PhotoContract.State?) {
        when (state) {
            is PhotoContract.State.Content -> {
                when (state.isPermissionGranted) {
                    true -> setupViewPager()
                    false -> showPermissionDenied()
                    null -> Unit
                }
                state.photoUri?.let { binding.ivLastPhoto.loadImage(it) }
                    ?: binding.ivLastPhoto.hide()
            }
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.ivCapture.setOnClickListener {
            getCurrentPhotoHandler()?.onCapturePhoto()
        }
        binding.ivFlip.setOnClickListener {
            getCurrentPhotoHandler()?.flipCamera()
        }
        binding.ivGrid.setOnClickListener {
            currentCollageListener()?.onGridSelected()
        }
        binding.ivGridHorizontal.setOnClickListener {
            currentCollageListener()?.onHorizontalSelected()
        }
        binding.ivGridVertical.setOnClickListener {
            currentCollageListener()?.onVerticalSelected()
        }
        binding.ivLastPhoto.setOnClickListener {
            openMedia()
        }
    }

    override fun onStart() {
        super.onStart()
        when {
            hasPermission(Manifest.permission.CAMERA) ->
                viewModel.sendEvent(PhotoContract.Event.PermissionResult(true))
            permissionsRequested ->
                viewModel.sendEvent(PhotoContract.Event.PermissionResult(false))
            else -> {
                permissionsRequested = true
                permissionLauncher.launch(REQUIRED_PERMISSIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh the thumbnail after coming back from the gallery or the collage editor.
        viewModel.sendEvent(PhotoContract.Event.EditPhotoClosed)
    }

    @Deprecated("Kept for the existing in-app back handling")
    override fun onBackPressed() {
        if (!addNoteSheet.collapse()) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onCaptured() {
        binding.clPhotoPanel.gone()
        addNoteSheet.show()
    }

    override fun onCollageShown() {
        binding.clCollageGrid.show()
    }

    override fun onPanoramaShown() {
        binding.clCollageGrid.hide()
    }

    override fun onPhotoShown() {
        binding.clCollageGrid.hide()
    }

    override fun openSettings() {
        startActivity(SettingsActivity.newIntent(this))
    }

    override fun openCollagePhoto(index: Int) {
        startActivity(CollagePhotoActivity.newIntent(this, index))
    }

    override fun switchEnabledCapture(isEnabled: Boolean) {
        binding.ivCapture.isEnabled = isEnabled
    }

    override fun collapseEditPhoto() {
        addNoteSheet.behavior.run {
            peekHeight = COLLAPSED_PEEK_HEIGHT
            state = STATE_COLLAPSED
        }
    }

    override fun onPhotoEditCancel() {
        addNoteSheet.clearAll()
        addNoteSheet.hide()
        viewModel.sendEvent(PhotoContract.Event.EditPhotoClosed)
        binding.clPhotoPanel.show()
    }

    override fun clearAll() {
        addNoteSheet.clearAll()
    }

    private fun setupViewPager() {
        if (adapter != null) return
        adapter = PhotoPagerAdapter(this).also { pagerAdapter ->
            binding.vpPhoto.run {
                adapter = pagerAdapter
                // Tabs switch modes; swiping does not, so a stray finger during a shot cannot
                // change the mode out from under the user.
                isUserInputEnabled = false
                setCurrentItem(BASIC_PHOTO_ITEM, false)
            }
            TabLayoutMediator(binding.tlPhotos, binding.vpPhoto) { tab, position ->
                tab.text = pagerAdapter.titleOf(position)
            }.attach()
        }
    }

    private fun openMedia() {
        startActivity(MediaActivity.newIntent(this))
    }

    /**
     * Once "don't ask again" is set the system dialog never appears again, so point the user at
     * the app settings instead of leaving them on a permanently blank screen. Shown at most once
     * per visit to the screen.
     */
    private fun showPermissionDenied() {
        if (permissionRationaleShown) return
        permissionRationaleShown = true
        Toast.makeText(this, R.string.give_permission, Toast.LENGTH_LONG).show()
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
    }

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun currentCollageListener(): CollageContainerListener? =
        currentFragment() as? CollageContainerListener

    private fun getCurrentPhotoHandler(): PhotoHandler? = currentFragment() as? PhotoHandler

    private fun currentFragment() = adapter?.currentFragment(binding.vpPhoto)
}

private const val COLLAPSED_PEEK_HEIGHT = 250
