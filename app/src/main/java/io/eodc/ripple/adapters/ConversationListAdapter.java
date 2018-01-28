package io.eodc.ripple.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import io.eodc.ripple.R;
import io.eodc.ripple.activities.ConversationActivity;
import io.eodc.ripple.telephony.Conversation;

/**
 * Adapter for showing the conversation list
 */

public class ConversationListAdapter extends RecyclerView.Adapter<ConversationListAdapter.ContactEntryViewHolder> {

    private static final int NO_CONTACT = 0;
    private static final int CONTACT_NO_PIC = 1;
    private static final int CONTACT = 2;

    private Context mContext;
    private List<Conversation> conversations;

    public ConversationListAdapter(Context context, List<Conversation> conversations) {
        mContext = context;
        this.conversations = conversations;
    }

    @Override
    public int getItemViewType(int position) {
        Conversation conversation = conversations.get(position);
        if (conversation.getName() == null)
            return NO_CONTACT;
        else if (conversation.getContactPhotoURI() == null)
            return CONTACT_NO_PIC;
        else
            return CONTACT;
    }

    @Override
    public ContactEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_conversation_entry, parent, false);
        return new ContactEntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ContactEntryViewHolder holder, int position) {
        Conversation conver = conversations.get(position);
        int holderType = holder.getItemViewType();

        holder.phoneNum = conver.getPhoneNum();
        holder.lastMsg.setText(conver.getLastMsgBody());
        holder.timestamp.setText(DateUtils.formatDateTime(mContext, conver.getTimestamp(), DateUtils.FORMAT_SHOW_TIME));

        switch (holderType) {
            case NO_CONTACT:
                holder.name.setText(conver.getPhoneNum());
                break;
            case CONTACT_NO_PIC:
                holder.name.setText(conver.getName());
                break;
            default:
                holder.name.setText(conver.getName());
                holder.contactPic.setImageURI(Uri.parse(conver.getContactPhotoURI()));
        }

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, ConversationActivity.class);
                intent.putExtra("phoneNum", holder.phoneNum);
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    class ContactEntryViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout root;
        TextView timestamp, name, lastMsg;
        ImageView contactPic;
        String phoneNum;
        ContactEntryViewHolder(View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.root_layout);
            timestamp = itemView.findViewById(R.id.lastMsgTimestamp);
            name = itemView.findViewById(R.id.contact_name);
            lastMsg = itemView.findViewById(R.id.lastMsg);
            contactPic = itemView.findViewById(R.id.contact_photo);
        }
    }
}
