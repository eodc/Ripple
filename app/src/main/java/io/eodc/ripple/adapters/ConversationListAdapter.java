package io.eodc.ripple.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Adapter for showing the conversation list
 */

public class ConversationListAdapter extends RecyclerView.Adapter<ConversationListAdapter.ContactEntryViewHolder> {

    @Override
    public ContactEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ContactEntryViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class ContactEntryViewHolder extends RecyclerView.ViewHolder {
        public ContactEntryViewHolder(View itemView) {
            super(itemView);
        }
    }
}
