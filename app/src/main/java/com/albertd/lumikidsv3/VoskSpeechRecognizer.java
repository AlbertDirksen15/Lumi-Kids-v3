package com.albertd.lumikidsv3;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import java.io.File;
import java.text.Normalizer;
import java.util.Locale;
import java.util.concurrent.*;

public final class VoskSpeechRecognizer implements SpeechRecognizerEngine {
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Model model;
    private String modelLang="";
    private SpeechService service;
    private Recognizer recognizer;
    private String expected="";
    private String lastPartial="";
    private Callback callback;

    public VoskSpeechRecognizer(Context context) {
        this.context=context.getApplicationContext();
    }

    @Override public synchronized void listen(String lang, String expected, Callback callback) {
        stop();
        this.expected=expected;
        this.callback=callback;
        executor.execute(() -> {
            try {
                ensureModel(lang);
                String grammar = "[\"" + jsonEscape(normalize(expected)) + "\", \"[unk]\"]";
                Recognizer rec;
                try { rec = new Recognizer(model, 16000.0f, grammar); }
                catch(Exception e) { rec = new Recognizer(model,16000.0f); }
                rec.setMaxAlternatives(3);
                SpeechService ss = new SpeechService(rec,16000.0f);
                final Recognizer readyRec = rec;
                final SpeechService readyService = ss;
                main.post(() -> startService(readyRec,readyService));
            } catch(Exception e) {
                main.post(() -> {
                    if(callback!=null) callback.onError("Offline speech model: "+e.getMessage());
                });
            }
        });
    }

    private void ensureModel(String lang) throws Exception {
        synchronized(this) {
            if(model!=null && lang.equals(modelLang)) return;
            if(model!=null){try{model.close();}catch(Exception ignored){} model=null;}
        }
        File base=new File(context.getFilesDir(),"vosk/"+lang);
        AssetFolder.copy(context,"vosk/"+lang,base);
        Model m=new Model(base.getAbsolutePath());
        synchronized(this){model=m;modelLang=lang;}
    }

    private synchronized void startService(Recognizer rec, SpeechService ss) {
        recognizer=rec; service=ss; lastPartial="";
        if(callback!=null) callback.onReady();
        ss.startListening(new RecognitionListener() {
            @Override public void onPartialResult(String hypothesis) {
                String t=extract(hypothesis,"partial");
                if(!t.isEmpty()) lastPartial=t;
                if(callback!=null) callback.onPartial(t);
            }
            @Override public void onResult(String hypothesis) {
                String t=extractBest(hypothesis);
                if(!t.isEmpty()) finish(t);
            }
            @Override public void onFinalResult(String hypothesis) {
                String t=extractBest(hypothesis);
                finish(t.isEmpty()?lastPartial:t);
            }
            @Override public void onError(Exception e) {
                Callback cb=callback; cleanupService();
                if(cb!=null) cb.onError(e.getMessage());
            }
            @Override public void onTimeout() { finish(lastPartial); }
        }, 4200);
    }

    private synchronized void finish(String text) {
        if(service==null) return;
        float score=similarity(normalize(expected),normalize(text));
        boolean ok=score>=.64f || normalize(text).contains(normalize(expected));
        Callback cb=callback;
        cleanupService();
        if(cb!=null) cb.onResult(text,score,ok);
    }

    @Override public synchronized void stop() { cleanupService(); }

    private void cleanupService() {
        if(service!=null) {
            try{service.stop();}catch(Exception ignored){}
            try{service.shutdown();}catch(Exception ignored){}
            service=null;
        }
        if(recognizer!=null) {
            try{recognizer.close();}catch(Exception ignored){}
            recognizer=null;
        }
    }

    @Override public synchronized void release() {
        cleanupService();
        if(model!=null){try{model.close();}catch(Exception ignored){}model=null;}
        executor.shutdownNow();
    }

    private static String extract(String json,String key) {
        try{return new JSONObject(json).optString(key,"").trim();}catch(Exception e){return"";}
    }

    private static String extractBest(String json) {
        try {
            JSONObject o=new JSONObject(json);
            String t=o.optString("text","");
            if(!t.isEmpty()) return t.trim();
            if(o.has("alternatives") && o.getJSONArray("alternatives").length()>0)
                return o.getJSONArray("alternatives").getJSONObject(0).optString("text","").trim();
        }catch(Exception ignored){}
        return "";
    }

    static String normalize(String s) {
        if(s==null)return"";
        s=Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFKC);
        return s.replaceAll("[^\\p{L}\\p{N}]+"," ").trim().replaceAll("\\s+"," ");
    }

    static float similarity(String a,String b) {
        if(a.equals(b)) return 1f;
        if(a.isEmpty()||b.isEmpty()) return 0f;
        int[][] d=new int[a.length()+1][b.length()+1];
        for(int i=0;i<=a.length();i++)d[i][0]=i;
        for(int j=0;j<=b.length();j++)d[0][j]=j;
        for(int i=1;i<=a.length();i++)
            for(int j=1;j<=b.length();j++)
                d[i][j]=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),
                        d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
        return 1f-(float)d[a.length()][b.length()]/Math.max(a.length(),b.length());
    }

    private static String jsonEscape(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
}
