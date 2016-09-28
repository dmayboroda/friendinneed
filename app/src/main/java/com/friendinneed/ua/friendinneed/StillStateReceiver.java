package com.friendinneed.ua.friendinneed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.google.android.gms.awareness.fence.FenceState;

public class StillStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!TextUtils.equals(InneedService.FENCE_RECEIVER_ACTION, intent.getAction())) {
            //some wrong action?
            return;
        }

        FenceState fenceState = FenceState.extract(intent);

        switch (fenceState.getCurrentState()) {
            case FenceState.TRUE:
                InneedService.suspendInnedService(context);
                break;
            case FenceState.FALSE:
                break;
            default:
                //unknown state!
        }

    }
}