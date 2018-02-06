package io.eodc.ripple.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
    private Conversation conversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup Activity Basics
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName())) {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivity(intent);
        }

        Intent startingIntent = getIntent();
        conversation = startingIntent.getParcelableExtra("conversation");

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
}

