package com.albertd.lumikidsv3;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public final class LumiView extends View {
    public enum State { IDLE, TALKING, LISTENING, HAPPY, THINKING, WRONG, CELEBRATE }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private State state = State.IDLE;
    private long stateSince = SystemClock.uptimeMillis();
    private float lookX = 0f, lookY = 0f;
    private Runnable tickleListener;
    private final Random random = new Random();
    private final ArrayList<Confetti> confetti = new ArrayList<>();

    private static final int NAVY = Color.rgb(17, 47, 89);
    private static final int BLUE = Color.rgb(16, 119, 239);
    private static final int CYAN = Color.rgb(37, 220, 255);
    private static final int CREAM = Color.rgb(253, 245, 226);
    private static final int PINK = Color.rgb(255, 111, 132);

    public LumiView(Context c) { super(c); init(); }
    public LumiView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setState(State s) {
        state = s;
        stateSince = SystemClock.uptimeMillis();
        if (s == State.CELEBRATE) spawnConfetti();
        invalidate();
    }

    public State getState() { return state; }

    public void setLookAt(float normalizedX, float normalizedY) {
        lookX = clamp(normalizedX, -1f, 1f);
        lookY = clamp(normalizedY, -1f, 1f);
        invalidate();
    }

    public void setOnTickle(Runnable r) { tickleListener = r; }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now = SystemClock.uptimeMillis();
        float t = (now - stateSince) / 1000f;
        float w = getWidth(), h = getHeight();
        float scale = Math.min(w / 420f, h / 520f);
        float cx = w * 0.5f, cy = h * 0.56f;

        float bob = (float)Math.sin(now / 420.0) * 4f * scale;
        float jump = 0f;
        float tilt = 0f;
        float armWave = 0f;
        float mouth = 0.28f;

        switch (state) {
            case TALKING:
                bob += (float)Math.sin(now / 130.0) * 3f * scale;
                tilt = (float)Math.sin(now / 360.0) * 3f;
                armWave = (float)Math.sin(now / 190.0) * 15f;
                mouth = 0.35f + 0.45f * Math.abs((float)Math.sin(now / 110.0));
                break;
            case LISTENING:
                tilt = -7f;
                bob -= 4f * scale;
                mouth = 0.12f;
                break;
            case HAPPY:
                jump = -18f * scale * Math.abs((float)Math.sin(Math.min(t, 1f) * Math.PI));
                tilt = (float)Math.sin(now / 150.0) * 4f;
                mouth = 0.9f;
                break;
            case THINKING:
                tilt = -9f;
                mouth = 0.18f;
                break;
            case WRONG:
                tilt = (float)Math.sin(now / 80.0) * 5f;
                mouth = 0.2f;
                break;
            case CELEBRATE:
                jump = -24f * scale * Math.abs((float)Math.sin(now / 180.0));
                tilt = (float)Math.sin(now / 120.0) * 7f;
                armWave = 25f;
                mouth = 1f;
                drawConfetti(c, now, scale);
                break;
            default:
                mouth = 0.28f;
        }

        c.save();
        c.translate(cx, cy + bob + jump);
        c.rotate(tilt);
        c.scale(scale, scale);

        drawLeg(c, -70, 110, -82, 190, -4f);
        drawLeg(c, 70, 110, 82, 190, 4f);

        // body
        p.setStyle(Paint.Style.FILL); p.setColor(CREAM);
        c.drawRoundRect(-105, 15, 105, 170, 55, 55, p);
        p.setColor(BLUE); c.drawRoundRect(-105, 120, 105, 152, 16, 16, p);
        p.setColor(NAVY); c.drawRoundRect(-32, 0, 32, 38, 14, 14, p);

        // glowing heart
        p.setShadowLayer(16, 0, 0, CYAN); p.setColor(CYAN);
        Path heart = new Path();
        heart.moveTo(0, 115);
        heart.cubicTo(-55, 80, -54, 38, -16, 48);
        heart.cubicTo(0, 52, 0, 70, 0, 70);
        heart.cubicTo(0, 70, 0, 52, 16, 48);
        heart.cubicTo(54, 38, 55, 80, 0, 115);
        c.drawPath(heart, p);
        p.clearShadowLayer();

        drawArm(c, -93, 70, -160, 90, state == State.CELEBRATE ? -60f : -8f + armWave, false);
        drawArm(c, 93, 70, 160, 90,
                state == State.THINKING ? -75f : (state == State.CELEBRATE ? 60f : 8f - armWave), true);

        // head/face
        p.setColor(CREAM);
        c.drawRoundRect(-155, -165, 155, 30, 70, 70, p);
        p.setColor(BLUE);
        c.drawRoundRect(-88, -185, 88, -147, 25, 25, p);
        c.drawOval(-178, -108, -142, -22, p);
        c.drawOval(142, -108, 178, -22, p);
        p.setColor(NAVY);
        c.drawRoundRect(-128, -135, 128, 4, 48, 48, p);

        // antenna
        p.setStrokeWidth(13); p.setColor(NAVY);
        c.drawLine(108, -150, 136, -205, p);
        p.setShadowLayer(state == State.LISTENING ? 24 : 10, 0, 0, CYAN);
        p.setColor(CYAN); c.drawCircle(143, -218, state == State.LISTENING ? 23 : 19, p);
        p.clearShadowLayer();

        drawEyes(c, now, state == State.WRONG || state == State.THINKING);
        drawMouth(c, mouth);

        // eyebrows communicate state
        p.setStrokeWidth(9); p.setColor(CYAN);
        if (state == State.WRONG) {
            c.drawLine(-78, -114, -37, -123, p);
            c.drawLine(37, -123, 78, -114, p);
        } else if (state == State.THINKING) {
            c.drawArc(-83, -128, -34, -98, 190, 120, false, p);
            c.drawArc(34, -128, 83, -98, 230, 120, false, p);
        } else {
            c.drawArc(-83, -128, -34, -98, 200, 100, false, p);
            c.drawArc(34, -128, 83, -98, 200, 100, false, p);
        }

        // thinking hand/finger near cheek
        if (state == State.THINKING) {
            p.setColor(BLUE); c.drawCircle(117, -52, 13, p);
            p.setColor(CREAM); c.drawCircle(102, -35, 22, p);
        }

        c.restore();

        // keep frame updates cheap, only while attached
        if (isShown()) postInvalidateOnAnimation();
    }

    private void drawEyes(Canvas c, long now, boolean worried) {
        float blinkPhase = (now % 4300L) / 4300f;
        float blink = (blinkPhase > 0.94f) ? Math.abs((blinkPhase - 0.97f) / 0.03f) : 1f;
        blink = clamp(blink, 0.08f, 1f);
        drawEye(c, -62, -65, blink, worried);
        drawEye(c, 62, -65, blink, worried);
    }

    private void drawEye(Canvas c, float x, float y, float blink, boolean worried) {
        p.setColor(Color.WHITE);
        c.save();
        c.scale(1f, blink, x, y);
        c.drawOval(x - 39, y - 46, x + 39, y + 46, p);
        float px = x + lookX * 12f, py = y + lookY * 8f;
        p.setColor(CYAN); c.drawCircle(px, py + 7, 25, p);
        p.setColor(Color.rgb(1, 42, 88)); c.drawCircle(px, py + 5, 15, p);
        p.setColor(Color.WHITE); c.drawCircle(px + 7, py - 4, 6, p);
        c.restore();
    }

    private void drawMouth(Canvas c, float openness) {
        float hh = 11 + 23 * openness;
        p.setColor(Color.rgb(74, 15, 54));
        c.drawRoundRect(-42, -27, 42, -27 + hh * 2, 20, 20, p);
        if (openness > .42f) {
            p.setColor(PINK);
            c.drawOval(-25, -3 + hh * .25f, 25, 13 + hh * .7f, p);
        }
    }

    private void drawArm(Canvas c, float sx, float sy, float ex, float ey, float angle, boolean right) {
        c.save();
        c.rotate(angle, sx, sy);
        p.setStrokeWidth(30); p.setColor(CREAM); c.drawLine(sx, sy, ex, ey, p);
        p.setStrokeWidth(10); p.setColor(NAVY);
        c.drawCircle((sx + ex) / 2f, (sy + ey) / 2f, 13, p);
        p.setColor(BLUE); c.drawCircle(ex, ey, 31, p);
        // fingers
        p.setStrokeWidth(11);
        for (int i = -1; i <= 1; i++) {
            c.drawLine(ex + i * 10, ey, ex + i * 12, ey - 27, p);
        }
        c.restore();
    }

    private void drawLeg(Canvas c, float sx, float sy, float ex, float ey, float angle) {
        c.save(); c.rotate(angle, sx, sy);
        p.setStrokeWidth(34); p.setColor(NAVY); c.drawLine(sx, sy, ex, ey - 17, p);
        p.setColor(CREAM); c.drawRoundRect(ex - 43, ey - 20, ex + 48, ey + 45, 35, 35, p);
        p.setColor(BLUE); c.drawRoundRect(ex - 46, ey + 23, ex + 51, ey + 49, 13, 13, p);
        c.restore();
    }

    private void spawnConfetti() {
        confetti.clear();
        for (int i = 0; i < 36; i++) {
            Confetti q = new Confetti();
            q.x = random.nextFloat();
            q.y = -random.nextFloat() * .25f;
            q.v = .22f + random.nextFloat() * .38f;
            q.phase = random.nextFloat() * 8f;
            int[] cs = {BLUE, CYAN, Color.rgb(255,192,50), PINK, Color.rgb(95,210,120)};
            q.color = cs[random.nextInt(cs.length)];
            confetti.add(q);
        }
    }

    private void drawConfetti(Canvas c, long now, float scale) {
        float dt = (now - stateSince) / 1000f;
        p.setStyle(Paint.Style.FILL);
        for (Confetti q : confetti) {
            float x = getWidth() * q.x + (float)Math.sin(dt * 4 + q.phase) * 28 * scale;
            float y = getHeight() * (q.y + dt * q.v);
            p.setColor(q.color);
            c.save(); c.rotate(dt * 240 + q.phase * 20, x, y);
            c.drawRect(x - 6*scale, y - 10*scale, x + 6*scale, y + 10*scale, p);
            c.restore();
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            setState(State.HAPPY);
            if (tickleListener != null) tickleListener.run();
            postDelayed(() -> { if (state == State.HAPPY) setState(State.IDLE); }, 900);
            performClick();
            return true;
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }
    private static final class Confetti { float x,y,v,phase; int color; }
}
