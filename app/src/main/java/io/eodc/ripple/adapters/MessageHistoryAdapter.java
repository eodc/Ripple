package io.eodc.ripple.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.eodc.ripple.R;
import io.eodc.ripple.telephony.MediaMessage;
import io.eodc.ripple.telephony.Message;
import io.eodc.ripple.telephony.TextMessage;

/**
 * Adapter for showing the history of texts
 */

public class MessageHistoryAdapter extends RecyclerView.Adapter {

    private List<Message> mMessageList;
    private Context mContext;

    private final int VIEW_TYPE_SENT = 2;
    private final int VIEW_TYPE_SENT_DIVIDER = 4;
    private final int VIEW_TYPE_SENT_DATE_VIS = 6;

    private final int VIEW_TYPE_RECEIVE = 1;
    private final int VIEW_TYPE_RECEIVE_DIVIDER = 3;
    private final int VIEW_TYPE_RECEIVE_DATE_VIS = 5;

    private final int VIEW_TYPE_SENT_MEDIA = 200;
    private final int VIEW_TYPE_SENT_MEDIA_DIVIDER = 400;
    private final int VIEW_TYPE_SENT_MEDIA_DATE_VIS = 600;

    private final int VIEW_TYPE_RECEIVE_MEDIA = 100;
    private final int VIEW_TYPE_RECEIVE_MEDIA_DIVIDER = 300;
    private final int VIEW_TYPE_RECEIVE_MEDIA_DATE_VIS = 500;

    private final int MEDIA_MODIFIER = 100;



    public MessageHistoryAdapter(List<Message> messageList, Context context) {
        mMessageList = messageList;
        mContext = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType % VIEW_TYPE_SENT_MEDIA == 0) {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_media_message_sent, parent, false);
            return new MediaMessageHolder(view);
        } else if (viewType % VIEW_TYPE_RECEIVE_MEDIA == 0) {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_media_message_received, parent, false);
            return new MediaMessageHolder(view);
        } else if (viewType % VIEW_TYPE_SENT == 0) {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_message_sent, parent, false);
            return new TextMessageHolder(view);
        } else if (viewType % VIEW_TYPE_RECEIVE == 0) {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_message_received, parent, false);
            return new TextMessageHolder(view);
        } else
            return null;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = mMessageList.get(position);
        if (position < mMessageList.size() - 1) {
            Message lastMsg =  mMessageList.get(position + 1);

            Calendar thisMsgCal = Calendar.getInstance();
            Calendar lastMsgCal = Calendar.getInstance();
            thisMsgCal.setTime(new Date(message.getDate()));
            lastMsgCal.setTime(new Date(lastMsg.getDate()));
            long minSinceLastMsg = TimeUnit.MILLISECONDS.toMinutes(message.getDate() - lastMsg.getDate());

            if (!(thisMsgCal.get(Calendar.YEAR) == lastMsgCal.get(Calendar.YEAR) &&
                    thisMsgCal.get(Calendar.DAY_OF_YEAR) == lastMsgCal.get(Calendar.DAY_OF_YEAR))) {
                // Not from same day
                if (message.isFromUser())
                    if (isSms(message))
                        return VIEW_TYPE_SENT_DIVIDER;
                    else
                        return VIEW_TYPE_SENT_MEDIA_DIVIDER;
                else
                    if (isSms(message))
                        return VIEW_TYPE_RECEIVE_DIVIDER;
                    else
                        return VIEW_TYPE_RECEIVE_MEDIA_DIVIDER;
            } else if (minSinceLastMsg > 30) {
                // 30 mins since last message
                if (message.isFromUser())
                    if (isSms(message))
                        return VIEW_TYPE_SENT_DATE_VIS;
                    else
                        return VIEW_TYPE_SENT_MEDIA_DATE_VIS;
                else
                    if (isSms(message))
                        return VIEW_TYPE_RECEIVE_DATE_VIS;
                    else
                        return VIEW_TYPE_RECEIVE_MEDIA_DATE_VIS;
            }
        } else if (position == mMessageList.size() - 1) {
            if (message.isFromUser())
                if (isSms(message))
                    return VIEW_TYPE_SENT_DIVIDER;
                else
                    return VIEW_TYPE_SENT_MEDIA_DIVIDER;
            else
                if (isSms(message))
                    return VIEW_TYPE_RECEIVE_DIVIDER;
                else
                    return VIEW_TYPE_RECEIVE_MEDIA_DIVIDER;
        }
        if (message.isFromUser())
            if (isSms(message))
                return VIEW_TYPE_SENT;
            else
                return VIEW_TYPE_SENT_MEDIA;
        else
            if (isSms(message))
                return VIEW_TYPE_RECEIVE;
            else
                return VIEW_TYPE_RECEIVE_MEDIA;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int holderType = holder.getItemViewType();
        if (holderType % MEDIA_MODIFIER != 0) {
            TextMessage message = (TextMessage) mMessageList.get(position);
            TextMessageHolder textMessageHolder = (TextMessageHolder) holder;

            final TextView content = textMessageHolder.content;
            final TextView date = textMessageHolder.date;

            content.setText(message.getContent());
            date.setText(DateUtils.formatDateTime(mContext, message.getDate(), DateUtils.FORMAT_SHOW_TIME));

            content.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int dateVisibility = date.getVisibility();
                    date.setVisibility(dateVisibility == View.GONE ? View.VISIBLE : View.GONE);
                }
            });

            if (holderType == VIEW_TYPE_RECEIVE_DATE_VIS || holderType == VIEW_TYPE_SENT_DATE_VIS)
                date.setVisibility(View.VISIBLE);

            else if (holderType == VIEW_TYPE_RECEIVE_DIVIDER || holderType == VIEW_TYPE_SENT_DIVIDER) {
                textMessageHolder.divider.setText(mContext.getString(R.string.date_divider,
                        DateUtils.formatDateTime(mContext,
                                message.getDate(),
                                DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY)));
                textMessageHolder.divider.setVisibility(View.VISIBLE);
                date.setVisibility(View.VISIBLE);
            }
        } else {
            MediaMessage message = (MediaMessage) mMessageList.get(position);
            MediaMessageHolder mediaMessageHolder = (MediaMessageHolder) holder;

            final ImageView media = mediaMessageHolder.media;
            final TextView date = mediaMessageHolder.date;

            media.setImageBitmap(message.getMedia());
            date.setText(DateUtils.formatDateTime(mContext, message.getDate(), DateUtils.FORMAT_SHOW_TIME));

            if (holderType == VIEW_TYPE_RECEIVE_MEDIA_DATE_VIS || holderType == VIEW_TYPE_SENT_MEDIA_DATE_VIS)
                date.setVisibility(View.VISIBLE);

            else if (holderType == VIEW_TYPE_RECEIVE_MEDIA_DIVIDER || holderType == VIEW_TYPE_SENT_MEDIA_DIVIDER) {
                mediaMessageHolder.divider.setText(mContext.getString(R.string.date_divider,
                        DateUtils.formatDateTime(mContext,
                                message.getDate(),
                                DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY)));
                mediaMessageHolder.divider.setVisibility(View.VISIBLE);
                date.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    private boolean isSms(Message message) {
        return message.getDiscrim() == Message.SMS;
    }

    class TextMessageHolder extends RecyclerView.ViewHolder {
        TextView content, date, divider;
        TextMessageHolder(View itemView) {
            super(itemView);

            content = itemView.findViewById(R.id.message_text);
            date = itemView.findViewById(R.id.timestamp);
            divider = itemView.findViewById(R.id.date_divider);
        }
    }
    class MediaMessageHolder extends RecyclerView.ViewHolder {
        TextView date, divider;
        ImageView media;
        MediaMessageHolder(View itemView) {
            super(itemView);

            date = itemView.findViewById(R.id.timestamp);
            divider = itemView.findViewById(R.id.date_divider);
            media = itemView.findViewById(R.id.media);
        }
    }
}
