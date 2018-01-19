package io.eodc.ripple;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Representation of a generic message
 */

public class Message implements Parcelable{
    private boolean fromUser;
    private long date;

    Message(boolean fromUser, long date) {
        this.fromUser = fromUser;
        this.date = date;
    }

    Message(long date) {
        this.date = date;
    }

    Message(Parcel in) {
        fromUser = in.readByte() != 0;
        date = in.readLong();
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public boolean isFromUser() {
        return fromUser;
    }

    public void setFromUser() {
        this.fromUser = true;
    }

    public long getDate() {
        return date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte(fromUser ? (byte) 1 : 0);
        parcel.writeLong(date);
    }
}
