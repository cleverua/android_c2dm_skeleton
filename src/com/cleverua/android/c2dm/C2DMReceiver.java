package com.cleverua.android.c2dm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.cleverua.android.c2dm.core.C2DMBaseReceiver;

public class C2DMReceiver extends C2DMBaseReceiver {
    private static final String TAG = "C2DMReceiver";
    
	private static final String ITEM_ID_KEY = "item_id";
    private static final String MESSAGE_KEY = "message";
    
    private static final int C2DM_EVENT_ID = 1;

    public C2DMReceiver() {
		super(TAG);
	}

	@Override
	public void onRegistrered(Context context, String registrationId) {
		Log.w(TAG, "Registered: " + registrationId);
		postNotification(new Intent(this, Main.class), getString(R.string.registered_msg));
		
		/* To complete the registration, the application sends the registration ID 
		 * to the application server. The application server typically stores the registration ID in a 
		 * database to identify the particular device.
		 */
	}
	
	@Override
	public void onUnregistered(Context context) {
		Log.w(TAG, getString(R.string.unregistered_msg));
		postNotification(new Intent(this, Main.class), getString(R.string.unregistered_msg));
	}
	
	@Override
	public void onError(Context context, String errorId) {
		Log.w(TAG + "-onError", errorId);
		
		String errorMessage = null;
		Intent notificationIntent = new Intent(this, Main.class);
		notificationIntent.putExtra(Main.C2DM_MESSAGE, errorId);
		if (ACCOUNT_MISSING.equals(errorId)) {
		    errorMessage = ACCOUNT_MISSING_MSG;
		    notificationIntent.putExtra(Main.C2DM_POST_ACTION, Settings.ACTION_ADD_ACCOUNT);
		} else if (AUTHENTICATION_FAILED.equals(errorId)) {
		    errorMessage = AUTHENTICATION_FAILED_MSG;
		} else if (TOO_MANY_REGISTRATIONS.equals(errorId)) {
		    errorMessage = TOO_MANY_REGISTRATIONS_MSG;
		} else if (PHONE_REGISTRATION_ERROR.equals(errorId)) {
		    errorMessage = PHONE_REGISTRATION_ERROR_MSG;
		} else if (INVALID_SENDER.equals(errorId)) {
		    errorMessage = INVALID_SENDER_MSG;
		} else if (SERVICE_NOT_AVAILABLE.equals(errorId)) {
		    errorMessage = SERVICE_NOT_AVAILABLE_MSG;
		}
		notificationIntent.putExtra(Main.C2DM_DESCRIPTION, errorMessage);
		postNotification(notificationIntent, errorMessage);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
	    Log.d(TAG, getString(R.string.message_alert));
	    for (String key : intent.getExtras().keySet()) {
	        Log.d(key, intent.getStringExtra(key));
	    }
        String message = intent.getStringExtra(MESSAGE_KEY);
        String itemId = intent.getStringExtra(ITEM_ID_KEY);
        if (message != null) {
            if (itemId == null) {
                /* Multiple items update for example. */
            } else {
                /* Single item update for example.*/
            }
            /* In this example we will only display the message on the Main activity.*/
            Intent notificationIntent = new Intent(this, Main.class);
            notificationIntent.putExtra(Main.C2DM_MESSAGE, getString(R.string.message_alert));
            notificationIntent.putExtra(Main.C2DM_DESCRIPTION, message);
            postNotification(notificationIntent, message);
        }
	}
	
	private void postNotification(Intent notificationIntent, String message) {
	    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon, getString(R.string.app_name), System.currentTimeMillis());
        notification.flags |= Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setLatestEventInfo(this, getString(R.string.app_name), message, contentIntent);
        notificationManager.notify(C2DM_EVENT_ID, notification);
	}
}
