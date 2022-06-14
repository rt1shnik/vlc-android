package org.videolan.resources.util

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Environment
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.R

const val LENGTH_WEEK = 7 * 24 * 60 * 60
const val LENGTH_MONTH = 30 * LENGTH_WEEK
const val LENGTH_YEAR = 52 * LENGTH_WEEK
const val LENGTH_2_YEAR = 2 * LENGTH_YEAR

fun getTimeCategory(timestamp: Long): Int {
    val delta = (System.currentTimeMillis() / 1000L) - timestamp
    return when {
        delta < LENGTH_WEEK -> 0
        delta < LENGTH_MONTH -> 1
        delta < LENGTH_YEAR -> 2
        delta < LENGTH_2_YEAR -> 3
        else -> 4
    }
}

fun getTimeCategoryString(context: Context, cat: Int) = when (cat) {
    0 -> context.getString(R.string.time_category_new)
    1 -> context.getString(R.string.time_category_current_month)
    2 -> context.getString(R.string.time_category_current_year)
    3 -> context.getString(R.string.time_category_last_year)
    else -> context.getString(R.string.time_category_older)
}

fun MediaLibraryItem.isSpecialItem() = itemType == MediaLibraryItem.TYPE_ARTIST
        && (id == 1L || id == 2L)

fun MediaLibraryItem.getLength() = when {
    itemType == MediaLibraryItem.TYPE_MEDIA -> (this as MediaWrapper).length
    else -> 0L
}

/**
 * Check if the app has the [Manifest.permission.MANAGE_EXTERNAL_STORAGE] granted
 */
fun isExternalStorageManager(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()
