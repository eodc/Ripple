package io.eodc.ripple.fragments;

import android.annotation.SuppressLint;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.SmsMessage;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.eodc.ripple.R;
import io.eodc.ripple.adapters.MessageHistoryAdapter;
import io.eodc.ripple.telephony.Conversation;
import io.eodc.ripple.telephony.MediaMessage;
import io.eodc.ripple.telephony.Message;
import io.eodc.ripple.telephony.TextMessage;
import timber.log.Timber;

/**
 * Fragment for Conversations, to be used in ConversationActivity and NewConversationActivity
 */

public class ConversationFragment extends Fragment {

    private static final int QUERY_TOKEN = 0;
    private static final int ADD_MMS_TOKEN = 1;
    private static final int SENT_FLAG = 2;

    private AsyncQueryHandler queryHandler;
    private BroadcastReceiver newSmsReceiver;
    private Context mContext;

    private RecyclerView conversationMsgs;
    private ConstraintLayout mMessageComposer;
    private EditText mMessageComposerInput;
    private TextView mJumpRecents;

    private long threadId;
    private String phoneNum;
    private List<Message> messages;


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
            populateMessages();
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
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("savedMsgContent", mMessageComposerInput.getText().toString());
        outState.putParcelableArrayList("messages", (ArrayList<Message>) messages);
    }

    public void addMessageToList(Message message, boolean newMsg) {
        if (newMsg) messages.add(0, message);
        else messages.add(message);
        Collections.sort(messages, new Message.SortByDate());
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

    private void displayMessages() {
        conversationMsgs.getAdapter().notifyDataSetChanged();
        conversationMsgs.scrollToPosition(0);
    }

    private void populateMessages() {
        messages = new ArrayList<>();
        Uri queryUri = Uri.withAppendedPath(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, Uri.encode(String.valueOf(threadId)));
        queryHandler.startQuery(QUERY_TOKEN,
                null,
                queryUri,
                new String[] { Telephony.Sms._ID, Telephony.Mms.CONTENT_TYPE, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Mms.MESSAGE_BOX },
                null, null,
                Telephony.Sms.DEFAULT_SORT_ORDER );
    }

    @SuppressLint("HandlerLeak")
    private class MessageProviderQueryHandler extends AsyncQueryHandler {
        MessageProviderQueryHandler(ContentResolver cr) {
            super(cr);
        }

        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            switch (token) {
                case QUERY_TOKEN:
                    new PopulateTextMessagesTask().execute(cursor);
                    break;
                case ADD_MMS_TOKEN:
                    new PopulateMediaMessagesTask().execute(cursor, cookie);
                    break;
            }
        }


    }

    @SuppressLint("StaticFieldLeak")
    class PopulateTextMessagesTask extends AsyncTask<Cursor, Void, Void> {

        @Override
        protected Void doInBackground(Cursor... cursors) {
            Cursor cursor = cursors[0];
            List<MediaMessage> mediaMessages = new ArrayList<>();
            int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
            int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
            int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);
            int mBoxIndex = cursor.getColumnIndex(Telephony.Mms.MESSAGE_BOX);
            int ctIndex = cursor.getColumnIndex(Telephony.Mms.CONTENT_TYPE);
            int idIndex = cursor.getColumnIndex(Telephony.Sms._ID);

            StringBuilder sb = new StringBuilder();
            while (cursor.moveToNext()) {
                String mmsDiscim = cursor.getString(ctIndex);
                if (mmsDiscim == null) {
                    TextMessage message = new TextMessage(cursor.getString(bodyIndex), cursor.getLong(dateIndex));
                    if (cursor.getInt(typeIndex) == SENT_FLAG) {
                        message.setFromUser();
                    }
                    addMessageToList(message, false);
                } else {
                    long msgId = cursor.getLong(idIndex);
                    if (sb.toString().equals("")) {
                        sb.append(Telephony.Mms.Part.MSG_ID).append("=").append(msgId);
                    } else {
                        sb.append(" OR ").append(Telephony.Mms.Part.MSG_ID).append("=").append(msgId);
                    }
                    MediaMessage message = new MediaMessage(msgId);
                    message.setDate(TimeUnit.SECONDS.toMillis(cursor.getLong(dateIndex)));
                    if (cursor.getInt(mBoxIndex) == Telephony.Mms.MESSAGE_BOX_SENT)
                        message.setFromUser();
                    mediaMessages.add(message);
                }
            }
            queryHandler.startQuery(ADD_MMS_TOKEN, mediaMessages, Uri.parse("content://mms/part"), null,
                    sb.toString(), null, null);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            displayMessages();
        }
    }

    @SuppressLint("StaticFieldLeak")
    @SuppressWarnings("unchecked")
    class PopulateMediaMessagesTask extends AsyncTask<Object, Void, Void> {

        @Override
        protected Void doInBackground(Object... objects) {
            Cursor cursor = (Cursor) objects[0];
            List<MediaMessage> mediaMessages = (List<MediaMessage>) objects[1];

            if (cursor.moveToFirst()) {
                do {
                    String partId = cursor.getString(cursor.getColumnIndex("_id"));
                    String type = cursor.getString(cursor.getColumnIndex("ct"));
                    if ("image/jpeg".equals(type) || "image/bmp".equals(type) ||
                            "image/gif".equals(type) || "image/jpg".equals(type) ||
                            "image/png".equals(type)) {
                        for (MediaMessage m : mediaMessages) {
                            if (cursor.getLong(cursor.getColumnIndex(Telephony.Mms.Part.MSG_ID)) == m.getId()) {
                                m.setMedia(getMediaFromPartId(partId));
                                addMessageToList(m, false);
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            displayMessages();
        }
    }

    private Bitmap getMediaFromPartId(String partId) {
        Uri partURI = Uri.parse("content://mms/part/" + partId);
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = mContext.getContentResolver().openInputStream(partURI);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Timber.e(e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
        }

        return scaleBitmap(bitmap);
    }

    @Nullable
    private Bitmap scaleBitmap(Bitmap bitmap) {
        final float MAX_LANDSCAPE_WIDTH = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, mContext.getResources().getDisplayMetrics());
        final float MAX_LANDSCAPE_HEIGHT = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 200, mContext.getResources().getDisplayMetrics());

        if (bitmap.getWidth() > bitmap.getHeight()) {
            if (bitmap.getWidth() > MAX_LANDSCAPE_WIDTH && bitmap.getHeight() > MAX_LANDSCAPE_HEIGHT)
                return Bitmap.createScaledBitmap(bitmap, (int) MAX_LANDSCAPE_WIDTH,
                        (int) MAX_LANDSCAPE_HEIGHT, false);
        } else {
            if (bitmap.getWidth() > MAX_LANDSCAPE_HEIGHT && bitmap.getHeight() > MAX_LANDSCAPE_WIDTH)
                return Bitmap.createScaledBitmap(bitmap, (int) MAX_LANDSCAPE_HEIGHT,
                        (int) MAX_LANDSCAPE_WIDTH, false);
        }
        return bitmap;
    }
}
