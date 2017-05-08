package com.pushprime.models;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.pushprime.PushPrime;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by PushPrime on 2/9/2017.
 */

/**
 * Represens an action button specified in action buttons array.
 */
public class PPNotificationButton {

    /**
     * Title of the button
     */
    public String title = "";

    /**
     * Icon reference to be displayed with action button in notification center
     */
    public int icon = 0;

    /**
     * Intent to launch when action button is clicked
     */
    public Intent launchIntent = null;

    /**
     * @hide
     */
    public int buttonIndex = -1;

    /**
     * Application Context Reference
     */
    public Context context;

    /**
     * Designated Initializer - Parses and converts JSON to PPNotificationButton
     * @param cxt Application Context
     * @param object JSONObject containing button data
     * @param index Index of the action button
     * @throws JSONException
     */
    public PPNotificationButton(Context cxt, JSONObject object, int index) throws JSONException{
        context = cxt;
        buttonIndex = index;
        title = object.getString("t");

        if(object.has("i")) {
            icon = PPNotification.getNotificationIcon(context, object.getString("i"));
        }

        if(object.has("a")){
            launchIntent = PPNotification.getIntentForActivity(context, object.getString("a"));
        }
    }

    /**
     * Returns wether or not this button has a launchable intent
     * @return
     */
    public boolean hasIntent(){
        return launchIntent != null;
    }

    /**
     * Get Action Intent for this action button. Please use {@link PPNotificationButton#getTrackedIntent(PPNotification)}.
     * @param notification
     * @return Action Intent
     */
    public Intent getIntent(PPNotification notification){
        if(launchIntent != null) {
            launchIntent.putExtra(PushPrime.NOTIFICATION, notification);
            launchIntent.putExtra(PushPrime.NOTIFICATION_BUTTON, buttonIndex);
        }
        return launchIntent;
    }

    /**
     * Returns a trackable intent for this action button
     * @param notification Notification this button is part of.
     * @return Trackable Action Intent
     */
    public Intent getTrackedIntent(PPNotification notification){
        Intent trackedIntent = notification.getTrackedIntent();
        trackedIntent.putExtra(PushPrime.NOTIFICATION_BUTTON, buttonIndex);
        trackedIntent.putExtra("youclicked", "yolo"+buttonIndex);
        return trackedIntent;
    }

    /**
     * @hide
     * @param notification
     * @return
     */
    public NotificationCompat.Action getTrackedAction(PPNotification notification){
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, this.getTrackedIntent(notification),
                PushPrime.sharedHandler().intentLaunchOptions);
        NotificationCompat.Action.Builder builder = new NotificationCompat.Action.Builder(icon, title, pendingIntent);
        return  builder.build();
    }
}
