package io.eodc.ripple.telephony;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Representation of a TextMessage Object
 */

public class TextMessage extends Message implements Parcelable {

    private String content;

    public TextMessage(String content, boolean fromUser, long date) {
        super(fromUser, date, Message.SMS);
        this.content = content;
    }

    public TextMessage(String content, long date) {
        super(false, date, Message.SMS);
        this.content = content;
    }

    private TextMessage(Parcel in) {
        super(in);
        content = in.readString();
    }

    public String getContent() {
        return content;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(content);
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
