package io.eodc.ripple.telephony;

/**
 * Representation of a conversation thread
 */

public class Conversation {
    private String name;
    private String phoneNum;
    private String lastMsgBody;
    private String contactPhotoURI;
    private long timestamp;


    public Conversation(String phoneNum, String lastMsgBody, long timestamp) {
        this.phoneNum = phoneNum;
        this.lastMsgBody = lastMsgBody;
        this.timestamp = timestamp;
    }

    public String getPhoneNum() {
        return phoneNum;
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

    public void setName(String name) {
        this.name = name;
    }

    public String getContactPhotoURI() {
        return contactPhotoURI;
    }

    public void setContactPhotoURI(String contactPhotoURI) {
        this.contactPhotoURI = contactPhotoURI;
    }
}
