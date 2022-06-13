package org.videolan.medialibrary.interfaces.media;

import android.os.Parcel;
import android.os.Parcelable;

import org.videolan.medialibrary.MLServiceLocator;
import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.media.MediaLibraryItem;

public abstract class Genre extends MediaLibraryItem {

    private int mPresentTracksCount;
    private int mTracksCount;

    public Genre(long id, String title, int nbTracks, int nbPresentTracks) {
        super(id, title);
        this.mTracksCount = nbTracks;
        this.mPresentTracksCount = nbPresentTracks;
    }
    public Genre(Parcel in) { super(in); }

    abstract public MediaWrapper[] getTracks(boolean withThumbnail, int sort, boolean desc, boolean includeMissing);
    abstract public MediaWrapper[] getPagedTracks(boolean withThumbnail, int sort, boolean desc, boolean includeMissing, int nbItems, int offset);
    abstract public MediaWrapper[] searchTracks(String query, int sort, boolean desc, boolean includeMissing, int nbItems, int offset);
    abstract public int searchTracksCount(String query);

    public MediaWrapper[] getPagedTracks(int sort, boolean desc, boolean includeMissing, int nbItems, int offset) {
        return getPagedTracks(false, sort, desc, includeMissing, nbItems, offset);
    }

    public MediaWrapper[] getTracks(int sort, boolean desc, boolean includeMissing) {
        return getTracks(false, sort, desc, includeMissing);
    }

    public MediaWrapper[] getTracks() {
        return getTracks(false, Medialibrary.SORT_ARTIST, false, true);
    }
    @Override
    public int getItemType() {
        return TYPE_GENRE;
    }

    @Override
    public int getTracksCount() {
        return this.mPresentTracksCount;
    }

    public int getPresentTracksCount() {
        return mPresentTracksCount;
    }

    public static Parcelable.Creator<Genre> CREATOR
            = new Parcelable.Creator<Genre>() {
        @Override
        public Genre createFromParcel(Parcel in) {
            return MLServiceLocator.getAbstractGenre(in);
        }

        @Override
        public Genre[] newArray(int size) {
            return new Genre[size];
        }
    };

}
