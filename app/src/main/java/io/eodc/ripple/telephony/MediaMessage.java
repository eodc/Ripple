package io.eodc.ripple.telephony;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Representation of a Media Message
 */

public class MediaMessage extends Message implements Parcelable {

    private long id;
    private Bitmap media;

    public MediaMessage(long id) {
        super(Message.MMS);
        this.id = id;
    }

    public MediaMessage(long date, Bitmap media) {
        super(date, Message.MMS);
        this.media = media;
    }

    public long getId() {
        return id;
    }

    public Bitmap getMedia() {
        return media;
    }

    public void setMedia(Bitmap media) {
        this.media = media;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
    }
}
