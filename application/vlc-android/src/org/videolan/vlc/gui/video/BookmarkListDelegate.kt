package org.videolan.vlc.gui.video

import androidx.constraintlayout.widget.ConstraintLayout
import org.videolan.vlc.gui.helpers.BookmarkAdapter

interface BookmarkListDelegate: BookmarkAdapter.IBookmarkManager {
    fun show()
    fun setProgressHeight(toFloat: Float)
    fun hide()

    val visible: Boolean
    var visibilityListener: () -> Unit
    var markerContainer: ConstraintLayout
}