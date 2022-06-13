package org.videolan.vlc.gui.helpers


import androidx.core.net.toUri
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.tools.runIO
import org.videolan.tools.stripTrailingSlash

object MedialibraryUtils {

    fun removeDir(path: String) {
        runIO(Runnable { Medialibrary.getInstance().removeFolder(path) })
    }

    fun isScanned(path: String): Boolean {
        //scheme is supported => test if the parent is scanned
        var isScanned = false
        Medialibrary.getInstance().foldersList.forEach search@{
            if (path.stripTrailingSlash().startsWith(it.toUri().toString().stripTrailingSlash())) {
                isScanned = true
                return@search
            }
        }
        return isScanned
    }
}
