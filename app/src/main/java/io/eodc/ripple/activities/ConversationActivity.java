package io.eodc.ripple.activities;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.eodc.ripple.BuildConfig;
import io.eodc.ripple.R;
import io.eodc.ripple.TextMessage;
import io.eodc.ripple.adapters.MessageHistoryAdapter;
import timber.log.Timber;

public class ConversationActivity extends AppCompatActivity {

    SmsManager smsManager;

    ImageView mAttachIcon;
    EditText mMessageComposer;
    ImageView mSendButton;
    RelativeLayout mComposerLayout;

    private static final int INBOX_TOKEN = 0;
    private static final int SENT_TOKEN = 1;
    MessageHistoryAdapter adapter;
    List<TextMessage> messages;
    RecyclerView conversationMsgs;
    private BroadcastReceiver newSmsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup Activity Basics
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName())) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivity(intent);
        }

        smsManager = SmsManager.getDefault();
        mAttachIcon = findViewById(R.id.attach_icon);
        mMessageComposer = findViewById(R.id.message_composer);

        mComposerLayout = findViewById(R.id.new_message_container);
        mSendButton = findViewById(R.id.send_button);

        conversationMsgs = findViewById(R.id.conversation_msgs);

        if (savedInstanceState == null) {
            messages = new ArrayList<>();
            final List<Long> msgDates = new ArrayList<>();
            @SuppressLint("HandlerLeak")
            AsyncQueryHandler queryHandler = new AsyncQueryHandler(this.getContentResolver()) {
                @Override
                protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                    int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                    int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                    while (cursor.moveToNext()) {
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
            };
            queryHandler.startQuery(INBOX_TOKEN,
                    null,
                    Telephony.Sms.Inbox.CONTENT_URI,
                    new String[]{Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE},
                    Telephony.Sms.ADDRESS + "= ?",
                    new String[]{"+16504838732"},
                    Telephony.Sms.DEFAULT_SORT_ORDER);
            queryHandler.startQuery(SENT_TOKEN,
                    null,
                    Telephony.Sms.Sent.CONTENT_URI,
                    new String[]{Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE},
                    Telephony.Sms.ADDRESS + "= ?",
                    new String[]{"+16504838732"},
                    Telephony.Sms.DEFAULT_SORT_ORDER);
        } else {
            String savedMsgContent = savedInstanceState.getString("savedMsgContent");
            messages = savedInstanceState.getParcelableArrayList("messageList");
            if (savedMsgContent != null)
                mMessageComposer.setText(savedMsgContent);
        }

        // Plant Timber tree if debug build
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        // Initialize UI
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Timber.i("Toolbar initialized");

        newSmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Object[] pdus = (Object[]) extras.get("pdus");

                    for (Object pdu : pdus) {
                        SmsMessage newMsg = SmsMessage.createFromPdu((byte[]) pdu);
                        addMessageToList(new TextMessage(newMsg.getMessageBody(), System.currentTimeMillis()));
                    }
                    if (!isAtLastItem()) {
                        Snackbar newMsgNotif = Snackbar.make(findViewById(R.id.root_layout), "New Message", Snackbar.LENGTH_SHORT);
                        newMsgNotif.setAction("View", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                conversationMsgs.scrollToPosition(0);
                            }
                        });
                        newMsgNotif.show();
                    }
                }
            }
        };

        LinearLayoutManager rvLayoutMan = new LinearLayoutManager(this);
        rvLayoutMan.setReverseLayout(true);
        rvLayoutMan.setStackFromEnd(true);
        conversationMsgs.setLayoutManager(rvLayoutMan);
        adapter = new MessageHistoryAdapter(messages, this);
        conversationMsgs.setAdapter(adapter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(newSmsReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_DELIVER");
        registerReceiver(newSmsReceiver, filter);
        conversationMsgs.scrollToPosition(0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("savedMsgContent", mMessageComposer.getText().toString());
        outState.putParcelableArrayList("messageList", (ArrayList<TextMessage>) messages);
        super.onSaveInstanceState(outState);
    }

    public void addMessageToList(TextMessage newMsg) {
        messages.add(0, newMsg);
        adapter.notifyDataSetChanged();
    }

    private boolean isAtLastItem() {
        if (conversationMsgs.getAdapter().getItemCount() != 0) {
            int lastVisibleItemPos = ((LinearLayoutManager) conversationMsgs.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            return lastVisibleItemPos != RecyclerView.NO_POSITION && lastVisibleItemPos == 0;
        }
        return false;
    }
}

