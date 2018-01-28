package io.eodc.ripple.util;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Collections;
import java.util.List;

import io.eodc.ripple.adapters.ConversationListAdapter;
import io.eodc.ripple.adapters.MessageHistoryAdapter;
import io.eodc.ripple.telephony.Conversation;
import io.eodc.ripple.telephony.TextMessage;

/**
 * Helper for querying the Telephony and Contact content providers
 */

public class QueryHelper {
    private static AsyncQueryHandler queryHandler;
    private static final int INBOX_TOKEN = 0;
    private static final int SENT_TOKEN = 1;
    private static final int CONVERSATION_QUERY_TOKEN = 2;

    @SuppressLint("HandlerLeak")
    public static void queryMessages(ContentResolver cr, final Context context, final String phoneNum, final List<Long> msgDates, final List<TextMessage> messages, final RecyclerView rv) {
        queryHandler = new AsyncQueryHandler(cr) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                int addrIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                while (cursor.moveToNext()) {
                    PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
                    Phonenumber.PhoneNumber parsedNum;
                    String parsedNumStr;
                    try {
                        parsedNum = pnu.parse(cursor.getString(addrIndex), "US");
                        parsedNumStr = pnu.format(parsedNum, PhoneNumberUtil.PhoneNumberFormat.E164);
                    } catch (NumberParseException e) {
                        parsedNumStr = cursor.getString(addrIndex);
                    }

                    if (parsedNumStr.equals(phoneNum)) {
                        TextMessage newMsg = new TextMessage(cursor.getString(bodyIndex), cursor.getLong(dateIndex));

                        if (token == INBOX_TOKEN) {
                            messages.add(newMsg);
                            msgDates.add(newMsg.getDate());
                        } else if (token == SENT_TOKEN) {
                            newMsg.setFromUser();
                            int index = Math.abs(Collections.binarySearch(msgDates, newMsg.getDate(), Collections.reverseOrder()) + 1);
                            msgDates.add(index, newMsg.getDate());
                            messages.add(index, newMsg);
                        }
                    }
                }
                LinearLayoutManager rvLayoutMan = new LinearLayoutManager(context);
                rvLayoutMan.setReverseLayout(true);
                rvLayoutMan.setStackFromEnd(true);
                rv.setLayoutManager(rvLayoutMan);
                rv.setAdapter(new MessageHistoryAdapter(messages, context));

                rv.scrollToPosition(0);
            }
        };
        queryHandler.startQuery(INBOX_TOKEN,
                null,
                Telephony.Sms.Inbox.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE},
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER);
        queryHandler.startQuery(SENT_TOKEN,
                null,
                Telephony.Sms.Sent.CONTENT_URI,
                new String[]{Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE},
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER);
    }

    @SuppressLint("HandlerLeak")
    public static void queryContacts(final ContentResolver cr, final Context context, final List<Conversation> conversations, final RecyclerView rv) {
        queryHandler = new AsyncQueryHandler(cr) {
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                int addrIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                PhoneNumberUtil pnu = PhoneNumberUtil.getInstance();
                while (cursor.moveToNext()) {
                    Phonenumber.PhoneNumber parsedNum;
                    String parsedNumStr;
                    try {
                        // Standardize Number format
                        parsedNum = pnu.parse(cursor.getString(addrIndex), "US");
                        parsedNumStr = pnu.format(parsedNum, PhoneNumberUtil.PhoneNumberFormat.E164);

                    } catch (NumberParseException e) {
                        // Is a robonumber, ie from Twitter Digits
                        parsedNumStr = cursor.getString(addrIndex);
                    }
                    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(parsedNumStr));
                    Cursor contactsLookupCursor = cr.query(uri, new String[] {
                                    ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                                    ContactsContract.PhoneLookup.DISPLAY_NAME },
                            null, null, null);
                    Conversation conversation = new Conversation(parsedNumStr, cursor.getString(bodyIndex), cursor.getLong(dateIndex));
                    if (contactsLookupCursor != null && contactsLookupCursor.moveToNext()) {
                        conversation.setContactPhotoURI(contactsLookupCursor.getString(contactsLookupCursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)));
                        conversation.setName(contactsLookupCursor.getString(contactsLookupCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)));
                    }
                    conversations.add(conversation);
                    contactsLookupCursor.close();
                }

                rv.setAdapter(new ConversationListAdapter(context, conversations));
                rv.setLayoutManager(new LinearLayoutManager(context));
            }
        };

        queryHandler.startQuery(CONVERSATION_QUERY_TOKEN, null,
                Telephony.Threads.CONTENT_URI,
                new String[] {
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE },
                null, null,
                Telephony.Sms.DEFAULT_SORT_ORDER);

    }
}
