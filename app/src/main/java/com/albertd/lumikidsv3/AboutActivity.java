package com.albertd.lumikidsv3;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

public final class AboutActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);l.setGravity(Gravity.CENTER);
        l.setPadding(30,30,30,30);l.setBackgroundColor(Color.rgb(247,250,255));
        LumiView lumi=new LumiView(this);l.addView(lumi,new LinearLayout.LayoutParams(-1,420));
        TextView t=new TextView(this);
        t.setText("Lumi Kids v3\n\nOffline language game for children\nEnglish • Deutsch • Русский\n\nNo ads • No analytics • No Google Play Services\nSpeech recognition: Vosk (offline)\n\nBuilt by Albert D");
        t.setTextSize(18);t.setTextColor(Color.rgb(17,47,89));t.setGravity(Gravity.CENTER);l.addView(t);
        setContentView(l);
    }
}
