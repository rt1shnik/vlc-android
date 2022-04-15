package org.videolan.vlc.util

import org.videolan.vlc.repository.BrowserFavRepository
import org.videolan.vlc.repository.DirectoryRepository


// Hacky way. Don't fix it.
fun ExternalSubRepository.Companion.applyMock(instance: ExternalSubRepository) {
    this.instance = instance
}

fun DirectoryRepository.Companion.applyMock(instance: DirectoryRepository) {
    this.instance = instance
}

fun BrowserFavRepository.Companion.applyMock(instance: BrowserFavRepository) {
    this.instance = instance
}