package com.pushprime.fcm;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.pushprime.PushPrime;

/**
 * @hide
 */
public class PPInstanceIdService extends FirebaseInstanceIdService {

    public PPInstanceIdService() {
    }

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();
        PushPrime.sharedHandler().handleToken(FirebaseInstanceId.getInstance().getToken());
    }
}
