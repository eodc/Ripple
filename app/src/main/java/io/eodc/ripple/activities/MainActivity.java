package io.eodc.ripple.activities;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.List;

import io.eodc.ripple.BuildConfig;
import io.eodc.ripple.R;
import io.eodc.ripple.adapters.ConversationListAdapter;
import io.eodc.ripple.telephony.Conversation;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static int CONVERSATION_QUERY_TOKEN = 0;

    private ConversationProviderQueryHandler queryHandler;

    private RecyclerView conversationList;
    private FloatingActionButton newConversationFab;

    private List<Conversation> conversations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String packageName = getPackageName();
        conversations = new ArrayList<>();

        // Plant Timber tree if debug build
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        queryHandler = new ConversationProviderQueryHandler(getContentResolver());
        startQuery();

        newConversationFab = findViewById(R.id.new_conversation_fab);
        newConversationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Telephony.Sms.getDefaultSmsPackage(getApplicationContext()).equals(packageName)) {
                    Snackbar.make(view, "I'm not the default SMS app!", Snackbar.LENGTH_LONG)
                            .setAction("Make Default", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
                                    startActivity(intent);
                                }
                            }).show();
                }
            }
        });

        conversationList = findViewById(R.id.conversation_list);

    }

    private void startQuery() {
        queryHandler.startQuery(CONVERSATION_QUERY_TOKEN, null,
                Telephony.Threads.CONTENT_URI,
                new String[] {
                        Telephony.Sms.THREAD_ID,
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE },
                null, null,
                Telephony.Sms.DEFAULT_SORT_ORDER);
    }

    @SuppressLint("HandlerLeak")
    private class ConversationProviderQueryHandler extends AsyncQueryHandler {
        private ConversationProviderQueryHandler(ContentResolver cr) {
            super(cr);
        }
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            int idIndex = cursor.getColumnIndex(Telephony.Sms.THREAD_ID);
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
                Cursor contactsLookupCursor = getContentResolver().query(uri, new String[] {
                                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                                ContactsContract.PhoneLookup.DISPLAY_NAME },
                        null, null, null);
                Conversation conversation = new Conversation(idIndex, parsedNumStr, cursor.getString(bodyIndex), cursor.getLong(dateIndex));
                if (contactsLookupCursor != null && contactsLookupCursor.moveToNext()) {
                    conversation.setContactPhotoURI(contactsLookupCursor.getString(contactsLookupCursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)));
                    conversation.setName(contactsLookupCursor.getString(contactsLookupCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)));
                }
                conversations.add(conversation);
                assert contactsLookupCursor != null;
                contactsLookupCursor.close();
            }

            conversationList.setAdapter(new ConversationListAdapter(conversations, getBaseContext()));
            conversationList.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        }
    }
}
