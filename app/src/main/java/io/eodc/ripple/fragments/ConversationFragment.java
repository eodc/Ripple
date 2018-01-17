package io.eodc.ripple.fragments;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsMessage;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.eodc.ripple.R;
import io.eodc.ripple.TextMessage;
import io.eodc.ripple.adapters.MessageHistoryAdapter;

/**
 * Fragment containing the RecyclerView that contains the message history
 */

public class ConversationFragment extends Fragment {

    private static final int INBOX_TOKEN = 0;
    private static final int SENT_TOKEN = 1;
    MessageHistoryAdapter adapter;
    List<TextMessage> messages;
    RecyclerView conversationMsgs;
    private BroadcastReceiver newSmsReceiver;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
                       Snackbar newMsgNotif = Snackbar.make(getActivity().findViewById(R.id.root_layout), "New Message", Snackbar.LENGTH_SHORT);
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
    }

    @SuppressLint("HandlerLeak")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View conversation = inflater.inflate(R.layout.fragment_conversation, container, false);
        conversationMsgs = conversation.findViewById(R.id.conversation_msgs);
        if (savedInstanceState == null) {
            messages = new ArrayList<>();
            final List<Long> msgDates = new ArrayList<>();
            AsyncQueryHandler queryHandler = new AsyncQueryHandler(getActivity().getContentResolver()) {
                @Override
                protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                    int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
                    int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                    while (cursor.moveToNext()) {
                        TextMessage newMsg = new TextMessage();

                        newMsg.setContent(cursor.getString(bodyIndex));
                        newMsg.setDate(cursor.getLong(dateIndex));
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
                    new String[]{"6505551212"},
                    Telephony.Sms.DEFAULT_SORT_ORDER);
            queryHandler.startQuery(SENT_TOKEN,
                    null,
                    Telephony.Sms.Sent.CONTENT_URI,
                    new String[]{Telephony.Sms.ADDRESS,
                            Telephony.Sms.BODY,
                            Telephony.Sms.DATE},
                    Telephony.Sms.ADDRESS + "= ?",
                    new String[]{"6505551212"},
                    Telephony.Sms.DEFAULT_SORT_ORDER);
        } else {
            messages = savedInstanceState.getParcelableArrayList("messageList");
        }

        LinearLayoutManager rvLayoutMan = new LinearLayoutManager(getActivity());
        rvLayoutMan.setReverseLayout(true);
        rvLayoutMan.setStackFromEnd(true);
        conversationMsgs.setLayoutManager(rvLayoutMan);
        adapter = new MessageHistoryAdapter(messages, getActivity());
        conversationMsgs.setAdapter(adapter);
        conversationMsgs.scrollToPosition(0);

        return conversation;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        getActivity().registerReceiver(newSmsReceiver, filter);
        conversationMsgs.scrollToPosition(0);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(newSmsReceiver);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList("messageList", (ArrayList<TextMessage>) messages);
        super.onSaveInstanceState(outState);
    }

    public void addMessageToList(TextMessage newMsg) {
        messages.add(0, newMsg);
        adapter.notifyDataSetChanged();
    }

    public void sendMessage(String content) {
        ContentValues values = new ContentValues();
        long messageSent = System.currentTimeMillis();
        @SuppressLint("HandlerLeak") AsyncQueryHandler queryHandler = new AsyncQueryHandler(getContext().getContentResolver()) {
            @Override
            protected Handler createHandler(Looper looper) {
                return super.createHandler(looper);
            }
        };

        values.put(Telephony.Sms.ADDRESS, "6505551212");
        values.put(Telephony.Sms.BODY, content);
        values.put(Telephony.Sms.DATE_SENT, messageSent);
        queryHandler.startInsert(0, null, Telephony.Sms.Sent.CONTENT_URI, values);

        TextMessage newMsg = new TextMessage(content, true, messageSent);
        addMessageToList(newMsg);
    }

    public boolean isAtLastItem() {
        if (conversationMsgs.getAdapter().getItemCount() != 0) {
            int lastVisibleItemPos = ((LinearLayoutManager) conversationMsgs.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            return lastVisibleItemPos != RecyclerView.NO_POSITION && lastVisibleItemPos == 0;
        }
        return false;
    }
}
