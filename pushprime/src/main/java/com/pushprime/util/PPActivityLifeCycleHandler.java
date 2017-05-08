package com.pushprime.util;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.pushprime.PushPrime;

/**
 * Created by PushPrime on 2/9/2017.
 */

/**
 * @hide
 * Listens for activity events and updates {@link PushPrime#currentActivity} accordingly
 */
public class PPActivityLifeCycleHandler implements Application.ActivityLifecycleCallbacks {

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        PushPrime.sharedHandler().currentActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if(activity == PushPrime.sharedHandler().currentActivity){
            PushPrime.sharedHandler().currentActivity = null;
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if(activity == PushPrime.sharedHandler().currentActivity){
            PushPrime.sharedHandler().currentActivity = null;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if(activity == PushPrime.sharedHandler().currentActivity){
            PushPrime.sharedHandler().currentActivity = null;
        }
    }
}
