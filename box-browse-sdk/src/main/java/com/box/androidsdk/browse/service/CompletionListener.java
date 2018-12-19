package com.box.androidsdk.browse.service;

import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxLogUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class CompletionListener implements BoxFutureTask.OnCompletedListener {

    private static final String TAG = CompletionListener.class.getName();

    private final LocalBroadcastManager mBroadcastManager;

    /**
     * Instantiates a new Completion listener.
     *
     * @param broadcastManager the broadcast manager
     */
    public CompletionListener(LocalBroadcastManager broadcastManager) {
        mBroadcastManager = broadcastManager;
    }

    @Override
    public void onCompleted(BoxResponse response) {
        BoxResponseIntent intent = new BoxResponseIntent(response);
        if (!response.isSuccess()) {
            BoxLogUtils.e(TAG, response.getException());
        }

        mBroadcastManager.sendBroadcast(intent);
    }

}
