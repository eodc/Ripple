package io.eodc.ripple.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.eodc.ripple.TextMessage;
import io.eodc.ripple.R;
import timber.log.Timber;

/**
 * Created by 2n on 1/4/18.
 */

public class MessageHistoryAdapter extends RecyclerView.Adapter<MessageHistoryAdapter.MessageHolder> {

    private List<TextMessage> mMessageList;
    private Context mContext;
    private final int VIEW_TYPE_SENT = 0;
    private final int VIEW_TYPE_RECEIVE = 1;
    private final int VIEW_TYPE_SENT_DIVIDER = 2;
    private final int VIEW_TYPE_RECEIVE_DIVIDER = 3;

    public MessageHistoryAdapter(List<TextMessage> messageList, Context context) {
        mMessageList = messageList;
        mContext = context;
    }

    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT || viewType == VIEW_TYPE_SENT_DIVIDER) {
            view = LayoutInflater.from(mContext)
                    .inflate(R.layout.item_message_sent, parent, false);
            return new MessageHolder(view);
        } else if (viewType == VIEW_TYPE_RECEIVE || viewType == VIEW_TYPE_RECEIVE_DIVIDER) {
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

            if (!(thisMsgCal.get(Calendar.YEAR) == lastMsgCal.get(Calendar.YEAR) &&
                    thisMsgCal.get(Calendar.DAY_OF_YEAR) == lastMsgCal.get(Calendar.DAY_OF_YEAR))) {
                if (message.isFromUser())
                    return VIEW_TYPE_SENT_DIVIDER;
                else
                    return VIEW_TYPE_RECEIVE_DIVIDER;
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
        final TextView content = holder.content;
        final TextView date = holder.date;

        content.setText(message.getContent());
        date.setText(DateUtils.formatDateTime(mContext, message.getDate(), DateUtils.FORMAT_SHOW_TIME));

        content.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int dateVisibility = date.getVisibility();
                switch (dateVisibility) {
                    case View.VISIBLE:
                        date.setVisibility(View.GONE);
                        break;
                    case View.GONE:
                        date.setVisibility(View.VISIBLE);
                        break;
                    case View.INVISIBLE:
                        Timber.e("For some reason the date was set as invisible... autosetting it to visible.");
                        date.setVisibility(View.VISIBLE);
                        break;
                    }
                }
            });
        if (holder.getItemViewType() == VIEW_TYPE_RECEIVE_DIVIDER || holder.getItemViewType() == VIEW_TYPE_SENT_DIVIDER) {
            holder.divider.setText(mContext.getString(R.string.date_divider,
                    DateUtils.formatDateTime(mContext,
                            message.getDate(),
                            DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY)));
            holder.divider.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
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
