package com.pushprime.util;

import com.pushprime.models.PPNotification;

/**
 * Created by PushPrime on 2/8/2017.
 */

/**
 * Notification Received Interface
 */
public interface PPNotificationReceivedListener {

    /**
     * Called whenever a notification is received by the application
     * @param notification Reveived notification
     */
    void PPNotificationReceived(PPNotification notification);

}
