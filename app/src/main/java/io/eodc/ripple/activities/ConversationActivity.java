package io.eodc.ripple.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.r0adkll.slidr.Slidr;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import io.eodc.ripple.BuildConfig;
import io.eodc.ripple.R;
import io.eodc.ripple.fragments.ConversationFragment;
import timber.log.Timber;

public class ConversationActivity extends AppCompatActivity {

    SmsManager smsManager;

    SlidingUpPanelLayout slidingLayout;
    ImageView mAttachIcon;
    EditText mMessageComposer;
    FloatingActionButton mSendButton;
    RelativeLayout mComposerLayout;

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

        smsManager = SmsManager.getDefault();
        Slidr.attach(this);

        if (savedInstanceState != null) {
            String savedMsgContent = savedInstanceState.getString("savedMsgContent");
            if (savedMsgContent != null)
                mMessageComposer.setText(savedMsgContent);
        }

        // Plant Timber tree if debug build
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        // Initialize UI
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Timber.i("Toolbar initialized");

        slidingLayout = findViewById(R.id.root_layout);
        mAttachIcon = findViewById(R.id.attach_icon);
        mAttachIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                } else if (slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }
        });
        mMessageComposer = findViewById(R.id.message_composer);
        mMessageComposer.addTextChangedListener(new TextLineCountListener());

        mComposerLayout = findViewById(R.id.message_composer_container);
        mSendButton = findViewById(R.id.send_button);

        getSupportFragmentManager().beginTransaction()
                .add(new ConversationFragment(), "conversationFragment")
                .commit();
    }

    public void sendMessage(View view) {
        String msgContent = mMessageComposer.getText().toString();

        if (msgContent != null && !msgContent.equals("")) {
            ConversationFragment conversationFrag = (ConversationFragment) getSupportFragmentManager().findFragmentById(R.id.msg_history);
            conversationFrag.sendMessage(msgContent);
            mMessageComposer.setText("");
        } else {
            Toast.makeText(this, "Empty message...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putString("savedMsgContent", mMessageComposer.getText().toString());
        super.onSaveInstanceState(outState, outPersistentState);
    }

    class TextLineCountListener implements TextWatcher {
        int numLines = 1;

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (mMessageComposer.getLineCount() == 0)
                return;

            int composerHeight = mComposerLayout.getMeasuredHeight();
            RelativeLayout.LayoutParams composerLayoutParams = (RelativeLayout.LayoutParams) mComposerLayout.getLayoutParams();

            if (numLines < mMessageComposer.getLineCount() && mMessageComposer.getLineCount() <= 5) {
                numLines = mMessageComposer.getLineCount();
                int deltaHeight = mMessageComposer.getHeight() / numLines;
                slidingLayout.setPanelHeight(slidingLayout.getPanelHeight() + deltaHeight);
                composerLayoutParams.height = composerHeight + deltaHeight;

            } else if (numLines > mMessageComposer.getLineCount()) {
                numLines = mMessageComposer.getLineCount();
                slidingLayout.setPanelHeight(mMessageComposer.getHeight());
                composerLayoutParams.height = mMessageComposer.getHeight();
            }
            mComposerLayout.setLayoutParams(composerLayoutParams);
        }

        @Override
        public void afterTextChanged(Editable editable) { }
    }
}

