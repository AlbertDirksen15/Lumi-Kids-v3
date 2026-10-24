package com.albertd.lumikidsv3;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

public final class LumiDb extends SQLiteOpenHelper {
    private static final String DB = "lumi_kids_v3.db";
    private static final int VERSION = 1;

    public LumiDb(Context context) {
        super(context, DB, null, VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE progress (" +
                "word_key TEXT NOT NULL, known_lang TEXT NOT NULL, target_lang TEXT NOT NULL," +
                "stage INTEGER NOT NULL DEFAULT -1, due_at INTEGER NOT NULL DEFAULT 0," +
                "correct_count INTEGER NOT NULL DEFAULT 0, wrong_count INTEGER NOT NULL DEFAULT 0," +
                "last_format TEXT, PRIMARY KEY(word_key,known_lang,target_lang))");
        db.execSQL("CREATE TABLE custom_words (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, en TEXT NOT NULL, de TEXT NOT NULL, ru TEXT NOT NULL," +
                "emoji TEXT NOT NULL DEFAULT '✨', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE settings (k TEXT PRIMARY KEY, v TEXT NOT NULL)");
        db.execSQL("CREATE TABLE rewards (k TEXT PRIMARY KEY, v INTEGER NOT NULL DEFAULT 0)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public static final class Progress {
        public int stage = -1, correct = 0, wrong = 0;
        public long dueAt = 0;
        public String format = "";
    }

    public Progress getProgress(String wordKey, String known, String target) {
        Progress p = new Progress();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT stage,due_at,correct_count,wrong_count,last_format FROM progress " +
                        "WHERE word_key=? AND known_lang=? AND target_lang=?",
                new String[]{wordKey, known, target})) {
            if (c.moveToFirst()) {
                p.stage = c.getInt(0); p.dueAt = c.getLong(1);
                p.correct = c.getInt(2); p.wrong = c.getInt(3);
                p.format = c.isNull(4) ? "" : c.getString(4);
            }
        }
        return p;
    }

    public void saveProgress(String wordKey, String known, String target, Progress p) {
        ContentValues v = new ContentValues();
        v.put("word_key", wordKey); v.put("known_lang", known); v.put("target_lang", target);
        v.put("stage", p.stage); v.put("due_at", p.dueAt);
        v.put("correct_count", p.correct); v.put("wrong_count", p.wrong);
        v.put("last_format", p.format);
        getWritableDatabase().insertWithOnConflict("progress", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long addCustomWord(String en, String de, String ru, String emoji) {
        ContentValues v = new ContentValues();
        v.put("en", en.trim()); v.put("de", de.trim()); v.put("ru", ru.trim());
        v.put("emoji", emoji == null || emoji.trim().isEmpty() ? "✨" : emoji.trim());
        v.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("custom_words", null, v);
    }

    public List<Word> getCustomWords() {
        ArrayList<Word> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id,en,de,ru,emoji FROM custom_words ORDER BY id DESC", null)) {
            while (c.moveToNext()) {
                out.add(new Word(-c.getInt(0), c.getString(1), c.getString(2), c.getString(3),
                        "custom", c.getString(4), true));
            }
        }
        return out;
    }

    public void deleteCustomWord(int negativeId) {
        getWritableDatabase().delete("custom_words", "id=?",
                new String[]{String.valueOf(Math.abs(negativeId))});
    }

    public String getSetting(String key, String def) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT v FROM settings WHERE k=?", new String[]{key})) {
            return c.moveToFirst() ? c.getString(0) : def;
        }
    }

    public void setSetting(String key, String value) {
        ContentValues v = new ContentValues(); v.put("k", key); v.put("v", value);
        getWritableDatabase().insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getReward(String key) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT v FROM rewards WHERE k=?", new String[]{key})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public void addReward(String key, int amount) {
        int n = getReward(key) + amount;
        ContentValues v = new ContentValues(); v.put("k", key); v.put("v", n);
        getWritableDatabase().insertWithOnConflict("rewards", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
