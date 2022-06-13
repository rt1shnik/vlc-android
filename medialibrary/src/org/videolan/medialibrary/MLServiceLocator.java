package org.videolan.medialibrary;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;

import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.interfaces.media.Bookmark;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;
import org.videolan.medialibrary.interfaces.media.Playlist;
import org.videolan.medialibrary.media.BookmarkImpl;
import org.videolan.medialibrary.media.MediaWrapperImpl;
import org.videolan.medialibrary.media.PlaylistImpl;

public class MLServiceLocator {

    private static LocatorMode sMode = LocatorMode.VLC_ANDROID;
    private static volatile Medialibrary instance;

    public static void setLocatorMode(LocatorMode mode) {
        if (instance != null && mode != sMode) {
            throw new IllegalStateException("LocatorMode must be set before Medialibrary initialization");
        }
        MLServiceLocator.sMode = mode;
    }

    public static LocatorMode getLocatorMode() {
        return MLServiceLocator.sMode;
    }

    public static String EXTRA_TEST_STUBS = "extra_test_stubs";

    public enum LocatorMode {
        VLC_ANDROID,
        TESTS,
    }

    public static synchronized Medialibrary getAbstractMedialibrary() {
        if (instance == null) {
            instance = new MedialibraryImpl();
        }
        return instance;
    }

    // MediaWrapper
    public static MediaWrapper getAbstractMediaWrapper(long id, String mrl, long time, float position, long length,
                                                       int type, String title, String filename,
                                                       String artist, String genre, int width, int height,
                                                       String artworkURL, int audio, int spu,
                                                       int trackNumber, int discNumber, long lastModified,
                                                       long seen, boolean isThumbnailGenerated, int releaseDate, boolean isPresent) {
        return new MediaWrapperImpl(id, mrl, time, position, length, type, title,
                filename, artist, genre, width, height, artworkURL,
                audio, spu, trackNumber, discNumber, lastModified, seen, isThumbnailGenerated, releaseDate, isPresent);
    }

    public static MediaWrapper getAbstractMediaWrapper(Uri uri, long time, float position, long length, int type,
                                                       Bitmap picture, String title, String artist,
                                                       String genre,
                                                       int width, int height, String artworkURL,
                                                       int audio, int spu, int trackNumber,
                                                       int discNumber, long lastModified, long seen) {
        return new MediaWrapperImpl(uri, time, position, length, type, picture, title, artist, genre,
                width, height, artworkURL, audio, spu, trackNumber,
                discNumber, lastModified, seen);
    }

    public static MediaWrapper getAbstractMediaWrapper(Uri uri) {
        return new MediaWrapperImpl(uri);
    }

    public static MediaWrapper getAbstractMediaWrapper(IMedia media) {
        return new MediaWrapperImpl(media);
    }

    public static MediaWrapper getAbstractMediaWrapper(Parcel in) {
        return new MediaWrapperImpl(in);
    }

    public static Bookmark getAbstractBookmark(Parcel in) {
        return new BookmarkImpl(in);
    }

    public static Playlist getAbstractPlaylist(Parcel in) {
        return new PlaylistImpl(in);
    }
}
