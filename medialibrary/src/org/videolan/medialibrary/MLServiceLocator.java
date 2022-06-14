package org.videolan.medialibrary;

import android.net.Uri;
import android.os.Parcel;

import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.interfaces.media.Bookmark;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;
import org.videolan.medialibrary.media.BookmarkImpl;
import org.videolan.medialibrary.media.MediaWrapperImpl;

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
}
