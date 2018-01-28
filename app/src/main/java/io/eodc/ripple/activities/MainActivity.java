package io.eodc.ripple.activities;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import io.eodc.ripple.BuildConfig;
import io.eodc.ripple.R;
import io.eodc.ripple.telephony.Conversation;
import io.eodc.ripple.util.QueryHelper;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private RecyclerView conversationList;
    private FloatingActionButton newConversationFab;

    private List<Conversation> conversations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String packageName = getPackageName();
        conversations = new ArrayList<>();

        // Plant Timber tree if debug build
        if (BuildConfig.DEBUG)
            Timber.plant(new Timber.DebugTree());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        newConversationFab = findViewById(R.id.new_conversation_fab);
        newConversationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Telephony.Sms.getDefaultSmsPackage(getApplicationContext()).equals(packageName)) {
                    Snackbar.make(view, "I'm not the default SMS app!", Snackbar.LENGTH_LONG)
                            .setAction("Make Default", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName);
                                    startActivity(intent);
                                }
                            }).show();
                }
            }
        });

        conversationList = findViewById(R.id.conversation_list);
        QueryHelper.queryContacts(getContentResolver(), this, conversations, conversationList);
    }

}
