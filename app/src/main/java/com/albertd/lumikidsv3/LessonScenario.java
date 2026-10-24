package com.albertd.lumikidsv3;

import java.util.*;

public final class LessonScenario {
    public String id;
    public final Map<String,String> title = new HashMap<>();
    public final Map<String,String> intro = new HashMap<>();
    public final Map<String,String> closing = new HashMap<>();
    public final ArrayList<Round> rounds = new ArrayList<>();

    public static final class Round {
        public String word;
        public GameView.Mode mode;
        public final ArrayList<String> distractors = new ArrayList<>();
        public final Map<String,String> prompt = new HashMap<>();
        public final Map<String,String> success = new HashMap<>();
        public final Map<String,String> retry = new HashMap<>();
        public final Map<String,String> joke = new HashMap<>();
        public final Map<String,String> speechPrompt = new HashMap<>();
        public final Map<String,String> speechSuccess = new HashMap<>();
    }

    public static String text(Map<String,String> m, String lang) {
        String s=m.get(lang);
        if(s==null)s=m.get("en");
        return s==null?"":s;
    }
}
