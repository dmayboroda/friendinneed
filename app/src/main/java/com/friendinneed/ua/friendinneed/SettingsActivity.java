package com.friendinneed.ua.friendinneed;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;

/**
 * Created by mymac on 8/7/16.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    Preference timerPreference;
    TextView tv;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_general);

        timerPreference = findPreference(getString(R.string.timer_key));
        updateTimerUi();






        EditTextPreference etp = (EditTextPreference) findPreference("edit_text_pref");
        etp.setTitle("Contacts");
        etp.setText("yyyyyyyyyyyyyyyyy");

        String contactName = "David";

        //convert image to spannableString



        final SpannableStringBuilder sb = new SpannableStringBuilder();
        TextView tv = createContactTextView(contactName);

        BitmapDrawable bd = (BitmapDrawable) convertViewToDrawable(tv);
        bd.setBounds(0, 0, bd.getIntrinsicWidth(),bd.getIntrinsicHeight());

        sb.append(contactName + ",");
        sb.setSpan(new ImageSpan(bd), sb.length()-(contactName.length()+1), sb.length()-1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        etp.getEditText().setText(sb);
        etp.setText("ddd");


    }


    public TextView createContactTextView(String text){
        //creating textview dynamically
        tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(20);
        tv.setBackgroundResource(R.drawable.widget_off);
            tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.app_icon, 0);
        return tv;
    }

    public static Object convertViewToDrawable(View view) {
//        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

//        view.measure(spec, spec);
//        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());


        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, 100, 100);

        Bitmap b = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.translate(-view.getScrollX(), -view.getScrollY());
        view.draw(c);
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache(true);
        Bitmap cacheBmp = view.getDrawingCache(true);
        Bitmap viewBmp = cacheBmp.copy(Bitmap.Config.ARGB_8888, true);
//                view.setDrawingCacheEnabled(false);
        view.destroyDrawingCache();
        return new BitmapDrawable(viewBmp);
    }








    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(getString(R.string.timer_key))) {
            updateTimerUi();
        }
    }

    private void updateTimerUi() {
        timerPreference.setTitle(String.format(getString(R.string.timer_pref_title),
                PreferenceManager.getDefaultSharedPreferences(this).
                        getInt(getString(R.string.timer_key), 15)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);








    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }












}
