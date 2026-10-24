package com.albertd.lumikidsv3;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DictionaryRepository {
    private final Context context;
    private final ArrayList<Word> builtIn = new ArrayList<>();
    private final HashMap<String, Word> byEnglish = new HashMap<>();

    public DictionaryRepository(Context context) {
        this.context = context.getApplicationContext();
        loadBuiltIn();
    }

    private void loadBuiltIn() {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                context.getAssets().open("dictionary.csv"), StandardCharsets.UTF_8))) {
            String line = r.readLine();
            while ((line = r.readLine()) != null) {
                List<String> a = parseCsv(line);
                if (a.size() < 6) continue;
                Word w = new Word(Integer.parseInt(a.get(0)), a.get(1), a.get(2), a.get(3),
                        a.get(4), a.get(5), false);
                builtIn.add(w);
                byEnglish.put(w.en.toLowerCase(Locale.ROOT), w);
            }
        } catch (Exception e) {
            throw new IllegalStateException("dictionary.csv could not be loaded", e);
        }
    }

    public List<Word> all(LumiDb db) {
        ArrayList<Word> out = new ArrayList<>(builtIn);
        out.addAll(db.getCustomWords());
        return out;
    }

    public Word byEnglish(String english, LumiDb db) {
        Word w = byEnglish.get(english.toLowerCase(Locale.ROOT));
        if (w != null) return w;
        for (Word c : db.getCustomWords()) {
            if (c.en.equalsIgnoreCase(english)) return c;
        }
        return null;
    }

    public List<Word> builtIn() { return Collections.unmodifiableList(builtIn); }

    private static List<String> parseCsv(String line) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    b.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                out.add(b.toString()); b.setLength(0);
            } else b.append(c);
        }
        out.add(b.toString());
        return out;
    }
}
