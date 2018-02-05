package io.eodc.ripple.fragments;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsMessage;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.eodc.ripple.R;
import io.eodc.ripple.adapters.MessageHistoryAdapter;
import io.eodc.ripple.telephony.TextMessage;

/**
 * Fragment for Conversations, to be used in ConversationActivity and NewConversationActivity
 */

public class ConversationFragment extends Fragment {

    private static final int INBOX_TOKEN = 0;
    private static final int SENT_TOKEN = 1;

    private AsyncQueryHandler queryHandler;
    private BroadcastReceiver newSmsReceiver;
    private Context mContext;

    private RecyclerView conversationMsgs;
    private ConstraintLayout mMessageComposer;
    private EditText mMessageComposerInput;
    private TextView mJumpRecents;

    private String phoneNum;
    private List<TextMessage> messages;


    public ConversationFragment() {

    }

    public static ConversationFragment newInstance(String phoneNum) {
        ConversationFragment fragment = new ConversationFragment();
        Bundle args = new Bundle();
        args.putString("phoneNum", phoneNum);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            phoneNum = getArguments().getString("phoneNum");
        }

        mContext = getContext();
        queryHandler = new MessageProviderQueryHandler(mContext != null ? mContext.getContentResolver() : null);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        newSmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    Object[] pdus = (Object[]) extras.get("pdus");

                    for (Object pdu : pdus != null ? pdus : new Object[0]) {
                        SmsMessage newMsg = SmsMessage.createFromPdu((byte[]) pdu);
                        addMessageToList(new TextMessage(newMsg.getMessageBody(), System.currentTimeMillis()));
                    }
                }
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_conversation, container, false);

        conversationMsgs = view.findViewById(R.id.conversation_msgs);
        mMessageComposer = view.findViewById(R.id.message_composer);
        mMessageComposerInput = view.findViewById(R.id.message_composer_input);
        mJumpRecents = view.findViewById(R.id.jump_recents);

        if (savedInstanceState == null) {
            startQuery();
        } else {
            messages = savedInstanceState.getParcelableArrayList("messages");
            String savedMsgContent = savedInstanceState.getString("savedMsgContent");
            if (savedMsgContent != null)
                mMessageComposerInput.setText(savedMsgContent);
        }

        LinearLayoutManager rvLayoutMan = new LinearLayoutManager(mContext);
        rvLayoutMan.setReverseLayout(true);
        rvLayoutMan.setStackFromEnd(true);
        conversationMsgs.setLayoutManager(rvLayoutMan);
        conversationMsgs.setAdapter(new MessageHistoryAdapter(messages, mContext));

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.unregisterReceiver(newSmsReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        mContext.registerReceiver(newSmsReceiver, filter);

        conversationMsgs.scrollToPosition(0);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("savedMsgContent", mMessageComposerInput.getText().toString());
        outState.putParcelableArrayList("messages", (ArrayList<TextMessage>) messages);
    }

    public void addMessageToList(TextMessage newMsg) {
        messages.add(0, newMsg);
        conversationMsgs.getAdapter().notifyDataSetChanged();
    }
    public void scrollToPresent() {
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
        mJumpRecents.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.collapse_recents_notif));
        mJumpRecents.postOnAnimation(new Runnable() {
            @Override
            public void run() {
                mJumpRecents.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void startQuery() {
        messages = new ArrayList<>();
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

    private void displayMessages() {
        conversationMsgs.getAdapter().notifyDataSetChanged();
        conversationMsgs.scrollToPosition(0);
    }


    @SuppressLint("HandlerLeak")
    private class MessageProviderQueryHandler extends AsyncQueryHandler {
        MessageProviderQueryHandler(ContentResolver cr) {
            super(cr);
        }

        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            int addrIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
            int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
            int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
            List<Long> msgDates = new ArrayList<>();

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
            displayMessages();
        }
    }
}
