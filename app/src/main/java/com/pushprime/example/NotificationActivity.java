package com.pushprime.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.pushprime.PushPrime;
import com.pushprime.models.PPNotification;

/**
 * Created by PushPrime on 2/14/2017.
 */

public class NotificationActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extra = this.getIntent().getExtras();

        PPNotification notification = (PPNotification) extra.get(PushPrime.NOTIFICATION);
        String userId = extra.getString("user_id");
    }
}
