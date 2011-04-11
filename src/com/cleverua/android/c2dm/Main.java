package com.cleverua.android.c2dm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.cleverua.android.c2dm.core.C2DMessaging;

/**
 * This sample application made according to C2DM documentation that can be found at http://code.google.com/android/c2dm/
 * */
public class Main extends Activity {
    public static final String C2DM_MESSAGE     = "c2dm_msg";
    public static final String C2DM_DESCRIPTION = "c2dm_desc";
    public static final String C2DM_POST_ACTION = "c2dm_post_action";
   
  //put your account here. You may sign up the application at http://code.google.com/android/c2dm/signup.html
    private static final String ACCOUNT = ""; 
    
    private static final int C2DM_RESULT_DIALOG = 100;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button registerBtn = (Button) findViewById(R.id.register_btn);
        registerBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /* The first time the application needs to use the messaging service, it fires off 
                 * a registration Intent to a C2DM server. */
                C2DMessaging.register(Main.this, ACCOUNT);
            }
        });
        
        Button unregisterBtn = (Button) findViewById(R.id.unregister_btn);
        unregisterBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                C2DMessaging.unregister(Main.this);
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getStringExtra(C2DM_MESSAGE) != null) {
            showDialog(C2DM_RESULT_DIALOG, intent.getExtras());
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id, final Bundle args) {
        Dialog d;
        if (id == C2DM_RESULT_DIALOG) {
            d = new AlertDialog.Builder(this)
            .setTitle(args.getString(C2DM_MESSAGE))
            .setMessage(args.getString(C2DM_DESCRIPTION))
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int arg1) {
                    String postErrorAction = args.getString(C2DM_POST_ACTION);
                    if (postErrorAction != null) {
                        Intent i = new Intent(postErrorAction);
                        startActivity(i);
                    }
                    d.dismiss();
                }
            }).create();
            
        } else {
            d = super.onCreateDialog(id);
        }
        return d;
    }
}