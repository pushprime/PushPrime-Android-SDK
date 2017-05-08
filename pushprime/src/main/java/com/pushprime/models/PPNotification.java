package com.pushprime.models;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.pushprime.PushPrime;
import com.pushprime.R;
import com.pushprime.util.PPApi;
import com.pushprime.util.PPLog;
import com.pushprime.util.PPOpenTrackerActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Created by PushPrime on 2/3/2017.
 */

/**
 * Represent a notification received from the server. This class is parcelable and it is safe to use PPNotification objects in activity extras etc.
 */
public class PPNotification implements Parcelable {

    /**
     * Application Context Reference, Could be null
     */
    public Context context;

    /**
     * Title of the notification
     */
    public String title = "";

    /**
     * Body of the notification
     */
    public String body = "";

    /**
     * Icon name for the notification.
     */
    public String icon = "";

    /**
     * Image URL to use for thumbnail
     */
    public String image = "";

    /**
     * Activity to launch when this notification is clicked
     */
    public String launchActivity = "";

    /**
     * Sound file to play when notification is received, the file specified should already exist in application bundle
     */
    public String sound = "";

    /**
     * @hide
     * Sring containing info about action buttons
     */
    public String buttonsJSON = "";

    /**
     * @hide
     * String containing custom data.
     */
    public String dataJSON = "";

    /**
     * @hide
     * Notification tag
     */
    public String tag = "";

    /**
     * Badge count to set on application icon in launcher
     */
    public int badgeCount = 0;

    /**
     * In app alert type, 0 for none, 1 for alert dialog
     */
    public int inAppAlertType = 1; // 0 = none, 1 = alert

    /**
     * Boolean value specifying if the notification have been shown to the user.
     */
    public boolean shownToTheUser = false;

    /**
     * JSONObject containing custom data
     */
    public JSONObject customData = new JSONObject();

    /**
     * An array of action buttons specified in the notification. Each index contains a {@link PPNotificationButton} object.
     */
    public ArrayList<PPNotificationButton> notificationButtons = new ArrayList<>();

    /**
     * @hide
     */
    public static final Creator<PPNotification> CREATOR = new Creator<PPNotification>() {
        @Override
        public PPNotification createFromParcel(Parcel in) {
            return new PPNotification(in);
        }

        @Override
        public PPNotification[] newArray(int size) {
            return new PPNotification[size];
        }
    };

    /**
     * Designated Initializer, Initialized the notification and parses the payload
     * @param cxt Application Context
     * @param remoteMessage {@link RemoteMessage} object containing notification payload
     */
    public PPNotification(Context cxt, RemoteMessage remoteMessage){

        this.context = cxt;

        Map<String, String> data = remoteMessage.getData();

        if(data.containsKey("nt")){ // title
            title = data.get("nt");
        }

        if(data.containsKey("nb")){ // body
            body = data.get("nb");
        }

        if(data.containsKey("ni")){ // icon
            icon = data.get("ni");
        }

        if(data.containsKey("ds")){ // sound
            sound = data.get("ds");
        }

        if(data.containsKey("nbu")){ // action buttons
            buttonsJSON = data.get("nbu");
            this.parseButtonsJSON();
        }

        if(data.containsKey("custom")){ // custom data
            dataJSON = data.get("custom");
            this.parseCustomDataJSON();
        }

        if(data.containsKey("t")){ // sound
            tag = data.get("t");
        }

        if(data.containsKey("db")){ // badge
            badgeCount = Integer.parseInt(data.get("db"));
        }

        if(data.containsKey("dt")){ // image thumbnail
            image = data.get("dt");
        }

        if(data.containsKey("dl")){ // launch activity
            launchActivity = data.get("dl");
        }

        if(data.containsKey("di")){ // in-app alert
            inAppAlertType = Integer.parseInt(data.get("di"));
        }

        PPLog.print("Notification parsing done");
    }

    /**
     * @hide
     * @param in Parcel
     */
    protected PPNotification(Parcel in) {

        if(context == null){
            context = PushPrime.sharedHandler().applicationContext;
        }

        title = in.readString();
        body = in.readString();
        icon = in.readString();
        image = in.readString();
        launchActivity = in.readString();
        sound = in.readString();
        buttonsJSON = in.readString();
        dataJSON = in.readString();
        tag = in.readString();
        badgeCount = in.readInt();
        inAppAlertType = in.readInt();
        shownToTheUser = in.readInt() != 0;

        this.parseButtonsJSON();
        this.parseCustomDataJSON();
    }

    /**
     * Returns wether or not the notification can be shown to user
     * @return true if can be displayed, false otherwise
     */
    public boolean canDisplay(){
        if(title.isEmpty() && body.isEmpty()){
            return false;
        }

        return true;
    }

    /**
     * Show Notification to the user. If application is in background and notification is not silent, than notification will appear in notification center. If Application is in foreground, than {@link PPNotification#inAppAlertType} will decide if notification will be displayed or not.
     */
    public void showNotification(){

        Intent notificationIntent = this.getTrackedIntent();
        ShortcutBadger.applyCount(context, badgeCount);


        if(this.canDisplay()) {
            if (inAppAlertType == 1 && PushPrime.sharedHandler().currentActivity != null) {
                shownToTheUser = true;
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(PushPrime.sharedHandler().currentActivity);
                alertDialog.setTitle(this.title);
                alertDialog.setMessage(this.body);
                if(notificationButtons.size() >= 1) {
                    PPNotificationButton button = notificationButtons.get(0);
                    alertDialog.setPositiveButton(button.title, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PPNotificationButton button = notificationButtons.get(0);
                            if(button.hasIntent()) {
                                Intent intent = button.getTrackedIntent(PPNotification.this);
                                PushPrime.sharedHandler().currentActivity.startActivity(intent);
                            }
                        }
                    });
                }

                if(notificationButtons.size() >= 2) {
                    PPNotificationButton button = notificationButtons.get(1);
                    alertDialog.setNegativeButton(button.title, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PPNotificationButton button = notificationButtons.get(1);
                            if(button.hasIntent()) {
                                Intent intent = button.getTrackedIntent(PPNotification.this);
                                PushPrime.sharedHandler().currentActivity.startActivity(intent);
                            }
                        }
                    });
                }

                if(notificationButtons.size() >= 3) {
                    PPNotificationButton button = notificationButtons.get(2);
                    alertDialog.setNeutralButton(button.title, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PPNotificationButton button = notificationButtons.get(2);
                            if(button.hasIntent()) {
                                Intent intent = button.getTrackedIntent(PPNotification.this);
                                PushPrime.sharedHandler().currentActivity.startActivity(intent);
                            }
                        }
                    });
                }

                PushPrime.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        alertDialog.show();
                    }
                });
            } else if(inAppAlertType != 0){
                shownToTheUser = true;
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                        .setContentTitle(this.title)
                        .setContentText(this.body)
                        .setSmallIcon(getNotificationIcon(context, icon))
                        .setAutoCancel(true)
                        .setSound(getNotificationSound(context, sound));



                if (notificationButtons.size() > 0) {
                    for (int i = 0; i < notificationButtons.size(); i++) {
                        PPNotificationButton button = notificationButtons.get(i);
                        notificationBuilder.addAction(button.getTrackedAction(this));
                    }
                }

                if (notificationIntent != null) {
                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent,
                            PushPrime.sharedHandler().intentLaunchOptions);
                    notificationBuilder.setContentIntent(pendingIntent);
                }

                if(!image.isEmpty()){
                    Bitmap bitmap;
                    if(image.startsWith("http://") || image.startsWith("https://")){
                        bitmap = getBitmapFromURL(image);
                    }else{
                        int largeIcon = PPNotification.getNotificationIcon(context, image);
                        bitmap = BitmapFactory.decodeResource(context.getResources(), largeIcon);
                    }

                    if(bitmap != null){
                        notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap).setSummaryText(body));
                    }
                }

                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(0, notificationBuilder.build());
            }
        }
    }

    /**
     * Get the intent which will be launched when notification is clicked. Please note that manually getting the intent and launching it will not be tracked in PushPrime dashboard. It is better to use {@link PPNotification#getTrackedIntent()} for any such scenario where you have to manually execute the intent.
     * @return Intent for the activity to launch
     */
    public Intent getNotificationIntent(){
        Intent originalIntent = getIntentForActivity(context, launchActivity);
        originalIntent.putExtra(PushPrime.NOTIFICATION, this);
        return originalIntent;
    }

    /**
     * Get Trackable intent for the activity to launch when notification is clicked
     * @return Tracked Intent for the activity to launch
     */
    public Intent getTrackedIntent(){
        Intent trackedIntent = new Intent(context, PPOpenTrackerActivity.class);
        trackedIntent.putExtra(PushPrime.NOTIFICATION, this);
        return trackedIntent;
    }

    /**
     * Fetch custom data from the notification.
     * @param key Key to fetch data against from custom data
     * @param defaultValue Default value to return if key doesn't exist of is null.
     * @return Value specified against the key or default value
     */
    public String getCustomData(String key, String defaultValue){
        try{
            return customData.getString(key);
        }catch (Exception ignore){

        }

        return defaultValue;
    }

    /**
     * @hide
     */
    private void parseButtonsJSON(){
        try{
            JSONArray buttons = new JSONArray(buttonsJSON);
            for (int i = 0; i < buttons.length(); i++) {
                JSONObject object = buttons.getJSONObject(i);
                notificationButtons.add(new PPNotificationButton(context, object, i));
            }
        }catch (Exception ignore){
            buttonsJSON = "";
        }
    }

    /**
     * @hide
     */
    private void parseCustomDataJSON(){
        try{
            this.customData = new JSONObject(dataJSON);
        }catch (Exception ignore){
            this.customData = new JSONObject();
            dataJSON = "";
        }
    }

    /**
     * @hide
     * @param location To fetch image from
     * @return Bitmap object of image
     */
    private Bitmap getBitmapFromURL(String location) {
        try {
            return BitmapFactory.decodeStream(new URL(location).openConnection().getInputStream());
        } catch (Throwable t) {}

        return null;
    }

    /**
     * Get reference to the icon specified in notification. This method will search for the icon in application bundle, if it is not present, than it will look for a default icon with name ic_stat_pushprime, if that icon is missing as well than it will return default pushprime icon.
     * @param cxt Application Context.
     * @param icon String containing icon name
     * @return Icon reference
     */
    public static int getNotificationIcon(Context cxt, String icon){
        int imageResource = 0;
        if(!icon.isEmpty()) {
            imageResource = cxt.getResources().getIdentifier(icon, "drawable", cxt.getPackageName());
            if (imageResource != 0) {
                return imageResource;
            }
        }

        imageResource = cxt.getResources().getIdentifier("ic_stat_pushprime", "drawable", cxt.getPackageName());
        if(imageResource != 0){
            return imageResource;
        }
        return R.drawable.ic_stat_pushprime_fallback;
    }

    /**
     * Returns a Uri pointing towards a sound file inside application bundle. If sound file is missing the default sound is returned.
     * @param cxt Application Context
     * @param sound String containing sound file name
     * @return Uri pointing towards sound file
     */
    public static Uri getNotificationSound(Context cxt, String sound){
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if(!sound.isEmpty()) {
            int soundResource = cxt.getResources().getIdentifier(sound, "raw", cxt.getPackageName());
            if (soundResource != 0) {
                soundUri = Uri.parse("android.resource://"
                        + cxt.getPackageName() + "/" + soundResource);
            }
        }

        return soundUri;
    }

    /**
     * @hide
     * @param context
     * @param activity
     * @return
     */
    public static Intent getIntentForActivity(Context context, String activity){
        Intent createdIntent = null;
        if(!activity.isEmpty()){
            try {
                Class<?> className = Class.forName(activity);
                createdIntent = new Intent(context, className);
                createdIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return createdIntent;
            } catch (ClassNotFoundException e) {
                // TODO Auto-generated catch block
                PPLog.print("Unable to find activity "+activity);
            }
        }
        //by default we will launch the launcer activity
        try {
            PackageManager pm = context.getPackageManager();
            createdIntent = pm.getLaunchIntentForPackage(context.getPackageName());
            createdIntent.setAction(Intent.ACTION_MAIN);
            createdIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            createdIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }catch (Exception e) {
            // TODO Auto-generated catch block
            PPLog.print("Unable to find activity "+activity);
        }

        return createdIntent;
    }

    /**
     * @hide
     * @return
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @hide
     * @param dest
     * @param flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(body);
        dest.writeString(icon);
        dest.writeString(image);
        dest.writeString(launchActivity);
        dest.writeString(sound);
        dest.writeString(buttonsJSON);
        dest.writeString(dataJSON);
        dest.writeString(tag);
        dest.writeInt(badgeCount);
        dest.writeInt(inAppAlertType);
        dest.writeInt(shownToTheUser ? 1 : 0);
    }
}
