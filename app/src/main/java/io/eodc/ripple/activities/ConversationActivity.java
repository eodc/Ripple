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
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsMessage;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.eodc.ripple.BuildConfig;
import io.eodc.ripple.R;
import io.eodc.ripple.TextMessage;
import io.eodc.ripple.adapters.MessageHistoryAdapter;
import timber.log.Timber;

public class ConversationActivity extends AppCompatActivity {


    private static final int INBOX_TOKEN = 0;
    private static final int SENT_TOKEN = 1;

    private BroadcastReceiver newSmsReceiver;

    ConstraintLayout mMessageComposer;
    RelativeLayout mComposerLayout;
    EditText mMessageComposerInput;
    ImageView mSendButton;
    TextView mJumpRecents;
    ImageView mAttachIcon;
    RecyclerView conversationMsgs;

    MessageHistoryAdapter adapter;

    List<TextMessage> messages;

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

        mAttachIcon = findViewById(R.id.attach_icon);
        mMessageComposer = findViewById(R.id.msg_composer);
        mMessageComposerInput = findViewById(R.id.message_composer_input);
        mJumpRecents = findViewById(R.id.jump_recents);

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
                    new String[]{""},
                    Telephony.Sms.DEFAULT_SORT_ORDER);
            queryHandler.startQuery(SENT_TOKEN,
                    null,
                    Telephony.Sms.Sent.CONTENT_URI,
                    new String[]{Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE},
                    Telephony.Sms.ADDRESS + "= ?",
                    new String[]{""},
                    Telephony.Sms.DEFAULT_SORT_ORDER);
        } else {
            String savedMsgContent = savedInstanceState.getString("savedMsgContent");
            messages = savedInstanceState.getParcelableArrayList("messageList");
            if (savedMsgContent != null)
                mMessageComposerInput.setText(savedMsgContent);
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
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(newSmsReceiver, filter);
        conversationMsgs.scrollToPosition(0);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("savedMsgContent", mMessageComposerInput.getText().toString());
        outState.putParcelableArrayList("messageList", (ArrayList<TextMessage>) messages);
        super.onSaveInstanceState(outState);
    }

    public void addMessageToList(TextMessage newMsg) {
        messages.add(0, newMsg);
        adapter.notifyDataSetChanged();
    }

    public void scrollToPresent(View view) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) conversationMsgs.getLayoutManager();
        int currentPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (currentPosition < 75)
            conversationMsgs.smoothScrollToPosition(0);
        else
            conversationMsgs.scrollToPosition(0);
            showComposer();
    }

    private void showComposer() {
        mMessageComposer.animate()
                .setDuration(100)
                .translationY(0);
        mJumpRecents.startAnimation(AnimationUtils.loadAnimation(this, R.anim.collapse_recents_notif));
        mJumpRecents.postOnAnimation(new Runnable() {
            @Override
            public void run() {
                mJumpRecents.setVisibility(View.INVISIBLE);
            }
        });
    }
}

