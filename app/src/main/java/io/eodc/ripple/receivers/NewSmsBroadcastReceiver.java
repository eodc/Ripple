package io.eodc.ripple.receivers;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/**
 * Receives a new SMS and adds it to the local SMS provider for later use
 */

public class NewSmsBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras != null) {
            Object[] pdus = (Object[]) extras.get("pdus");
            InsertMessageHandler insertHandler = new InsertMessageHandler(context.getContentResolver());
            for (Object pdu : pdus) {
                SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
                ContentValues values = new ContentValues();
                long messageReceived = System.currentTimeMillis();

                values.put(Telephony.Sms.ADDRESS, msg.getOriginatingAddress());
                values.put(Telephony.Sms.BODY, msg.getMessageBody());
                values.put(Telephony.Sms.DATE, messageReceived);
                values.put(Telephony.Sms.DATE_SENT, msg.getServiceCenterAddress());
                insertHandler.startInsert(0, null, Telephony.Sms.Inbox.CONTENT_URI, values);

            }
        }
    }
    static class InsertMessageHandler extends AsyncQueryHandler {
        InsertMessageHandler(ContentResolver cr) {
            super(cr);
        }
    }
}
