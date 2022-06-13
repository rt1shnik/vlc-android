package org.videolan.medialibrary.stubs;

import android.os.Parcel;

import org.videolan.medialibrary.Tools;
import org.videolan.medialibrary.interfaces.media.Genre;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

import java.util.ArrayList;

public class StubGenre extends Genre {

    private StubDataSource dt = StubDataSource.getInstance();

    public StubGenre(long id, String title, int nbTracks, int nbPresentTracks) { super(id, title, nbTracks, nbPresentTracks); }
    public StubGenre(Parcel in) { super(in); }

    public MediaWrapper[] getTracks(boolean withThumbnail, int sort, boolean desc, boolean includeMissing) {
        ArrayList<MediaWrapper> results = new ArrayList<>();
        for (MediaWrapper media : dt.mAudioMediaWrappers) {
            if (media.getGenre().equals(this.getTitle())) results.add(media);
        }
        return dt.sortMedia(results, sort, desc);
    }

    public MediaWrapper[] getPagedTracks(boolean withThumbnail, int sort, boolean desc, boolean includeMissing, int nbItems, int offset) {
        ArrayList<MediaWrapper> results = new ArrayList<>();
        for (MediaWrapper media : dt.mAudioMediaWrappers) {
            if (media.getGenre().equals(this.getTitle())) results.add(media);
        }
        return dt.sortMedia(dt.secureSublist(results, offset, offset + nbItems), sort, desc);
    }

    public int getTracksCount() {
        int count = 0;
        for (MediaWrapper media : dt.mAudioMediaWrappers) {
            if (media.getGenre().equals(this.getTitle())) count++;
        }
        return count;
    }


    public MediaWrapper[] searchTracks(String query, int sort, boolean desc, boolean includeMissing, int nbItems, int offset) {
        ArrayList<MediaWrapper> results = new ArrayList<>();
        for (MediaWrapper media : dt.mAudioMediaWrappers) {
            if (media.getGenre().equals(this.getTitle()) &&
                    Tools.hasSubString(media.getTitle(), query)) {
                results.add(media);
            }
        }
        return dt.sortMedia(dt.secureSublist(results, offset, offset + nbItems), sort, desc);
    }

    public int searchTracksCount(String query) {
        int count = 0;
        for (MediaWrapper media : dt.mAudioMediaWrappers) {
            if (media.getGenre().equals(this.getTitle()) &&
                    Tools.hasSubString(media.getTitle(), query)) {
                count++;
            }
        }
        return count;
    }
}
