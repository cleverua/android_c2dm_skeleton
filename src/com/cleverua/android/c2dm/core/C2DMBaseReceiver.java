/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cleverua.android.c2dm.core;

import java.io.IOException;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Base class for C2D message receiver. Includes constants for the
 * strings used in the protocol.
 */
public abstract class C2DMBaseReceiver extends IntentService {

    public static final String REGISTRATION_CALLBACK_INTENT = "com.google.android.c2dm.intent.REGISTRATION";

    // Extras in the registration callback intents.
    public static final String EXTRA_UNREGISTERED    = "unregistered";
    public static final String EXTRA_ERROR           = "error";
    public static final String EXTRA_REGISTRATION_ID = "registration_id";

    public static final String SERVICE_NOT_AVAILABLE  = "SERVICE_NOT_AVAILABLE";
    public static final String ACCOUNT_MISSING        = "ACCOUNT_MISSING";
    public static final String AUTHENTICATION_FAILED  = "AUTHENTICATION_FAILED";
    public static final String TOO_MANY_REGISTRATIONS = "TOO_MANY_REGISTRATIONS";
    public static final String INVALID_PARAMETERS     = "INVALID_PARAMETERS";
    public static final String INVALID_SENDER         = "INVALID_SENDER";
    public static final String PHONE_REGISTRATION_ERROR = "PHONE_REGISTRATION_ERROR";
    
    public static final String SERVICE_NOT_AVAILABLE_MSG  = "Failed to register. Please try again later.";
    public static final String ACCOUNT_MISSING_MSG        = "No Google account on the phone. Please, add a Google account.";
    public static final String AUTHENTICATION_FAILED_MSG  = "Bad password.";
    public static final String TOO_MANY_REGISTRATIONS_MSG = "Too many applications registered.";
    public static final String INVALID_SENDER_MSG         = "The sender account is not recognized.";
    public static final String PHONE_REGISTRATION_ERROR_MSG = "Phone doesn't currently support C2DM.";

    private static final String TAG = "C2DM"; // Logging tag

    private static final String C2DM_RECEIVE  = "com.google.android.c2dm.intent.RECEIVE";
    private static final String C2DM_RETRY    = "com.google.android.c2dm.intent.RETRY";

    private static final int DEFAULT_TIMEOUT_MS = 30000; // 30 seconds
    
    // wakelock
    private static final String WAKELOCK_KEY = "C2DM_LIB";

    private static PowerManager.WakeLock mWakeLock;
    private final String senderId;

    /**
     * The C2DMReceiver class must create a no-arg constructor and pass the 
     * sender id to be used for registration.
     */
    public C2DMBaseReceiver(String senderId) {
        // senderId is used as base name for threads, etc.
        super(senderId);
        this.senderId = senderId;
    }

    /**
     * Called when a cloud message has been received.
     */
    protected abstract void onMessage(Context context, Intent intent);

    /**
     * Called on registration error. Override to provide better
     * error messages.
     *	
     * This is called in the context of a Service - no dialog or UI.
     */
    public abstract void onError(Context context, String errorId);

    /**
     * Called when a registration token has been received.
     */
    public abstract void onRegistrered(Context context, String registrationId) throws IOException;

    /**
     * Called when the device has been unregistered.
     */
    public abstract void onUnregistered(Context context);


    @Override
    public final void onHandleIntent(Intent intent) {
        try {
            Context context = getApplicationContext();
            if (intent.getAction().equals(REGISTRATION_CALLBACK_INTENT)) {
                handleRegistration(context, intent);
            } else if (intent.getAction().equals(C2DM_RECEIVE)) {
                onMessage(context, intent);
            } else if (intent.getAction().equals(C2DM_RETRY)) {
                C2DMessaging.register(context, senderId);
            }
        } finally {
            /*	Release the power lock, so phone can get back to sleep.
            The lock is reference counted by default, so multiple 
            messages are ok.

            If the onMessage() needs to spawn a thread or do something else,
            it should use it's own lock.
            */
            mWakeLock.release();
        }
    }


    /**
     * Called from the broadcast receiver. 
     * Will process the received intent, call handleMessage(), registered(), etc.
     * in background threads, with a wake lock, while keeping the service 
     * alive. 
     */
    static void runIntentInService(Context context, Intent intent) {
        if (mWakeLock == null) {
            // This is called from BroadcastReceiver, there is no init.
            PowerManager pm =  (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
        }
        mWakeLock.acquire();

        // Use a naming convention, similar with how permissions and intents are 
        // used. 
        String receiver = context.getPackageName() + ".C2DMReceiver";
        intent.setClassName(context, receiver);
        
        context.startService(intent);

    }


    private void handleRegistration(final Context context, Intent intent) {
        final String registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID);
        String error = intent.getStringExtra(EXTRA_ERROR);
        String removed = intent.getStringExtra(EXTRA_UNREGISTERED);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "dmControl: registrationId = " + registrationId +
                    ", error = " + error + ", removed = " + removed);
        }

        if (removed != null) {
            // Remember we are unregistered
            C2DMessaging.clearRegistrationId(context);
            onUnregistered(context);
            return;
        } else if (error != null) {
            // we are not registered, can try again
            C2DMessaging.clearRegistrationId(context);
            // Registration failed
            Log.e(TAG, "Registration error " + error);
            onError(context, error);
            if (SERVICE_NOT_AVAILABLE.equals(error)) {
                /* The device can't read the response, or there was a 500/503 from the server 
                 * that can be retried later. 
                 */
                try {
                    Thread.sleep(DEFAULT_TIMEOUT_MS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                C2DMessaging.register(context, senderId);
            } 
        } else {
            try {
                onRegistrered(context, registrationId);
                C2DMessaging.setRegistrationId(context, registrationId);
            } catch (IOException ex) {
                Log.e(TAG, "Registration error " + ex.getMessage());
            }
        }
    }
}