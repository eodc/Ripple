package io.eodc.ripple.Activities;

import android.os.Bundle;
import android.os.PersistableBundle;
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

import com.r0adkll.slidr.Slidr;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import io.eodc.ripple.BuildConfig;
import io.eodc.ripple.R;
import timber.log.Timber;

public class ConversationActivity extends AppCompatActivity {

    SmsManager smsManager;

    SlidingUpPanelLayout slidingLayout;
    ImageView mAttachIcon;
    EditText mMessageComposer;
    FloatingActionButton mSendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup Activity Basics
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
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

        mSendButton = findViewById(R.id.send_button);
    }

    public void sendMessage(View view) {
    }

    class TextLineCountListener implements TextWatcher {
        int numLines = 1;

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (mMessageComposer.getLineCount() == 0)
                return;

            if (numLines < mMessageComposer.getLineCount() && mMessageComposer.getLineCount() <= 5) {
                numLines = mMessageComposer.getLineCount();
                int deltaHeight = mMessageComposer.getHeight() / numLines;
                slidingLayout.setPanelHeight((slidingLayout.getPanelHeight() + deltaHeight));

                RelativeLayout.LayoutParams attachIconParams = (RelativeLayout.LayoutParams) mAttachIcon.getLayoutParams();
                attachIconParams.topMargin = attachIconParams.topMargin + deltaHeight / 2;
                mAttachIcon.setLayoutParams(attachIconParams);

                RelativeLayout.LayoutParams sendButtonParams = (RelativeLayout.LayoutParams) mSendButton.getLayoutParams();
                sendButtonParams.topMargin = sendButtonParams.topMargin + deltaHeight / 2;
                mSendButton.setLayoutParams(sendButtonParams);

            } else if (numLines > mMessageComposer.getLineCount()) {
                numLines = mMessageComposer.getLineCount();
                int deltaHeight = slidingLayout.getPanelHeight() - mMessageComposer.getHeight();
                slidingLayout.setPanelHeight((mMessageComposer.getHeight()));

                RelativeLayout.LayoutParams attachIconParams = (RelativeLayout.LayoutParams) mAttachIcon.getLayoutParams();
                attachIconParams.topMargin = attachIconParams.topMargin - deltaHeight / 2;
                mAttachIcon.setLayoutParams(attachIconParams);

                RelativeLayout.LayoutParams sendButtonParams = (RelativeLayout.LayoutParams) mSendButton.getLayoutParams();
                sendButtonParams.topMargin = sendButtonParams.topMargin - deltaHeight / 2;
                mSendButton.setLayoutParams(sendButtonParams);
            }
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        outState.putString("savedMsgContent", mMessageComposer.getText().toString());
        super.onSaveInstanceState(outState, outPersistentState);
    }
}

