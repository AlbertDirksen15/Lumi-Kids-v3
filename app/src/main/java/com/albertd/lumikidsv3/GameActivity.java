package com.albertd.lumikidsv3;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class GameActivity extends Activity {
    private static final int REQ_MIC=701;
    private DictionaryRepository dict;
    private LumiDb db;
    private VoiceEngine voice;
    private SpeechRecognizerEngine speech;
    private LessonScenario scenario;
    private GameView game;
    private LumiView lumi;
    private TextView bubble, starsView, progressView;
    private String known,target,ui,profile,topic;
    private boolean readerMode;
    private int roundIndex=0,wrongTaps=0,speechAttempts=0,stars=0;
    private LessonScenario.Round current;
    private Word currentWord;
    private SrsScheduler.Format currentFormat;
    private boolean busy=false;
    private final ArrayList<LessonScenario.Round> sessionRounds=new ArrayList<>();

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        adaptOrientation();
        known=getIntent().getStringExtra("known"); if(known==null)known="ru";
        target=getIntent().getStringExtra("target"); if(target==null)target="en";
        ui=getIntent().getStringExtra("ui"); if(ui==null)ui="ru";
        profile=getIntent().getStringExtra("profile"); if(profile==null)profile="friendly";
        topic=getIntent().getStringExtra("topic"); if(topic==null)topic="animals";
        readerMode=getIntent().getBooleanExtra("reader",false);
        db=new LumiDb(this);dict=new DictionaryRepository(this);
        voice=new BundledVoiceEngine(this);speech=new VoskSpeechRecognizer(this);
        stars=db.getReward("stars");
        buildUi();
        if("custom".equals(topic)) buildCustomSession();
        else if("review".equals(topic)) buildVocabularySession();
        else {
            scenario=new ScenarioRepository(this).load(topic);
            buildScheduledSession();
        }
        playIntro();
    }

    private void adaptOrientation(){
        boolean tablet=getResources().getConfiguration().smallestScreenWidthDp>=600;
        setRequestedOrientation(tablet?
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    private void buildUi(){
        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(247,250,255));
        game=new GameView(this);
        game.setListener(new GameView.Listener(){
            @Override public void onCorrect(){GameActivity.this.onTapCorrect();}
            @Override public void onWrong(){GameActivity.this.onTapWrong();}
            @Override public void onObjectPosition(float x,float y){lumi.setLookAt(x*2-1,y*2-1);}
        });
        root.addView(game,new FrameLayout.LayoutParams(-1,-1));

        bubble=new TextView(this);
        bubble.setTextSize(20);bubble.setTextColor(Color.rgb(17,47,89));
        bubble.setGravity(Gravity.CENTER);bubble.setPadding(dp(18),dp(10),dp(18),dp(10));
        bubble.setBackground(roundBg(Color.WHITE,0x22000000));
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(74),Gravity.TOP);
        bp.setMargins(dp(16),dp(10),dp(16),0);root.addView(bubble,bp);

        starsView=new TextView(this);starsView.setTextSize(18);starsView.setTextColor(Color.rgb(17,47,89));
        starsView.setGravity(Gravity.CENTER);starsView.setBackground(roundBg(Color.WHITE,0x22000000));
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(dp(120),dp(50),Gravity.TOP|Gravity.RIGHT);
        sp.setMargins(0,dp(90),dp(16),0);root.addView(starsView,sp);

        progressView=new TextView(this);progressView.setTextSize(15);progressView.setTextColor(Color.DKGRAY);
        progressView.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(dp(150),dp(42),Gravity.TOP|Gravity.LEFT);
        pp.setMargins(dp(16),dp(94),0,0);root.addView(progressView,pp);

        lumi=new LumiView(this);
        lumi.setState(LumiView.State.IDLE);
        lumi.setOnTickle(()->{
            if(!busy){
                lumi.setState(LumiView.State.HAPPY);
                sayLine("tickle",ui,()->lumi.setState(LumiView.State.IDLE));
            }
        });
        int lw=getResources().getConfiguration().smallestScreenWidthDp>=600?dp(300):dp(190);
        int lh=getResources().getConfiguration().smallestScreenWidthDp>=600?dp(370):dp(235);
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(lw,lh,Gravity.BOTTOM|Gravity.LEFT);
        lp.setMargins(dp(8),0,0,dp(4));root.addView(lumi,lp);

        Button home=new Button(this);home.setText("⌂");home.setTextSize(21);home.setAllCaps(false);
        home.setBackground(roundBg(Color.WHITE,0x22000000));
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(dp(58),dp(50),Gravity.BOTTOM|Gravity.RIGHT);
        hp.setMargins(0,0,dp(12),dp(12));root.addView(home,hp);
        home.setOnClickListener(v->finish());

        setContentView(root);
        updateHud();
    }

    private void buildScheduledSession(){
        long now=System.currentTimeMillis();
        ArrayList<LessonScenario.Round> later=new ArrayList<>();
        for(LessonScenario.Round r:scenario.rounds){
            Word w=dict.byEnglish(r.word,db);if(w==null)continue;
            LumiDb.Progress p=db.getProgress(w.key(),known,target);
            if(p.stage<0||p.dueAt<=now)sessionRounds.add(r);else later.add(r);
        }
        if(sessionRounds.size()<4){
            for(LessonScenario.Round r:later){
                if(sessionRounds.size()>=6)break;
                sessionRounds.add(r);
            }
        }
        if(sessionRounds.isEmpty())sessionRounds.addAll(scenario.rounds);
    }

    private void buildCustomSession(){
        scenario=baseDynamicScenario("custom",
                "Поиграем с твоими словами!","Wir spielen mit deinen Wörtern!","Let's play with your words!",
                "Отличная работа!","Super gemacht!","Great job!");
        ArrayList<Word> custom=new ArrayList<>(db.getCustomWords());
        fillDynamicSession(custom,8);
    }

    private void buildVocabularySession(){
        scenario=baseDynamicScenario("review",
                "Отправляемся в приключение по тысяче слов!","Auf ins Abenteuer mit tausend Wörtern!","Let's explore a thousand words!",
                "Приключение продолжится в следующий раз!","Das Abenteuer geht nächstes Mal weiter!","The adventure continues next time!");
        ArrayList<Word> pool=new ArrayList<>(dict.builtIn());
        fillDynamicSession(pool,10);
    }

    private LessonScenario baseDynamicScenario(String id,String ruIntro,String deIntro,String enIntro,String ruClose,String deClose,String enClose){
        LessonScenario s=new LessonScenario();s.id=id;
        s.intro.put("ru",ruIntro);s.intro.put("de",deIntro);s.intro.put("en",enIntro);
        s.closing.put("ru",ruClose);s.closing.put("de",deClose);s.closing.put("en",enClose);
        return s;
    }

    private void fillDynamicSession(ArrayList<Word> pool,int limit){
        long now=System.currentTimeMillis();
        ArrayList<Word> due=new ArrayList<>(),fresh=new ArrayList<>(),later=new ArrayList<>();
        for(Word w:pool){
            LumiDb.Progress p=db.getProgress(w.key(),known,target);
            if(p.stage<0)fresh.add(w);else if(p.dueAt<=now)due.add(w);else later.add(w);
        }
        Comparator<Word> byDue=Comparator.comparingLong(w->db.getProgress(w.key(),known,target).dueAt);
        due.sort(byDue);Collections.shuffle(fresh,new Random());Collections.shuffle(later,new Random());
        ArrayList<Word> chosen=new ArrayList<>();
        for(Word w:due){if(chosen.size()>=limit)break;chosen.add(w);}
        for(Word w:fresh){if(chosen.size()>=limit)break;chosen.add(w);}
        for(Word w:later){if(chosen.size()>=limit)break;chosen.add(w);}
        Random r=new Random();
        for(int i=0;i<chosen.size();i++){
            Word w=chosen.get(i);LessonScenario.Round x=new LessonScenario.Round();x.word=w.en;
            x.mode=(i%3==2)?GameView.Mode.MEMORY:GameView.Mode.MOVING_TAP;
            ArrayList<Word> options=new ArrayList<>(pool);Collections.shuffle(options,r);
            for(Word d:options){if(x.distractors.size()>=3)break;if(!d.en.equalsIgnoreCase(w.en))x.distractors.add(d.en);}
            x.prompt.put("ru","Найди: "+w.ru);x.prompt.put("de","Finde: "+w.de);x.prompt.put("en","Find: "+w.en);
            x.success.put("ru","Да! Отлично!");x.success.put("de","Ja! Super!");x.success.put("en","Yes! Great job!");
            x.retry.put("ru","Ой! Посмотри ещё раз.");x.retry.put("de","Hoppla! Schau noch einmal.");x.retry.put("en","Oops! Look again.");
            x.joke.put("ru","У Lumi от радости мигает сердечко!");x.joke.put("de","Lumis Herz blinkt vor Freude!");x.joke.put("en","Lumi's heart is blinking with joy!");
            x.speechPrompt.put("ru","Теперь повтори слово.");x.speechPrompt.put("de","Jetzt sag das Wort.");x.speechPrompt.put("en","Now say the word.");
            x.speechSuccess.put("ru","Слышу! Здорово!");x.speechSuccess.put("de","Ich höre dich! Klasse!");x.speechSuccess.put("en","I hear you! Awesome!");
            sessionRounds.add(x);
        }
    }

    private void playIntro(){
        busy=true;lumi.setState(LumiView.State.TALKING);
        setBubble(LessonScenario.text(scenario.intro,ui));
        String key=scenario.id+"_intro";
        sayLine(key,ui,()->{
            lumi.setState(LumiView.State.IDLE);busy=false;
            startRound();
        });
    }

    private void startRound(){
        if(roundIndex>=sessionRounds.size()){finishLesson();return;}
        current=sessionRounds.get(roundIndex);
        currentWord=dict.byEnglish(current.word,db);
        if(currentWord==null){roundIndex++;startRound();return;}
        wrongTaps=0;speechAttempts=0;busy=true;
        LumiDb.Progress p=db.getProgress(currentWord.key(),known,target);
        currentFormat=SrsScheduler.formatForStage(p.stage);

        ArrayList<Word> dis=new ArrayList<>();
        for(String e:current.distractors){Word w=dict.byEnglish(e,db);if(w!=null)dis.add(w);}
        GameView.Mode mode=current.mode;
        if(currentFormat==SrsScheduler.Format.SPEAK && hasMic()) {
            setBubble(readerMode?LessonScenario.text(current.speechPrompt,target):"🎤");
            askSpeech();
            return;
        }
        if(currentFormat==SrsScheduler.Format.PHRASE && mode==GameView.Mode.MOVING_TAP)
            mode=GameView.Mode.MEMORY;
        game.setRound(mode,currentWord,dis,target,readerMode);
        float[] pos=game.correctObjectCenter();lumi.setLookAt(pos[0]*2-1,pos[1]*2-1);
        setBubble(readerMode?(currentFormat==SrsScheduler.Format.PHRASE?phraseFor(currentWord,target):LessonScenario.text(current.prompt,target)):"🔊  ✨");
        lumi.setState(LumiView.State.TALKING);
        sayLine(roundKey("prompt"),target,()->{
            lumi.setState(LumiView.State.IDLE);busy=false;
        });
        updateHud();
    }

    private void onTapCorrect(){
        if(busy)return;busy=true;
        lumi.setState(LumiView.State.HAPPY);stars++;db.addReward("stars",1);updateHud();
        setBubble(readerMode?LessonScenario.text(current.success,target):"⭐");
        sayLine(roundKey("success"),target,()->{
            if(hasMic()||android.os.Build.VERSION.SDK_INT>=23){
                askSpeech();
            } else completeRound(true);
        });
    }

    private void onTapWrong(){
        if(busy)return;
        wrongTaps++;
        lumi.setState(wrongTaps>=2?LumiView.State.THINKING:LumiView.State.WRONG);
        if(wrongTaps>=2){
            float[] p=game.correctObjectCenter();lumi.setLookAt(p[0]*2-1,p[1]*2-1);
        }
        busy=true;setBubble(readerMode?LessonScenario.text(current.retry,known):"🤔");
        sayLine(roundKey("retry"),known,()->{
            lumi.setState(wrongTaps>=2?LumiView.State.THINKING:LumiView.State.IDLE);busy=false;
        });
    }

    private void askSpeech(){
        busy=true;
        setBubble(readerMode?LessonScenario.text(current.speechPrompt,target):"🎤");
        lumi.setState(LumiView.State.TALKING);
        sayLine(roundKey("speech_prompt"),target,()->{
            voice.playWord(currentWord,target,profile,false,()->{
                if(checkSelfPermissionCompat()==PackageManager.PERMISSION_GRANTED) beginListening();
                else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);
            });
        });
    }

    private void beginListening(){
        lumi.setState(LumiView.State.LISTENING);
        setBubble(readerMode?"🎤  "+currentWord.value(target):"🎤");
        speech.listen(target,currentWord.value(target),new SpeechRecognizerEngine.Callback(){
            @Override public void onReady(){}
            @Override public void onPartial(String text){}
            @Override public void onResult(String text,float sim,boolean accepted){
                if(accepted){
                    stars+=2;db.addReward("stars",2);updateHud();
                    lumi.setState(LumiView.State.CELEBRATE);
                    setBubble(readerMode?LessonScenario.text(current.speechSuccess,target):"🌟🌟🌟");
                    sayLine(roundKey("speech_success"),target,()->completeRound(true));
                }else{
                    speechAttempts++;
                    lumi.setState(LumiView.State.WRONG);
                    if(speechAttempts<2){
                        setBubble(readerMode?LessonScenario.text(current.retry,known):"🙂 🔁");
                        sayLine(roundKey("retry"),known,()->
                                voice.playWord(currentWord,target,profile,true,GameActivity.this::beginListening));
                    }else completeRound(false);
                }
            }
            @Override public void onError(String message){completeRound(true);}
        });
    }

    private void completeRound(boolean success){
        LumiDb.Progress p=db.getProgress(currentWord.key(),known,target);
        db.saveProgress(currentWord.key(),known,target,SrsScheduler.answer(p,success,currentFormat));
        int finishedIndex=roundIndex;
        roundIndex++;
        String joke=LessonScenario.text(current.joke,known);
        if(success && finishedIndex%2==1 && !joke.isEmpty()){
            busy=true;lumi.setState(LumiView.State.TALKING);setBubble(readerMode?joke:"😄");
            sayLine(roundKey("joke"),known,()->{
                busy=false;lumi.setState(LumiView.State.IDLE);
                getWindow().getDecorView().postDelayed(this::startRound,350);
            });
        }else{
            busy=false;lumi.setState(LumiView.State.IDLE);
            getWindow().getDecorView().postDelayed(this::startRound,420);
        }
    }

    private void finishLesson(){
        busy=true;lumi.setState(LumiView.State.CELEBRATE);
        setBubble(LessonScenario.text(scenario.closing,ui));
        sayLine(scenario.id+"_closing",ui,()->{
            new AlertDialog.Builder(this)
                    .setTitle("Lumi Kids")
                    .setMessage(LessonScenario.text(scenario.closing,ui)+"\n⭐ "+stars)
                    .setPositiveButton("OK",(d,w)->finish())
                    .show();
        });
    }

    private void sayLine(String key,String lang,Runnable done){
        voice.playLine(key,lang,profile,done);
    }
    private String roundKey(String part){
        if("custom".equals(scenario.id)||"review".equals(scenario.id)) return "generic_"+part;
        return scenario.id+"_r"+(scenario.rounds.indexOf(current)+1)+"_"+part;
    }

    private boolean hasMic(){return checkSelfPermissionCompat()==PackageManager.PERMISSION_GRANTED;}
    private int checkSelfPermissionCompat(){
        if(android.os.Build.VERSION.SDK_INT<23)return PackageManager.PERMISSION_GRANTED;
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO);
    }

    @Override public void onRequestPermissionsResult(int req,String[] ps,int[] gs){
        super.onRequestPermissionsResult(req,ps,gs);
        if(req==REQ_MIC){
            if(gs.length>0&&gs[0]==PackageManager.PERMISSION_GRANTED)beginListening();
            else completeRound(true);
        }
    }

    private String phraseFor(Word w,String lang){
        if("de".equals(lang)) return "Das ist: "+w.de+".";
        if("ru".equals(lang)) return "Это: "+w.ru+".";
        return "This is: "+w.en+".";
    }

    private void setBubble(String text){bubble.setText(text);}
    private void updateHud(){starsView.setText("⭐ "+stars);progressView.setText((roundIndex+1)+" / "+Math.max(1,sessionRounds.size()));}

    private android.graphics.drawable.GradientDrawable roundBg(int color,int stroke){
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(color);g.setCornerRadius(dp(22));g.setStroke(1,stroke);return g;
    }
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}

    @Override protected void onDestroy(){
        if(voice!=null)voice.release();if(speech!=null)speech.release();if(db!=null)db.close();
        super.onDestroy();
    }
}
