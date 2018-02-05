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

import static io.eodc.ripple.telephony.Conversation.CONTACT;
import static io.eodc.ripple.telephony.Conversation.CONTACT_NO_PIC;
import static io.eodc.ripple.telephony.Conversation.NO_CONTACT;

/**
 * Adapter for showing the conversation list
 */

public class ConversationListAdapter extends RecyclerView.Adapter<ConversationListAdapter.ContactEntryViewHolder> {
    private Context mContext;
    private List<Conversation> conversations;

    public ConversationListAdapter(List<Conversation> conversations, Context context) {
        this.conversations = conversations;
        mContext = context;
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
        final Conversation conver = conversations.get(position);
        final int contactType = holder.getItemViewType();

        holder.phoneNum = conver.getPhoneNum();
        holder.lastMsg.setText(conver.getLastMsgBody());
        holder.timestamp.setText(DateUtils.formatDateTime(mContext, conver.getTimestamp(), DateUtils.FORMAT_SHOW_TIME));

        switch (contactType) {
            case NO_CONTACT:
                holder.name.setText(conver.getHumanReadableNum());
                holder.contactPic.setImageDrawable(Conversation.generateContactPic(contactType, mContext));
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
                intent.putExtra("conversation", conver);
                intent.putExtra("contactType", contactType);
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
