package com.pushprime.fcm;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pushprime.PushPrime;
import com.pushprime.models.PPNotification;
import com.pushprime.util.PPApi;

/**
 * @hide
 */
public class PPMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        PPNotification notification = new PPNotification(this, remoteMessage);
        notification.showNotification();
        PushPrime.sharedHandler().notifyClients(notification);
        if(!notification.tag.isEmpty()) {
            PPApi.Builder().setMethod("GET").setEndPoint("/track/delivery/wtf/" + notification.tag).send();
        }
    }
}
