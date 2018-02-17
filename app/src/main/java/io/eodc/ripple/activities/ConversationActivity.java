package io.eodc.ripple.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import io.eodc.ripple.BuildConfig;
import io.eodc.ripple.R;
import io.eodc.ripple.fragments.ConversationFragment;
import io.eodc.ripple.telephony.Conversation;
import timber.log.Timber;

public class ConversationActivity extends AppCompatActivity {

    private ConversationFragment conversationFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Activity Basics
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Intent startingIntent = getIntent();
        Conversation conversation = startingIntent.getParcelableExtra("conversation");

        // Plant Timber tree if debug build
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        // Initialize UI
        Toolbar mToolbar = findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            ImageView contactPic = findViewById(R.id.contact_photo);
            TextView contactName = findViewById(R.id.contact_name);
            contactName.setText(conversation.getName() != null ? conversation.getName() : conversation.getHumanReadableNum());
            if (conversation.getContactPhotoURI() != null)
                contactPic.setImageURI(Uri.parse(conversation.getContactPhotoURI()));
            else {
                int type = startingIntent.getIntExtra("contactType", -1);
                contactPic.setImageDrawable(Conversation.generateContactPic(type, this));
            }
        }

        if (savedInstanceState == null) {
            String phoneNum = conversation.getPhoneNum();
            conversationFragment = ConversationFragment.newInstance(conversation.getThreadId(), phoneNum);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.conversation_container, conversationFragment)
                    .commit();
        }
    }

    public void scrollToPresent(View view) {
        conversationFragment.scrollToPresent();
    }

    public void sendMessage(View view) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage("+16505551212", null, "test", null, null);
        ContentValues values = new ContentValues();
        values.put("address", "+16505551212");
        values.put("body", "test");
        values.put("date", System.currentTimeMillis());
        getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);
    }
}

