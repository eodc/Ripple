package io.eodc.ripple;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Representation of a TextMessage Object
 */

public class TextMessage implements Parcelable {
    private String content;
    private boolean fromUser;
    private long date;

    public TextMessage() {}

    public TextMessage(String content, long date) {
        this.content = content;
        this.date = date;
    }

    public TextMessage(String content, boolean fromUser, long date) {
        this.content = content;
        this.fromUser = fromUser;
        this.date = date;
    }

    private TextMessage(Parcel in) {
        content = in.readString();
        fromUser = in.readByte() != 0;
        date = in.readLong();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isFromUser() {
        return fromUser;
    }

    public void setFromUser() {
        this.fromUser = true;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(content);
        parcel.writeByte(fromUser ? (byte) 1 : 0);
        parcel.writeLong(date);
    }

    public static final Creator<TextMessage> CREATOR = new Creator<TextMessage>() {
        @Override
        public TextMessage createFromParcel(Parcel in) {
            return new TextMessage(in);
        }

        @Override
        public TextMessage[] newArray(int size) {
            return new TextMessage[size];
        }
    };
}
