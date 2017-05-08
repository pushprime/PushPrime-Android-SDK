package com.pushprime;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.pushprime.models.PPNotification;
import com.pushprime.models.PPNotificationButton;
import com.pushprime.util.PPActivityLifeCycleHandler;
import com.pushprime.util.PPApi;
import com.google.firebase.iid.FirebaseInstanceId;
import com.pushprime.util.PPLog;
import com.pushprime.util.PPNotificationClickedListener;
import com.pushprime.util.PPNotificationReceivedListener;
import com.pushprime.util.PPStorage;

import org.json.JSONObject;

/**
 * Created by PushPrime on 10/7/2016.
 */

    /*
    TODO: Lock screen display
   */

/**
 * `PushPrime` is responsible for handling the core features of this sdk. This class is an abstraction layer over the PushPrime service and makes it easy to integrate your iOS application with PushPrime.
 *  This class should neither be instantiated nor subclassed instead the sharedHandler static method should be used to obtain a reference to underlying global object.
 */

public class PushPrime {

    /**
     * PushPrime API Key, this value is automatically set and should not be modified manually.
     */
    public String pushPrimeApiKey = "";

    /**
     * Application Context Reference
     */
    public Context applicationContext;

    /**
     * Reference to currently visisble activity
     */
    public Activity currentActivity;

    /**
     * PushPrim Key representing the notification on which action took place. This key can be used to extract the notification from extras inside launched activity;
     */
    public static String NOTIFICATION = "PPNotification";

    /**
     * PushPrim Button Key representing the clicked button. This key can be used to extract the clicked button from extras inside launched activity;
     */
    public static String NOTIFICATION_BUTTON = "PPNotificationButton";

    /**
     * Broadcast identifier for received notifications. In addition to {@link PPNotificationReceivedListener}, you can listen to broadcasts to receive notifications in multiple places inside your app.
     */
    public static String RECEIVED_BROADCAST = "com.pushprime.android.RECEIVED";

    /**
     * PushPrime SDK Static object. This should not be initialized manually, instead static sharedHandler method should be used to obtain a reference to it.
     * @see PushPrime#sharedHandler()
     */
    public static PushPrime handler;

    /**
     * PushPrime storage object
     */
    public PPStorage storage;

    /**
     * Notification received Listener
     * @see PPNotificationReceivedListener
     */
    public PPNotificationReceivedListener receiveListener;

    /**
     * Notification Clicked Listener
     * @see PPNotificationClickedListener
     */
    public PPNotificationClickedListener clickListener;

    /**
     * Flags to use while launching the intent when user clicks an activity. You can use any value or multiple values specified in {@link PendingIntent}
     */
    public int intentLaunchOptions = PendingIntent.FLAG_ONE_SHOT;

    /**
     * @hide
     */
    private String segmentId = null;

    /**
     * @hide
     */
    private JSONObject customData = null;

    /**
     * @hide
     */
    public PushPrime(){
        this.storage = new PPStorage();
    }

    /**
     * Creates and reutns a PushPrime SDK Object which can be used to call various sdk methods.
     * @return PushPrime SDK Object
     */
    public static PushPrime sharedHandler() {
        if (handler == null) {
            handler = new PushPrime();
        }
        return handler;
    }

    /**
     * Initializes the SDK and starts the opt-in process.
     * @param context Application Context
     */
    public void initialize(Context context){
        this.applicationContext = context.getApplicationContext();
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2) {
            ((Application) context).registerActivityLifecycleCallbacks(new PPActivityLifeCycleHandler());
        } else {
            //in app notification dialog only supported on version 4.0+
        }

        try {
            ApplicationInfo applicationInfo = this.applicationContext.getPackageManager().getApplicationInfo(this.applicationContext.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            this.pushPrimeApiKey = bundle.getString("pushprime_api_key");
            PPLog.print("API Key is: "+this.pushPrimeApiKey);
        } catch (PackageManager.NameNotFoundException ignore){

        }

        if(FirebaseInstanceId.getInstance().getToken() != null){
            this.handleToken(FirebaseInstanceId.getInstance().getToken());
        }

        this.heartBeat();
    }

    /**
     * @hide
     * Handles token submission to server.
     * @param token Token String
     */
    public void handleToken(String token){
        if(this.isReady()) {
            if (!this.storage.get(PPStorage.FCM_TOKEN, "").equalsIgnoreCase(token)) {
                register(token);
                PPLog.print("Token Updated, Send to server");
            }
            PPLog.print("Registration Token is " + FirebaseInstanceId.getInstance().getToken());
        }
    }

    /**
     * @hide
     * Actually sends the token to the server
     * @param token Token String
     */
    public void register(final String token){
        if(this.isReady()) {
            String previousId = this.storage.get(PPStorage.PUSHPRIME_ID, "");
            PPApi builder = PPApi.Builder().setEndPoint("/register")
                    .setMethod("POST")
                    .setParameter("endpoint", token)
                    .setParameter("t", "android")
                    .setParameter("pi", previousId);

            if(this.segmentId != null){
                builder.setParameter("s", segmentId);
            }

            if(this.customData != null){
                builder.setParameter("c", customData.toString());
            }

            builder.setCallback(new PPApi.Callback() {
                @Override
                public void onSuccess(int statusCode, JSONObject content) {
                    try {
                        if (content.getString("status").equalsIgnoreCase("success")) {
                            String pushPrimeId = content.getString("t");
                            PushPrime.this.storage.save(PPStorage.PUSHPRIME_ID, pushPrimeId);
                            PushPrime.this.storage.save(PPStorage.FCM_TOKEN, token);
                            PPLog.print("Saving PushPrime Id: " + pushPrimeId);
                        }
                    }catch (Exception e){

                    }
                }

                @Override
                public void onError(int statusCode, Exception exception) {
                    PPLog.print("");
                }
            })
                    .send();
        }
    }

    /**
     * Returns whether the user is subscribed for notifications or not
     * @return true if subscribed, false otherwise
     */
    public boolean isRegistered(){
        String previousId = this.storage.get(PPStorage.PUSHPRIME_ID, "");
        return !previousId.isEmpty();
    }

    /**
     * @hide
     * Sends heartbeat event to sever.
     */
    public void heartBeat(){
        if(this.isReady() && this.isRegistered()) {

            long currentTime = System.currentTimeMillis();
            long previousTime = this.storage.getLong(PPStorage.PREVIOUS_HEARTBEAT, 0);

            long difference = (currentTime - previousTime)/(1000 * 60);

            if(difference > 40) {
                this.storage.saveLong(PPStorage.PREVIOUS_HEARTBEAT, System.currentTimeMillis());
                String previousId = this.storage.get(PPStorage.PUSHPRIME_ID, "");
                PPApi.Builder().setEndPoint("/heartbeat/t/" + previousId)
                        .setMethod("GET")
                        .send();
            }
        }
    }

    /**
     * Add current user to the specified segment
     * @param segmentId Id of the segment to add user to
     */
    public void setSegment(String segmentId){
        if(this.isReady() && this.isRegistered()) {
            String previousId = this.storage.get(PPStorage.PUSHPRIME_ID, "");
            PPApi.Builder().setEndPoint("/setSegment")
                    .setMethod("POST")
                    .setParameter("s", segmentId)
                    .setParameter("t", previousId)
                    .send();
        } else {
            this.segmentId = segmentId;
        }
    }

    /**
     * Remove current user from the specified segment
     * @param segmentId Id of the segment to remove user from
     */
    public void removeSegment(String segmentId){
        if(this.isReady() && this.isRegistered()) {
            String previousId = this.storage.get(PPStorage.PUSHPRIME_ID, "");
            PPApi.Builder().setEndPoint("/removeSegment")
                    .setMethod("POST")
                    .setParameter("s", segmentId)
                    .setParameter("t", previousId)
                    .send();
        }
    }

    /**
     * Save custom data for the current user. It is recommended to specify a numerical id for the user for proper handling of the custom data in PushPrim dashboard.
     * @param customData JSONObject containing the custom data.
     */
    public void setCustomData(JSONObject customData){
        if(this.isReady() && this.isRegistered()) {
            String previousId = this.storage.get(PPStorage.PUSHPRIME_ID, "");
            PPApi.Builder().setEndPoint("/customData")
                    .setMethod("POST")
                    .setParameter("c", customData.toString())
                    .setParameter("t", previousId)
                    .send();
        } else {
            this.customData = customData;
        }
    }

    /**
     * Checks if SDK has been properly initialized or not
     * @return true if initialized, false otherwise
     */
    public boolean isReady(){
        boolean isReady = this.applicationContext != null && !this.pushPrimeApiKey.isEmpty();
        if(!isReady){
            PPLog.print("PushPrime isn't ready, please make sure you have called initialize in your application class");
        }
        return isReady;
    }

    /**
     * @hide
     * Inform all receivers & listeners about an incoming notification.
     * @param notification Notification received.
     */
    public void notifyClients(PPNotification notification){
        if(receiveListener != null){
            receiveListener.PPNotificationReceived(notification);
        }

        Intent broadcastIntent = new Intent(RECEIVED_BROADCAST);
        broadcastIntent.putExtra(NOTIFICATION, notification);
        notification.context.sendBroadcast(broadcastIntent);
    }

    /**
     * @hide
     * Run the runner on Main UI Thread.
     * @param runner Runnable Task
     */
    public static void runOnUIThread(Runnable runner){
        new Handler(Looper.getMainLooper()).post(runner);
    }

    /**
     * @hide
     * Internal processing, DO NOT USE!
     * @param activity
     * @param extras
     */
    public static void processOpenerActivity(Activity activity, Bundle extras){
        if(extras != null){
            PPNotification notification = (PPNotification)extras.get(NOTIFICATION);
            int buttonIndex = extras.getInt(NOTIFICATION_BUTTON, -1);

            if(notification != null){
                if(!notification.tag.isEmpty()) {
                    PPApi.Builder().setMethod("GET").setEndPoint("/track/click/wtf/" + notification.tag).send();
                }

                Intent launchIntent = null;
                if(buttonIndex == -1){
                    launchIntent = notification.getNotificationIntent();
                } else {
                    PPNotificationButton clickedButton = notification.notificationButtons.get(buttonIndex);
                    launchIntent = clickedButton.getIntent(notification);
                }

                if(launchIntent != null){
                    activity.startActivity(launchIntent);
                    activity.finish();
                }
            }
        }
    }
}
