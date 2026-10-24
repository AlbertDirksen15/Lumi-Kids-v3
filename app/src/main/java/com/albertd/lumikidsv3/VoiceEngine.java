package com.albertd.lumikidsv3;

public interface VoiceEngine {
    void playLine(String key, String lang, String profile, Runnable onDone);
    void playWord(Word word, String lang, String profile, boolean slow, Runnable onDone);
    void stop();
    void release();
}
