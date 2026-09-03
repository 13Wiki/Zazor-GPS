package com.gps.zazor.ui.photo

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.gps.zazor.R
import com.gps.zazor.ui.photo.basic.BasicPhotoFragment
import com.gps.zazor.ui.photo.collage.container.CollageContainerFragment
import com.gps.zazor.ui.photo.panorama.PanoramaFragment

/**
 * Pages of the capture screen.
 *
 * [FragmentStateAdapter] keeps only the current page's fragment attached, so exactly one tab
 * holds the camera at a time - the guarantee the old ViewPager 1 adapter got from
 * `BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT`, now the default behaviour rather than a flag.
 */
class PhotoPagerAdapter(private val activity: FragmentActivity) : FragmentStateAdapter(activity) {

    /** Tabs in display order; the camera sits in the middle and is selected on open. */
    enum class Page(@StringRes val title: Int) {
        COLLAGE(R.string.collage),
        PHOTO(R.string.photo),
        PANORAMA(R.string.panorama)
    }

    override fun getItemCount(): Int = Page.entries.size

    override fun createFragment(position: Int): Fragment = when (Page.entries[position]) {
        Page.COLLAGE -> CollageContainerFragment()
        Page.PHOTO -> BasicPhotoFragment()
        Page.PANORAMA -> PanoramaFragment()
    }

    fun titleOf(position: Int): String = activity.getString(Page.entries[position].title)

    /**
     * The fragment the pager is actually showing.
     *
     * Resolved through the FragmentManager rather than from a locally held list, so it stays
     * correct after a process death restores its own fragment instances - previously the shutter
     * and flip buttons silently addressed detached objects.
     */
    fun currentFragment(pager: ViewPager2): Fragment? =
        activity.supportFragmentManager.findFragmentByTag("f${getItemId(pager.currentItem)}")
}
