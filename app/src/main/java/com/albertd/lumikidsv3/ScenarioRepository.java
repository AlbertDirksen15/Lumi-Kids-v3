package com.albertd.lumikidsv3;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ScenarioRepository {
    private final Context context;
    public ScenarioRepository(Context c){context=c.getApplicationContext();}

    public LessonScenario load(String id) {
        try(InputStream in=context.getAssets().open("scenarios/"+id+".json")) {
            String json=readAll(in);
            JSONObject o=new JSONObject(json);
            LessonScenario s=new LessonScenario();
            s.id=o.getString("id");
            readLocalized(o.getJSONObject("title"),s.title);
            readLocalized(o.getJSONObject("intro"),s.intro);
            readLocalized(o.getJSONObject("closing"),s.closing);
            JSONArray rs=o.getJSONArray("rounds");
            for(int i=0;i<rs.length();i++) {
                JSONObject r=rs.getJSONObject(i);
                LessonScenario.Round x=new LessonScenario.Round();
                x.word=r.getString("word");
                x.mode=GameView.Mode.valueOf(r.getString("mode"));
                JSONArray ds=r.optJSONArray("distractors");
                if(ds!=null)for(int j=0;j<ds.length();j++)x.distractors.add(ds.getString(j));
                readLocalized(r.getJSONObject("prompt"),x.prompt);
                readLocalized(r.getJSONObject("success"),x.success);
                readLocalized(r.getJSONObject("retry"),x.retry);
                if(r.has("joke"))readLocalized(r.getJSONObject("joke"),x.joke);
                readLocalized(r.getJSONObject("speechPrompt"),x.speechPrompt);
                readLocalized(r.getJSONObject("speechSuccess"),x.speechSuccess);
                s.rounds.add(x);
            }
            return s;
        }catch(Exception e){throw new IllegalStateException("Scenario "+id+" is invalid",e);}
    }

    private static void readLocalized(JSONObject o,Map<String,String> out) throws JSONException {
        for(String k:new String[]{"en","de","ru"}) if(o.has(k))out.put(k,o.getString(k));
    }

    private static String readAll(InputStream in)throws IOException{
        ByteArrayOutputStream b=new ByteArrayOutputStream();byte[]buf=new byte[8192];int n;
        while((n=in.read(buf))>0)b.write(buf,0,n);
        return b.toString(StandardCharsets.UTF_8.name());
    }
}
