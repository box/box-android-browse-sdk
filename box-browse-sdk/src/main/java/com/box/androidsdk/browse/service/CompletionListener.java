package com.box.androidsdk.browse.service;

import android.support.v4.content.LocalBroadcastManager;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxLogUtils;

public class CompletionListener implements BoxFutureTask.OnCompletedListener {

    private static final String TAG = CompletionListener.class.getName();

    private final LocalBroadcastManager mBroadcastManager;

    public CompletionListener(LocalBroadcastManager broadcastManager) {
        mBroadcastManager = broadcastManager;
    }

    @Override
    public void onCompleted(BoxResponse response) {
        BoxResponseIntent intent = new BoxResponseIntent(response);
        System.out.println("onCompleted " + response + " * " + response.getRequest() + " xx " + response.getResult());
        if (!response.isSuccess()) {
            BoxLogUtils.e(TAG, response.getException());
        }
        System.out.println("onCompleted before send " + intent + " mBroadcastManager " + mBroadcastManager);

        mBroadcastManager.sendBroadcast(intent);
    }

}
