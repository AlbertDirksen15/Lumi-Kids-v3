package com.albertd.lumikidsv3;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private LumiDb db;
    private VoiceEngine voice;
    private LumiView lumi;
    private Spinner knownSpinner,targetSpinner,uiSpinner,voiceSpinner;
    private boolean reader=false;
    private String ui="ru";

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);adaptOrientation();
        db=new LumiDb(this);voice=new BundledVoiceEngine(this);
        ui=db.getSetting("ui","ru");
        reader="reader".equals(db.getSetting("mode","pre_reader"));
        build();
        getWindow().getDecorView().postDelayed(this::welcome,500);
    }

    private void adaptOrientation(){
        boolean tablet=getResources().getConfiguration().smallestScreenWidthDp>=600;
        setRequestedOrientation(tablet?ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(12),dp(18),dp(18));root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(247,250,255));scroll.addView(root);

        TextView title=txt("Lumi Kids",32,true);title.setGravity(Gravity.CENTER);root.addView(title);

        lumi=new LumiView(this);
        int rh=getResources().getConfiguration().smallestScreenWidthDp>=600?dp(260):dp(210);
        root.addView(lumi,new LinearLayout.LayoutParams(-1,rh));
        lumi.setOnTickle(()->{
            lumi.setState(LumiView.State.HAPPY);
            voice.playLine("tickle",ui,profile(),()->lumi.setState(LumiView.State.IDLE));
        });

        TextView direction=txt(t("direction_help"),16,false);direction.setGravity(Gravity.CENTER);
        root.addView(direction);

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER);
        LinearLayout left=field(t("i_know"));knownSpinner=languageSpinner(db.getSetting("known","ru"));left.addView(knownSpinner);
        LinearLayout right=field(t("i_learn"));targetSpinner=languageSpinner(db.getSetting("target","en"));right.addView(targetSpinner);
        row.addView(left,new LinearLayout.LayoutParams(0,-2,1));TextView arrow=txt("→",30,true);arrow.setGravity(Gravity.CENTER);
        row.addView(arrow,new LinearLayout.LayoutParams(dp(50),dp(76)));row.addView(right,new LinearLayout.LayoutParams(0,-2,1));
        root.addView(row,new LinearLayout.LayoutParams(-1,-2));

        root.addView(txt(t("mode_title"),17,true));
        LinearLayout modes=new LinearLayout(this);modes.setOrientation(LinearLayout.HORIZONTAL);
        Button noRead=button("🧸\n"+t("pre_reader"),!reader);
        Button read=button("📖\n"+t("reader"),reader);
        modes.addView(noRead,new LinearLayout.LayoutParams(0,dp(92),1));
        modes.addView(read,new LinearLayout.LayoutParams(0,dp(92),1));
        root.addView(modes);
        noRead.setOnClickListener(v->{reader=false;db.setSetting("mode","pre_reader");build();});
        read.setOnClickListener(v->{reader=true;db.setSetting("mode","reader");build();});

        TextView choose=txt(t("choose_topic"),19,true);choose.setGravity(Gravity.CENTER);choose.setPadding(0,dp(12),0,dp(8));
        root.addView(choose);

        String[][] topics={{"animals","🐶",t("animals")},{"colors","🎨",t("colors")},
                {"numbers","🔢",t("numbers")},{"family","👨‍👩‍👧",t("family")},
                {"food","🍎",t("food")},{"body","🖐",t("body")},
                {"review","🚀",t("word_adventure")},{"custom","⭐",t("practice_my_words")}};
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<topics.length;i+=2){
            LinearLayout rr=new LinearLayout(this);rr.setOrientation(LinearLayout.HORIZONTAL);
            for(int j=0;j<2;j++){
                int k=i+j;Button b=button(topics[k][1]+"\n"+topics[k][2],false);
                String id=topics[k][0];b.setOnClickListener(v->{if("custom".equals(id)&&db.getCustomWords().isEmpty())startActivity(new Intent(this,CustomWordsActivity.class));else startTopic(id);});
                rr.addView(b,new LinearLayout.LayoutParams(0,dp(86),1));
            }
            grid.addView(rr);
        }
        root.addView(grid,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout tools=new LinearLayout(this);tools.setOrientation(LinearLayout.HORIZONTAL);
        Button my=button("✏️ "+t("my_words"),false);Button about=button("ℹ️ "+t("about"),false);
        tools.addView(my,new LinearLayout.LayoutParams(0,dp(60),1));tools.addView(about,new LinearLayout.LayoutParams(0,dp(60),1));
        root.addView(tools);
        my.setOnClickListener(v->startActivity(new Intent(this,CustomWordsActivity.class)));
        about.setOnClickListener(v->startActivity(new Intent(this,AboutActivity.class)));

        root.addView(txt(t("settings"),17,true));
        LinearLayout settings=new LinearLayout(this);settings.setOrientation(LinearLayout.HORIZONTAL);
        uiSpinner=languageSpinner(ui);voiceSpinner=new Spinner(this);
        voiceSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{t("voice_friendly"),t("voice_calm")}));
        voiceSpinner.setSelection("calm".equals(db.getSetting("voice","friendly"))?1:0);
        settings.addView(uiSpinner,new LinearLayout.LayoutParams(0,dp(58),1));
        settings.addView(voiceSpinner,new LinearLayout.LayoutParams(0,dp(58),1));root.addView(settings);

        Button save=button(t("save_settings"),false);root.addView(save,new LinearLayout.LayoutParams(-1,dp(58)));
        save.setOnClickListener(v->{
            ui=code(uiSpinner.getSelectedItemPosition());db.setSetting("ui",ui);
            db.setSetting("voice",voiceSpinner.getSelectedItemPosition()==1?"calm":"friendly");
            Toast.makeText(this,t("saved"),Toast.LENGTH_SHORT).show();build();
        });

        TextView built=txt("Built by Albert D",12,false);built.setTextColor(Color.GRAY);built.setGravity(Gravity.CENTER);
        built.setPadding(0,dp(12),0,0);root.addView(built);
        setContentView(scroll);
    }

    private void welcome(){
        lumi.setState(LumiView.State.TALKING);
        voice.playLine("home_welcome",ui,profile(),()->lumi.setState(LumiView.State.IDLE));
    }

    private void startTopic(String topic){
        String known=code(knownSpinner.getSelectedItemPosition());
        String target=code(targetSpinner.getSelectedItemPosition());
        if(known.equals(target)){
            Toast.makeText(this,t("different_languages"),Toast.LENGTH_LONG).show();return;
        }
        db.setSetting("known",known);db.setSetting("target",target);
        Intent i=new Intent(this,GameActivity.class);
        i.putExtra("known",known);i.putExtra("target",target);i.putExtra("ui",ui);
        i.putExtra("reader",reader);i.putExtra("profile",profile());i.putExtra("topic",topic);
        startActivity(i);
    }

    private String profile(){return db.getSetting("voice","friendly");}

    private LinearLayout field(String label){
        LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);
        TextView t=txt(label,14,true);t.setGravity(Gravity.CENTER);l.addView(t);return l;
    }
    private Spinner languageSpinner(String selected){
        Spinner s=new Spinner(this);
        s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"English","Deutsch","Русский"}));
        s.setSelection(pos(selected));return s;
    }
    private int pos(String c){return"en".equals(c)?0:"de".equals(c)?1:2;}
    private String code(int p){return p==0?"en":p==1?"de":"ru";}

    private Button button(String text,boolean selected){
        Button b=new Button(this);b.setText(text);b.setTextSize(16);b.setAllCaps(false);
        b.setTextColor(selected?Color.WHITE:Color.rgb(17,47,89));
        b.setBackground(round(selected?Color.rgb(16,119,239):Color.WHITE));return b;
    }
    private android.graphics.drawable.GradientDrawable round(int c){
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(c);g.setCornerRadius(dp(20));g.setStroke(1,Color.rgb(215,228,243));return g;
    }
    private TextView txt(String s,int size,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(17,47,89));
        if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);t.setPadding(dp(6),dp(5),dp(6),dp(5));return t;
    }
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}

    private String t(String k){
        Map<String,String[]> m=new HashMap<>();
        m.put("direction_help",new String[]{"Choose what the child knows and learns","Wähle bekannte und neue Sprache","Выбери, какой язык ребёнок знает и какой учит"});
        m.put("i_know",new String[]{"I know","Ich kenne","Я знаю"});
        m.put("i_learn",new String[]{"I learn","Ich lerne","Я учу"});
        m.put("mode_title",new String[]{"Learning mode","Lernmodus","Режим обучения"});
        m.put("pre_reader",new String[]{"I don't read yet","Ich kann noch nicht lesen","Я ещё не умею читать"});
        m.put("reader",new String[]{"I can read","Ich kann lesen","Я умею читать"});
        m.put("choose_topic",new String[]{"Choose a game","Wähle ein Spiel","Выбери игру"});
        m.put("animals",new String[]{"Animals","Tiere","Животные"});
        m.put("colors",new String[]{"Colors","Farben","Цвета"});
        m.put("numbers",new String[]{"Numbers","Zahlen","Числа"});
        m.put("family",new String[]{"Family","Familie","Семья"});
        m.put("food",new String[]{"Food","Essen","Еда"});
        m.put("body",new String[]{"Body","Körper","Тело"});
        m.put("practice_my_words",new String[]{"My words","Meine Wörter","Мои слова"});
        m.put("word_adventure",new String[]{"1000-word adventure","1000-Wörter-Abenteuer","Приключение: 1000 слов"});
        m.put("my_words",new String[]{"My words","Meine Wörter","Мои слова"});
        m.put("about",new String[]{"About","Über die App","О приложении"});
        m.put("settings",new String[]{"Settings: interface / voice","Einstellungen: Sprache / Stimme","Настройки: интерфейс / голос"});
        m.put("voice_friendly",new String[]{"Friendly","Freundlich","Дружелюбный"});
        m.put("voice_calm",new String[]{"Calm","Ruhig","Спокойный"});
        m.put("save_settings",new String[]{"Save settings","Speichern","Сохранить настройки"});
        m.put("saved",new String[]{"Saved","Gespeichert","Сохранено"});
        m.put("different_languages",new String[]{"Choose two different languages","Wähle zwei verschiedene Sprachen","Выбери два разных языка"});
        String[] a=m.get(k);return a==null?k:a["en".equals(ui)?0:"de".equals(ui)?1:2];
    }

    @Override protected void onResume(){super.onResume();}
    @Override protected void onDestroy(){if(voice!=null)voice.release();if(db!=null)db.close();super.onDestroy();}
}
