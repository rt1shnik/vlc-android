/*
 * ************************************************************************
 *  Bookmark.java
 * *************************************************************************
 * Copyright © 2021 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

package org.videolan.medialibrary.interfaces.media;

import android.os.Parcel;
import android.os.Parcelable;

import org.videolan.medialibrary.MLServiceLocator;
import org.videolan.medialibrary.media.MediaLibraryItem;

abstract public class BookmarkBase extends MediaLibraryItem implements Bookmark {
    public long mediaId;
    public long mTime;

    public BookmarkBase(long id, String name, String description, long mediaId, long time) {
        super(id, name);
        this.mediaId = mediaId;
        this.mTime = time;
        this.setDescription(description);
    }


    @Override
    public MediaWrapper[] getTracks() {
        return new MediaWrapper[0];
    }

    @Override
    public int getTracksCount() {
        return 0;
    }

    @Override
    public int getItemType() {
        return TYPE_BOOKMARK;
    }


    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        this.mTime = time;
    }

    abstract public boolean setName(String name);
    abstract public boolean updateDescription(String description);
    abstract public boolean setNameAndDescription(String name, String description);
    abstract public boolean move(long time);


    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeLong(mediaId);
    }

    public static Parcelable.Creator<BookmarkBase> CREATOR = new Parcelable.Creator<BookmarkBase>() {
        @Override
        public BookmarkBase createFromParcel(Parcel in) {
            return MLServiceLocator.getAbstractBookmark(in);
        }

        @Override
        public BookmarkBase[] newArray(int size) {
            return new BookmarkBase[size];
        }
    };

    public BookmarkBase(Parcel in) {
        super(in);
        this.mediaId = in.readLong();
        this.mTime = in.readLong();
    }

    public boolean equals(BookmarkBase other) {
        return mId == other.getId();
    }

    @Override
    public boolean equals(MediaLibraryItem other) {
        if (other instanceof BookmarkBase) return equals((BookmarkBase)other);
        return super.equals(other);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BookmarkBase) return equals((BookmarkBase)obj);
        return super.equals(obj);
    }
}
