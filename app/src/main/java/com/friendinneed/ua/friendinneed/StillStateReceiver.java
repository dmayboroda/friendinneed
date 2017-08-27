package com.friendinneed.ua.friendinneed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.google.android.gms.awareness.fence.FenceState;

public class StillStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TextUtils.equals(InNeedService.FENCE_RECEIVER_ACTION, intent.getAction())) {
            //some wrong action?
            return;
        }

        FenceState fenceState = FenceState.extract(intent);

        switch (fenceState.getCurrentState()) {
            case FenceState.TRUE:
                // TODO test this precisely then uncomment
                //InNeedService.suspendInnedService(context);
                break;
            case FenceState.FALSE:
                //InNeedService.resumeInnedService(context);
                break;
            default:
                //unknown state!
        }

    }
}