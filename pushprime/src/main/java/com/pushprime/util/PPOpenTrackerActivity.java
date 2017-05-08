package com.pushprime.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.pushprime.PushPrime;

/**
 * Created by PushPrime on 03/04/2017.
 */

/**
 * Tracked Activity to handle clicks
 */
public class PPOpenTrackerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PushPrime.processOpenerActivity(this, getIntent().getExtras());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        PushPrime.processOpenerActivity(this, getIntent().getExtras());
    }
}