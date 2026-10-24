package com.albertd.lumikidsv3;

public final class SrsScheduler {
    private static final long MIN = 60_000L;
    private static final long DAY = 24L * 60L * MIN;
    private static final long[] INTERVALS = {
            15L * MIN, DAY, 3L * DAY, 7L * DAY, 14L * DAY, 30L * DAY
    };

    public enum Format { PICTURE, TRANSLATION, PHRASE, MIXED, SPEAK }

    public static LumiDb.Progress answer(LumiDb.Progress p, boolean correct, Format format) {
        if (correct) {
            p.stage = Math.min(p.stage + 1, INTERVALS.length - 1);
            p.correct++;
        } else {
            p.stage = Math.max(0, p.stage - 2);
            p.wrong++;
        }
        p.dueAt = System.currentTimeMillis() + INTERVALS[Math.max(0, p.stage)];
        p.format = format.name();
        return p;
    }

    public static Format formatForStage(int stage) {
        if (stage <= 0) return Format.PICTURE;
        if (stage == 1) return Format.TRANSLATION;
        if (stage == 2) return Format.PICTURE;
        if (stage == 3) return Format.PHRASE;
        if (stage == 4) return Format.MIXED;
        return Format.SPEAK;
    }
}
