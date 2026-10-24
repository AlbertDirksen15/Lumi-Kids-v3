package com.albertd.lumikidsv3;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;
import java.io.*;

public final class BundledVoiceEngine implements VoiceEngine {
    private final Context context;
    private MediaPlayer player;

    public BundledVoiceEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override public void playLine(String key, String lang, String profile, Runnable onDone) {
        playAsset("voice/" + lang + "/" + key + ".wav", profile, false, onDone);
    }

    @Override public void playWord(Word word, String lang, String profile, boolean slow, Runnable onDone) {
        if (word.custom) {
            // Imported/custom words intentionally have no fake robotic TTS.
            if (onDone != null) onDone.run();
            return;
        }
        playAsset("voice/" + lang + "/word_" + word.id + ".wav", profile, slow, onDone);
    }

    private void playAsset(String assetPath, String profile, boolean slow, Runnable onDone) {
        stop();
        try {
            File out = new File(context.getCacheDir(), assetPath.replace('/','_'));
            if (!out.exists() || out.length()==0) {
                try (InputStream in=context.getAssets().open(assetPath);
                     OutputStream os=new FileOutputStream(out)) {
                    byte[] buf=new byte[16*1024]; int n;
                    while((n=in.read(buf))>0) os.write(buf,0,n);
                }
            }
            player = new MediaPlayer();
            player.setDataSource(out.getAbsolutePath());
            player.setOnPreparedListener(mp -> {
                if (Build.VERSION.SDK_INT >= 23) {
                    float speed = slow ? .78f : ("calm".equals(profile) ? .90f : 1.0f);
                    try { mp.setPlaybackParams(new PlaybackParams().setSpeed(speed).setPitch(1.02f)); }
                    catch (Exception ignored) {}
                }
                mp.start();
            });
            player.setOnCompletionListener(mp -> {
                mp.release(); if (player==mp) player=null;
                if(onDone!=null) onDone.run();
            });
            player.setOnErrorListener((mp,what,extra)->{
                try{mp.release();}catch(Exception ignored){}
                if(player==mp)player=null;
                if(onDone!=null)onDone.run();
                return true;
            });
            player.prepareAsync();
        } catch(Exception e) {
            if(onDone!=null)onDone.run();
        }
    }

    @Override public void stop() {
        if(player!=null) {
            try{player.stop();}catch(Exception ignored){}
            try{player.release();}catch(Exception ignored){}
            player=null;
        }
    }

    @Override public void release() { stop(); }
}
