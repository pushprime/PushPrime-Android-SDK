package com.pushprime.util;

import com.pushprime.models.PPNotification;

/**
 * Created by PushPrime on 03/04/2017.
 */

/**
 * Notification Clicked Interface
 */
public interface PPNotificationClickedListener {

    /**
     * Called whenever a notification or an action button is clicked
     * @param notification Notification on which the action took place
     * @param buttonIndex If user clicked a button, this will contain index of that button, -1 otherwise.
     */
    void PPNotificationClicked(PPNotification notification, int buttonIndex);

}
