package com.friendinneed.ua.friendinneed;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by mymac on 4/17/16.
 */
public class TickerDialog2222 extends DialogFragment implements View.OnClickListener {



    TextView timerTextView;
    CountDownTimer countDownTimer = new CountDownTimer(10000, 1000){

        @Override
        public void onTick(long millisUntilFinished) {
            timerTextView.setText("" + millisUntilFinished/1000);
        }

        @Override
        public void onFinish() {

            timerTextView.setText("Message sending");
        }
    };



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        getDialog().setTitle(R.string.dialog_title);
        View dialogView = inflater.inflate(R.layout.dialog_layout, null);
        dialogView.findViewById(R.id.btnSend).setOnClickListener(this);
        dialogView.findViewById(R.id.btnDiscard).setOnClickListener(this);
        timerTextView = (TextView) dialogView.findViewById(R.id.dialog_timer_text);

        countDownTimer.start();

        return dialogView;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnSend:

                break;
            case R.id.btnDiscard:
                countDownTimer.cancel();
        }
    }


}
