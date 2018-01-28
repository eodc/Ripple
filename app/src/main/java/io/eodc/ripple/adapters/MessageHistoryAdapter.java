package io.eodc.ripple.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.eodc.ripple.R;
import io.eodc.ripple.telephony.TextMessage;

/**
 * Adapter for showing the history of texts
 */

public class MessageHistoryAdapter extends RecyclerView.Adapter<MessageHistoryAdapter.MessageHolder> {

    private List<TextMessage> mMessageList;
    private Context mContext;
    private int lastItemPos = -1;

    private final int VIEW_TYPE_SENT = 0;
    private final int VIEW_TYPE_SENT_DIVIDER = 2;
    private final int VIEW_TYPE_SENT_DATE_VIS = 4;

    private final int VIEW_TYPE_RECEIVE = 1;
    private final int VIEW_TYPE_RECEIVE_DIVIDER = 3;
    private final int VIEW_TYPE_RECEIVE_DATE_VIS = 5;

    public MessageHistoryAdapter(List<TextMessage> messageList, Context context) {
        mMessageList = messageList;
        mContext = context;
    }

    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType % 2 == 0) {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_message_sent, parent, false);
            return new MessageHolder(view);
        } else if (viewType % 2 == 1) {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_message_received, parent, false);
            return new MessageHolder(view);
        } else
            return null;
    }

    @Override
    public int getItemViewType(int position) {
        TextMessage message = mMessageList.get(position);
        if (position < mMessageList.size() - 1) {
            TextMessage lastMsg = mMessageList.get(position + 1);

            Calendar thisMsgCal = Calendar.getInstance();
            Calendar lastMsgCal = Calendar.getInstance();
            thisMsgCal.setTime(new Date(message.getDate()));
            lastMsgCal.setTime(new Date(lastMsg.getDate()));
            long minSinceLastMsg = TimeUnit.MILLISECONDS.toMinutes(message.getDate() - lastMsg.getDate());

            if (!(thisMsgCal.get(Calendar.YEAR) == lastMsgCal.get(Calendar.YEAR) &&
                    thisMsgCal.get(Calendar.DAY_OF_YEAR) == lastMsgCal.get(Calendar.DAY_OF_YEAR))) {
                // Not from same day
                if (message.isFromUser())
                    return VIEW_TYPE_SENT_DIVIDER;
                else
                    return VIEW_TYPE_RECEIVE_DIVIDER;
            } else if (minSinceLastMsg > 30) {
                // 30 mins since last message
                if (message.isFromUser())
                    return VIEW_TYPE_SENT_DATE_VIS;
                else
                    return VIEW_TYPE_RECEIVE_DATE_VIS;
            }
        } else if (position == mMessageList.size() - 1) {
            if (message.isFromUser())
                return VIEW_TYPE_SENT_DIVIDER;
            else
                return VIEW_TYPE_RECEIVE_DIVIDER;
        }
        if (message.isFromUser())
            return VIEW_TYPE_SENT;
        else
            return VIEW_TYPE_RECEIVE;
    }

    @Override
    public void onBindViewHolder(MessageHolder holder, int position) {
        TextMessage message = mMessageList.get(position);
        int holderType = holder.getItemViewType();

        final TextView content = holder.content;
        final TextView date = holder.date;

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
            holder.divider.setText(mContext.getString(R.string.date_divider,
                    DateUtils.formatDateTime(mContext,
                            message.getDate(),
                            DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY)));
            holder.divider.setVisibility(View.VISIBLE);
            date.setVisibility(View.VISIBLE);
        }

        setAnimation(holder, message);
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }


    @Override
    public void onViewDetachedFromWindow(MessageHolder holder) {
        holder.itemView.clearAnimation();
    }

    private void setAnimation(MessageHolder holder, TextMessage message) {
        Collections.reverse(mMessageList);
        int position = mMessageList.indexOf(message);
        if (position > lastItemPos) {
            Animation animation;
            int viewType = holder.getItemViewType();
            if (viewType % 2 == 0)
                animation = AnimationUtils.loadAnimation(mContext, R.anim.outgoing_message);
            else if (viewType % 2 == 1)
                animation = AnimationUtils.loadAnimation(mContext, R.anim.incoming_message);
            else
                return;
            holder.itemView.startAnimation(animation);
            lastItemPos = position;
        }
        Collections.reverse(mMessageList);
    }

    class MessageHolder extends RecyclerView.ViewHolder {
        TextView content, date, divider;
        MessageHolder(View itemView) {
            super(itemView);

            content = itemView.findViewById(R.id.message_text);
            date = itemView.findViewById(R.id.timestamp);
            divider = itemView.findViewById(R.id.date_divider);
        }
    }
}
