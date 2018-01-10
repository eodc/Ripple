package io.eodc.ripple.Activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;

import com.r0adkll.slidr.Slidr;

import io.eodc.ripple.BuildConfig;
import io.eodc.ripple.R;
import timber.log.Timber;

public class ConversationActivity extends AppCompatActivity {

    SmsManager smsManager;
    EditText mMsgComposer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup Activity Basics
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        smsManager = SmsManager.getDefault();
        Slidr.attach(this);

        // Plant Timber tree if debug build
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        // Initialize UI
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Timber.i("Toolbar initialized");

        mMsgComposer =  findViewById(R.id.message_composer);
    }

    public void sendMessage(View view) {
    }
}

