package org.videolan.resources.util

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.resources.*
import org.videolan.tools.*
import java.io.File
import kotlin.coroutines.resume

fun Context.launchForeground(intent: Intent) {
    try {
        startService(intent)
    } catch (e: IllegalStateException) {
        //wait for the UI thread to be ready
        val ctx = this
        AppScope.launch(Dispatchers.Main) {
            intent.putExtra("foreground", true)
            ContextCompat.startForegroundService(ctx, intent)
        }
    }
}