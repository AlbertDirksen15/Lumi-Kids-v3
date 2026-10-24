package com.albertd.lumikidsv3;

public interface SpeechRecognizerEngine {
    interface Callback {
        void onReady();
        void onPartial(String text);
        void onResult(String text, float similarity, boolean accepted);
        void onError(String message);
    }
    void listen(String lang, String expected, Callback callback);
    void stop();
    void release();
}
