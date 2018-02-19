package io.eodc.ripple.telephony;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Comparator;

/**
 * Representation of a generic message
 */

public class Message implements Parcelable {

    public static final int SMS = 0;
    public static final int MMS = 1;

    private boolean fromUser;
    private long date;
    private int discrim;

    Message(int discrim) {
        this.discrim = discrim;
    }

    Message(long date, int discrim) {
        this.date = date;
        this.discrim = discrim;
    }

    Message(boolean fromUser, long date, int discrim) {
        this.fromUser = fromUser;
        this.date = date;
        this.discrim = discrim;
    }

    Message(Parcel in) {
        fromUser = in.readByte() != 0;
        date = in.readLong();
        discrim = in.readInt();
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

    public void setFromUser() { fromUser = true; }

    public void setDate(long date) {
        this.date = date;
    }

    public long getDate() {
        return date;
    }

    public int getDiscrim() {
        return discrim;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte(fromUser ? (byte) 1 : 0);
        parcel.writeLong(date);
        parcel.writeInt(discrim);
    }

    public static class SortByDate implements Comparator<Message> {
        @Override
        public int compare(Message a, Message b) {
            return Long.compare(b.getDate(), a.getDate());
        }
    }
}
