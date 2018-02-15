package io.eodc.ripple.telephony;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import io.eodc.ripple.R;

/**
 * Representation of a conversation thread
 */

public class Conversation implements Parcelable {
    public static final int NO_CONTACT = 0;
    public static final int CONTACT_NO_PIC = 1;
    public static final int CONTACT = 2;

    private String name;
    private String phoneNum;
    private String lastMsgBody;
    private String contactPhotoURI;
    private long timestamp;
    private long threadId;


    public Conversation(long threadId, String phoneNum, String lastMsgBody, long timestamp) {
        this.threadId = threadId;
        this.phoneNum = phoneNum;
        this.lastMsgBody = lastMsgBody;
        this.timestamp = timestamp;
    }

    private Conversation(Parcel in) {
        name = in.readString();
        phoneNum = in.readString();
        lastMsgBody = in.readString();
        contactPhotoURI = in.readString();
        timestamp = in.readLong();
        threadId = in.readLong();
    }

    public static final Creator<Conversation> CREATOR = new Creator<Conversation>() {
        @Override
        public Conversation createFromParcel(Parcel in) {
            return new Conversation(in);
        }

        @Override
        public Conversation[] newArray(int size) {
            return new Conversation[size];
        }
    };

    public String getPhoneNum() {
        return phoneNum;
    }

    public String getHumanReadableNum() {
        PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber unparsedNumber;
        try {
            unparsedNumber = pnu.parse(phoneNum, "US");
        } catch (NumberParseException e) {
            return phoneNum;
        }
        return pnu.format(unparsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    }

    public String getLastMsgBody() {
        return lastMsgBody;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactPhotoURI() {
        return contactPhotoURI;
    }

    public void setContactPhotoURI(String contactPhotoURI) {
        this.contactPhotoURI = contactPhotoURI;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(phoneNum);
        dest.writeString(lastMsgBody);
        dest.writeString(contactPhotoURI);
        dest.writeLong(timestamp);
        dest.writeLong(threadId);
    }


    public static Drawable generateContactPic(int type, Context mContext) {
        Drawable[] layers;

        switch (type) {
            case NO_CONTACT:
                Drawable noContactPicIcon = mContext.getDrawable(R.drawable.ic_no_contact);
                Drawable contactPicBg = new ShapeDrawable();
                Drawable baseLayer = new ShapeDrawable();
                contactPicBg.setColorFilter(Color.argb(128, 128, 128, 128), PorterDuff.Mode.MULTIPLY);
                baseLayer.setColorFilter(Color.WHITE, PorterDuff.Mode.ADD);
                layers = new Drawable[] { baseLayer, contactPicBg, noContactPicIcon };
                break;
            default:
                layers = null;
        }

        // TODO: Return a contact with no photo picture
        assert layers != null;
        return new LayerDrawable(layers);
    }

    public static String parseNumber(String inputNum) {
        PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber parsedNum;
        try {
            // Standardize Number format
            parsedNum = pnu.parse(inputNum, "US");
            return pnu.format(parsedNum, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            // Is a robonumber, ie from Twitter Digits
            return inputNum;
        }
    }
}
