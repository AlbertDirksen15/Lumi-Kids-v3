package com.albertd.lumikidsv3;

public final class Word {
    public final int id;
    public final String en, de, ru, category, emoji;
    public final boolean custom;

    public Word(int id, String en, String de, String ru, String category, String emoji, boolean custom) {
        this.id = id;
        this.en = en;
        this.de = de;
        this.ru = ru;
        this.category = category;
        this.emoji = (emoji == null || emoji.isEmpty()) ? "✨" : emoji;
        this.custom = custom;
    }

    public String value(String lang) {
        if ("de".equals(lang)) return de;
        if ("ru".equals(lang)) return ru;
        return en;
    }

    public String key() {
        return custom ? "c" + Math.abs(id) : "w" + id;
    }
}
