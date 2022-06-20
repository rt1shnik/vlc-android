package org.videolan.vlc.gui.video

import androidx.constraintlayout.widget.ConstraintLayout

interface BookmarkListDelegate {
    fun show()
    fun setProgressHeight(toFloat: Float)
    fun hide()

    val visible: Boolean
    var visibilityListener: () -> Unit
    var markerContainer: ConstraintLayout
}