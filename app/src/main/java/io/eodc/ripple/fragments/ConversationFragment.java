package io.eodc.ripple.fragments;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.List;

import io.eodc.ripple.R;
import io.eodc.ripple.adapters.MessageHistoryAdapter;
import io.eodc.ripple.telephony.Conversation;
import io.eodc.ripple.telephony.TextMessage;

/**
 * Fragment for Conversations, to be used in ConversationActivity and NewConversationActivity
 */

public class ConversationFragment extends Fragment {

    private static final int QUERY_TOKEN = 0;

    private AsyncQueryHandler queryHandler;
    private BroadcastReceiver newSmsReceiver;
    private Context mContext;

    private RecyclerView conversationMsgs;
    private ConstraintLayout mMessageComposer;
    private EditText mMessageComposerInput;
    private TextView mJumpRecents;

    private long threadId;
    private String phoneNum;
    private List<TextMessage> messages;


    public ConversationFragment() {

    }

    public static ConversationFragment newInstance(long threadId, String phoneNum) {
        ConversationFragment fragment = new ConversationFragment();
        Bundle args = new Bundle();
        args.putLong("thread_id", threadId);
        args.putString("phone_num", phoneNum);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            phoneNum = getArguments().getString("phone_num");
            threadId = getArguments().getLong("thread_id");
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
                        if (Conversation.parseNumber(newMsg.getOriginatingAddress()).equals(phoneNum))
                            addMessageToList(new TextMessage(newMsg.getMessageBody(), System.currentTimeMillis()), true);
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

    public void addMessageToList(TextMessage message, boolean newMsg) {
        if (newMsg) messages.add(0, message);
        else messages.add(message);
        displayMessages();
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
        Uri queryUri = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, Uri.encode(String.valueOf(threadId)));
        queryHandler.startQuery(QUERY_TOKEN,
                null,
                queryUri,
                new String[] { Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE },
                null, null,
                Telephony.Sms.DEFAULT_SORT_ORDER );
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
            while (cursor.moveToNext()) {
                int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                    Cursor sentProviderCursor = mContext.getContentResolver()
                            .query(Telephony.Sms.Sent.CONTENT_URI, null,
                                    Telephony.Sms.Sent.DATE + "=?", new String[]{cursor.getString(dateIndex)},
                                    Telephony.Sms.DEFAULT_SORT_ORDER);

                // If message is from user
                assert sentProviderCursor != null;
                if (sentProviderCursor.moveToNext()) {
                    bodyIndex = sentProviderCursor.getColumnIndex(Telephony.Sms.BODY);
                    dateIndex = sentProviderCursor.getColumnIndex(Telephony.Sms.DATE);
                    addMessageToList(new TextMessage(sentProviderCursor.getString(bodyIndex), true, Long.valueOf(sentProviderCursor.getString(dateIndex))), false);
                } else {
                    addMessageToList(new TextMessage(cursor.getString(bodyIndex), Long.valueOf(cursor.getString(dateIndex))), false);
                }
                sentProviderCursor.close();
            }
        }
    }
}
