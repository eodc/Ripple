package io.eodc.ripple.Fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.eodc.ripple.Adapters.MessageHistoryAdapter;
import io.eodc.ripple.R;
import io.eodc.ripple.TextMessage;

/**
 * Created by 2n on 1/6/18.
 */

public class ConversationFragment extends Fragment {

    private static final int INBOX_TOKEN = 0;
    private static final int SENT_TOKEN = 1;
    List<TextMessage> messages;
    List<Long> msgDates;

    @SuppressLint("HandlerLeak")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View conversation = inflater.inflate(R.layout.fragment_conversation, container, false);
        RecyclerView conversationMsgs = conversation.findViewById(R.id.conversation_msgs);
        if (savedInstanceState == null) {
            messages = new ArrayList<>();
            msgDates = new ArrayList<>();
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
                            newMsg.setFromUser(true);
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
            messages = savedInstanceState.getParcelableArrayList("messageList");
            long[] msgDatesArr = savedInstanceState.getLongArray("messageDates");
            assert msgDatesArr != null;
            msgDates = new ArrayList<>();
            for (long l : msgDatesArr)
                msgDates.add(l);
        }
        LinearLayoutManager rvLayoutMan = new LinearLayoutManager(getActivity());
        rvLayoutMan.setReverseLayout(true);
        rvLayoutMan.setStackFromEnd(true);
        conversationMsgs.setLayoutManager(rvLayoutMan);
        conversationMsgs.setAdapter(new MessageHistoryAdapter(messages, getActivity()));
        conversationMsgs.scrollToPosition(0);

        return conversation;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        long[] msgDatesArr = new long[msgDates.size()];
        outState.putParcelableArrayList("messageList", (ArrayList<TextMessage>) messages);
        for (int i = 0; i < msgDates.size(); i++) {
            msgDatesArr[i] = msgDates.get(i);
        }
        outState.putLongArray("messageDates", msgDatesArr);
        super.onSaveInstanceState(outState);
    }
}
