/*
 * *************************************************************************
 *  Util.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.helpers

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.media.MediaRouter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.*
import android.text.TextUtils
import android.view.*
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ActionMode
import androidx.cardview.widget.CardView
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.databinding.BindingAdapter
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import org.videolan.libvlc.util.AndroidUtil
import org.videolan.medialibrary.MLServiceLocator
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.*
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.*
import org.videolan.resources.util.launchForeground
import org.videolan.tools.*
import org.videolan.vlc.R
import org.videolan.vlc.gui.*
import org.videolan.vlc.gui.dialogs.*
import org.videolan.vlc.media.MediaUtils
import org.videolan.vlc.media.getAll
import org.videolan.vlc.util.FileUtils

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
object UiTools {
    var currentNightMode: Int = 0
    private val TAG = "VLC/UiTools"
    private var DEFAULT_COVER_VIDEO_DRAWABLE: BitmapDrawable? = null

    private val sHandler = Handler(Looper.getMainLooper())
    const val DELETE_DURATION = 3000

    private fun getSnackAnchorView(activity: Activity, overAudioPlayer: Boolean = false) =
            if (activity is BaseActivity && activity.getSnackAnchorView(overAudioPlayer) != null) activity.getSnackAnchorView(overAudioPlayer) else activity.findViewById(android.R.id.content)

    /**
     * Print an on-screen message to alert the user
     */
    fun snacker(activity: Activity, stringId: Int, overAudioPlayer: Boolean = false) {
        val view = getSnackAnchorView(activity, overAudioPlayer) ?: return
        val snack = Snackbar.make(view, stringId, Snackbar.LENGTH_SHORT)
        if (overAudioPlayer) snack.setAnchorView(R.id.time)
        snack.show()
    }

    /**
     * Print an on-screen message to alert the user
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun snacker(activity:Activity, message: String) {
        val view = getSnackAnchorView(activity) ?: return
        val snack = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        if (AndroidUtil.isLolliPopOrLater)
            snack.view.elevation = view.resources.getDimensionPixelSize(R.dimen.audio_player_elevation).toFloat()
        snack.show()
    }

    /**
     * Print an on-screen message to alert the user, with undo action
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun snackerConfirm(activity: Activity, message: String, overAudioPlayer: Boolean = false, @StringRes confirmMessage:Int = R.string.ok, action: () -> Unit) {
        val view = getSnackAnchorView(activity, overAudioPlayer) ?: return
        val snack = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(confirmMessage) { action.invoke() }
        if (overAudioPlayer) snack.setAnchorView(R.id.time)
        if (AndroidUtil.isLolliPopOrLater)
            snack.view.elevation = view.resources.getDimensionPixelSize(R.dimen.audio_player_elevation).toFloat()
        snack.show()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun CoroutineScope.snackerConfirm(activity:Activity, message: String, action: suspend() -> Unit) {
        val view = getSnackAnchorView(activity) ?: return
        val snack = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(R.string.ok) { launch { action.invoke() } }
        if (AndroidUtil.isLolliPopOrLater)
            snack.view.elevation = view.resources.getDimensionPixelSize(R.dimen.audio_player_elevation).toFloat()
        snack.show()
    }


    /**
     * Print an on-screen message to alert the user, with undo action
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun snackerWithCancel(activity: Activity, message: String, overAudioPlayer: Boolean = false, action: () -> Unit, cancelAction: () -> Unit) {
        val view = getSnackAnchorView(activity, overAudioPlayer) ?: return
        @SuppressLint("WrongConstant") val snack = Snackbar.make(view, message, DELETE_DURATION)
                .setAction(R.string.cancel) {
                    sHandler.removeCallbacks(action)
                    cancelAction.invoke()
                }
        if (AndroidUtil.isLolliPopOrLater)
            snack.view.elevation = view.resources.getDimensionPixelSize(R.dimen.audio_player_elevation).toFloat()
        if (overAudioPlayer) snack.setAnchorView(R.id.time)
        snack.show()
        sHandler.postDelayed(action, DELETE_DURATION.toLong())
    }

    /**
     * Get a resource id from an attribute id.
     *
     * @param context
     * @param attrId
     * @return the resource id
     */
    fun getResourceFromAttribute(context: Context, attrId: Int): Int {
        val a = context.theme.obtainStyledAttributes(intArrayOf(attrId))
        val resId = a.getResourceId(0, 0)
        a.recycle()
        return resId
    }

    fun setKeyboardVisibility(v: View?, show: Boolean) {
        if (v == null) return
        val inputMethodManager = v.context.applicationContext.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        sHandler.post {
            if (show)
                inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED)
            else
                inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    fun FragmentActivity.showVideoTrack(menuListener:(VideoTracksDialog.VideoTrackOption) -> Unit, trackSelectionListener:(Int, VideoTracksDialog.TrackType) -> Unit) {
        if (!isStarted()) return
        val videoTracksDialog = VideoTracksDialog()
        videoTracksDialog.arguments = bundleOf()
        videoTracksDialog.show(supportFragmentManager, "fragment_video_tracks")
        videoTracksDialog.menuItemListener = menuListener
        videoTracksDialog.trackSelectionListener = trackSelectionListener
    }
    fun Context.isTablet() = resources.getBoolean(R.bool.is_tablet)

    @TargetApi(Build.VERSION_CODES.N)
    fun setOnDragListener(activity: Activity) {
        val view = if (AndroidUtil.isNougatOrLater) activity.window.peekDecorView() else null
        view?.setOnDragListener(View.OnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DROP -> {
                    val clipData = event.clipData ?: return@OnDragListener false
                    val itemsCount = clipData.itemCount
                    for (i in 0 until itemsCount) {
                        val permissions = activity.requestDragAndDropPermissions(event)
                        if (permissions != null) {
                            val item = clipData.getItemAt(i)
                            if (item.uri != null)
                                MediaUtils.openUri(activity, item.uri)
                            else if (item.text != null) {
                                val uri = item.text.toString().toUri()
                                val media = MLServiceLocator.getAbstractMediaWrapper(uri)
                                if ("file" != uri.scheme)
                                    media.type = MediaWrapper.TYPE_STREAM
                                MediaUtils.openMedia(activity, media)
                            }
                            return@OnDragListener true
                        }
                    }
                    false
                }
                else -> false
            }
        })
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun setRotationAnimation(activity: Activity) {
        if (!AndroidUtil.isJellyBeanMR2OrLater) return
        val win = activity.window
        val winParams = win.attributes
        winParams.rotationAnimation = if (AndroidUtil.isOOrLater) WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS else WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
        win.attributes = winParams
    }


    fun restartDialog(context: Context) {
        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.restart_vlc))
                .setMessage(context.resources.getString(R.string.restart_message))
                .setPositiveButton(R.string.restart_message_OK) { _, _ -> android.os.Process.killProcess(android.os.Process.myPid()) }
                .setNegativeButton(R.string.restart_message_Later, null)
                .create()
                .show()
    }



    fun deleteSubtitleDialog(context: Context, positiveListener: DialogInterface.OnClickListener, negativeListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(context)
                .setTitle(context.resources.getString(R.string.delete_sub_title))
                .setMessage(context.resources.getString(R.string.delete_sub_message))
                .setPositiveButton(R.string.delete, positiveListener)
                .setNegativeButton(R.string.cancel, negativeListener)
                .create()
                .show()
    }

    fun hasSecondaryDisplay(context: Context): Boolean {
        val mediaRouter = context.getSystemService<MediaRouter>()!!
        val route = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO)
        val presentationDisplay = route?.presentationDisplay
        return presentationDisplay != null
    }

    /**
     * Invalidate the default bitmaps that are different in light and dark modes
     */
    fun invalidateBitmaps() {
        DEFAULT_COVER_VIDEO_DRAWABLE = null
    }

}

/**
 * Set the alignment mode of the specified TextView with the desired align
 * mode from preferences.
 *
 *
 * See @array/list_title_alignment_values
 *
 * @param alignMode Align mode as read from preferences
 * @param t         Reference to the textview
 */
@BindingAdapter("ellipsizeMode")
fun setEllipsizeModeByPref(t: TextView, activated: Boolean) {
    if (!activated) return

    when (Settings.listTitleEllipsize) {
        0 -> {}
        1 -> t.ellipsize = TextUtils.TruncateAt.START
        2 -> t.ellipsize = TextUtils.TruncateAt.END
        3 -> t.ellipsize = TextUtils.TruncateAt.MIDDLE
        4 -> {
            t.ellipsize = TextUtils.TruncateAt.MARQUEE
            t.marqueeRepeatLimit = 1
        }
    }
}

interface MarqueeViewHolder {
    val titleView: TextView?
}

fun enableMarqueeEffect(recyclerView: RecyclerView, handler: Handler) {
    (recyclerView.layoutManager as? LinearLayoutManager)?.let { layoutManager ->
        //Initial animation for already visible items
        launchMarquee(recyclerView, layoutManager, handler)
        //Animation when done scrolling
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                handler.removeCallbacksAndMessages(null)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) launchMarquee(recyclerView, layoutManager, handler)
            }
        })
    }
}

private fun launchMarquee(recyclerView: RecyclerView, layoutManager: LinearLayoutManager, handler: Handler) {
    handler.postDelayed({
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        for (i in firstVisibleItemPosition..lastVisibleItemPosition) {
            val holder = recyclerView.findViewHolderForLayoutPosition(i)
            (holder as? MarqueeViewHolder)?.titleView?.isSelected = true
        }
    }, 1500)
}

/**
 * sets the touch listener for a view
 *
 * @param view            the view
 * @param onTouchListener the listener
 */
@BindingAdapter("touchListener")
fun setTouchListener(view: View, onTouchListener: View.OnTouchListener?) {
    if (onTouchListener != null)
        view.setOnTouchListener(onTouchListener)
}

@BindingAdapter("selected")
fun isSelected(v: View, isSelected: Boolean?) {
    v.isSelected = isSelected!!
}

@BindingAdapter("selectedPadding")
fun selectedPadding(v: View, isSelected: Boolean?) {
    val padding = if (isSelected == true) 16.dp else 0.dp
    v.setPadding(padding, padding, padding, padding)
}

@BindingAdapter("selectedElevation")
fun selectedElevation(v: View, isSelected: Boolean?) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val elevation = if (isSelected == true) 0.dp else 4.dp
        if (v is CardView) v.cardElevation = elevation.toFloat() else v.elevation = elevation.toFloat()
    }
}

fun BaseActivity.applyTheme() {
    forcedTheme()?.let {
        setTheme(it)
        return
    }
    if (Settings.showTvUi) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setTheme(R.style.Theme_VLC_Black)
        return
    }
    AppCompatDelegate.setDefaultNightMode(Integer.valueOf(settings.getString(KEY_APP_THEME, "-1")!!))
}

fun getTvIconRes(mediaLibraryItem: MediaLibraryItem) = when (mediaLibraryItem.itemType) {
    MediaLibraryItem.TYPE_ARTIST -> R.drawable.ic_artist_big
    MediaLibraryItem.TYPE_GENRE -> R.drawable.ic_genre_big
    MediaLibraryItem.TYPE_MEDIA -> {
        val mw = mediaLibraryItem as MediaWrapper
        when (mw.type) {
            MediaWrapper.TYPE_VIDEO -> R.drawable.ic_browser_video_big_normal
            MediaWrapper.TYPE_DIR -> if (mw.uri.scheme == "file") R.drawable.ic_menu_folder_big else R.drawable.ic_menu_network_big
            MediaWrapper.TYPE_AUDIO -> R.drawable.ic_song_big
            else -> R.drawable.ic_browser_unknown_big_normal
        }
    }
    MediaLibraryItem.TYPE_DUMMY -> {
        when (mediaLibraryItem.id) {
            HEADER_VIDEO -> R.drawable.ic_video_collection_big
            HEADER_PERMISSION -> R.drawable.ic_permission_big
            HEADER_DIRECTORIES -> R.drawable.ic_menu_folder_big
            HEADER_NETWORK -> R.drawable.ic_menu_network_big
            HEADER_SERVER -> R.drawable.ic_menu_network_add_big
            HEADER_STREAM -> R.drawable.ic_menu_stream_big
            HEADER_PLAYLISTS -> R.drawable.ic_menu_playlist_big
            HEADER_MOVIES, CATEGORY_NOW_PLAYING_PIP -> R.drawable.ic_browser_movie_big
            HEADER_TV_SHOW -> R.drawable.ic_browser_tvshow_big
            ID_SETTINGS -> R.drawable.ic_menu_preferences_big
            ID_ABOUT_TV -> R.drawable.ic_default_cone
            ID_SPONSOR -> R.drawable.ic_donate_big
            CATEGORY_ARTISTS -> R.drawable.ic_artist_big
            CATEGORY_ALBUMS -> R.drawable.ic_album_big
            CATEGORY_GENRES -> R.drawable.ic_genre_big
            CATEGORY_SONGS, CATEGORY_NOW_PLAYING -> R.drawable.ic_song_big
            else -> R.drawable.ic_browser_unknown_big_normal
        }
    }
    else -> R.drawable.ic_browser_unknown_big_normal
}

suspend fun fillActionMode(context: Context, mode: ActionMode, multiSelectHelper: MultiSelectHelper<MediaLibraryItem>) {
    var realCount = 0
    var length = 0L
    //checks if the selection can be retrieved (if the adapter is populated).
    // If not, we want to prevent changing the title to avoid flashing an invalid empty title
    var ready: Boolean
    withContext(Dispatchers.IO) {
        val selection = multiSelectHelper.getSelection()
        ready = selection.size == multiSelectHelper.getSelectionCount()
        selection.forEach { mediaItem ->
            when (mediaItem) {
                is MediaWrapper -> realCount += 1
                is VideoGroup -> realCount += mediaItem.mediaCount()
                is Folder -> realCount += mediaItem.mediaCount(Folder.TYPE_FOLDER_VIDEO)
            }
        }

        selection.forEach { mediaItem ->
            when (mediaItem) {
                is MediaWrapper -> length += mediaItem.length
                is VideoGroup -> mediaItem.getAll().forEach { length += it.length }
                is Folder -> mediaItem.getAll().forEach { length += it.length }
            }
        }
    }
    if (ready) {
        mode.title = context.getString(R.string.selection_count, realCount)
        mode.subtitle = "${ Tools.millisToString(length)}"
    }
}