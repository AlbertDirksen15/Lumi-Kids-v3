package com.albertd.lumikidsv3;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public final class GameView extends View {
    public enum Mode { MOVING_TAP, COLOR_TAP, COUNT, MEMORY, DRAG_FEED, BODY_TAP, SORT, PUZZLE }
    public interface Listener {
        void onCorrect();
        void onWrong();
        void onObjectPosition(float normalizedX, float normalizedY);
    }

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final ArrayList<Obj> objects = new ArrayList<>();
    private Listener listener;
    private Mode mode = Mode.MOVING_TAP;
    private Word target;
    private String targetLang = "en";
    private boolean readerMode = false;
    private long roundStarted;
    private Obj dragging;
    private Obj firstMemory;
    private int memoryMatched = 0;
    private final RectF feedZone = new RectF();
    private final RectF sortLeft = new RectF(), sortRight = new RectF();

    public GameView(Context c) {
        super(c);
        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        setBackgroundColor(Color.rgb(247,250,255));
        setFocusable(true);
    }

    public void setListener(Listener listener) { this.listener = listener; }

    public void setRound(Mode mode, Word target, List<Word> distractors, String targetLang, boolean readerMode) {
        this.mode = mode;
        this.target = target;
        this.targetLang = targetLang;
        this.readerMode = readerMode;
        this.roundStarted = SystemClock.uptimeMillis();
        this.dragging = null;
        this.firstMemory = null;
        this.memoryMatched = 0;
        objects.clear();

        if (mode == Mode.PUZZLE) {
            setupPuzzle(target);
        } else if (mode == Mode.COUNT) {
            int wanted = numberValue(target.en);
            int[] opts = uniqueCounts(wanted);
            for (int i=0;i<opts.length;i++) {
                Obj o = new Obj();
                o.count = opts[i];
                o.correct = opts[i] == wanted;
                o.label = String.valueOf(opts[i]);
                o.emoji = "⭐";
                o.x = .18f + (i%2)*.46f;
                o.y = .25f + (i/2)*.34f;
                o.radius = .14f;
                objects.add(o);
            }
        } else if (mode == Mode.BODY_TAP) {
            setupBody(target);
        } else if (mode == Mode.MEMORY) {
            Word d = distractors.isEmpty() ? target : distractors.get(0);
            for (int i=0;i<2;i++) addMemory(target, true, 1);
            for (int i=0;i<2;i++) addMemory(d, false, 2);
            Collections.shuffle(objects, random);
            layoutGrid(objects);
        } else {
            Obj correct = fromWord(target, true);
            objects.add(correct);
            int n = Math.min(3, distractors.size());
            for (int i=0;i<n;i++) objects.add(fromWord(distractors.get(i), false));
            while (objects.size()<4) {
                Obj o = fromWord(target, false);
                o.label = "?"; o.emoji = "❓";
                objects.add(o);
            }
            layoutGrid(objects);
            if (mode == Mode.MOVING_TAP) {
                for (Obj o : objects) {
                    o.vx = (.05f + random.nextFloat()*.08f) * (random.nextBoolean()?1:-1);
                    o.vy = (.04f + random.nextFloat()*.06f) * (random.nextBoolean()?1:-1);
                }
            }
        }
        invalidate();
    }

    public float[] correctObjectCenter() {
        for (Obj o : objects) if (o.correct && !o.hidden) return new float[]{o.x, o.y};
        return new float[]{.5f,.45f};
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now = SystemClock.uptimeMillis();
        float dt = Math.min(.032f, (now - roundStarted) / 1000f);
        if (mode == Mode.MOVING_TAP) updateMoving(.016f);

        drawSoftBackground(c);

        switch (mode) {
            case COUNT: drawCount(c); break;
            case BODY_TAP: drawBody(c); break;
            case MEMORY: drawMemory(c, now); break;
            case DRAG_FEED: drawFeed(c); drawObjects(c, false); break;
            case SORT: drawSort(c); drawObjects(c, false); break;
            case PUZZLE: drawPuzzle(c); break;
            default: drawObjects(c, mode == Mode.COLOR_TAP); break;
        }
        if (isShown()) postInvalidateOnAnimation();
    }

    private void drawSoftBackground(Canvas c) {
        p.setColor(Color.rgb(233,244,255));
        p.setStyle(Paint.Style.FILL);
        for (int i=0;i<8;i++) {
            float x = (i*137 % 997) / 997f * getWidth();
            float y = (i*257 % 733) / 733f * getHeight();
            c.drawCircle(x,y,12 + (i%3)*8,p);
        }
    }

    private void drawObjects(Canvas c, boolean colorMode) {
        for (Obj o : objects) {
            if (o.hidden) continue;
            float x=o.x*getWidth(), y=o.y*getHeight(), r=o.radius*Math.min(getWidth(),getHeight());
            p.setColor(colorMode ? colorFor(o.label) : Color.WHITE);
            p.setShadowLayer(10,0,4,Color.argb(40,0,60,130));
            c.drawCircle(x,y,r,p); p.clearShadowLayer();

            if (colorMode) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5);
                p.setColor(Color.argb(160,255,255,255)); c.drawCircle(x,y,r-4,p);
                p.setStyle(Paint.Style.FILL);
            }

            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(r*.78f);
            p.setColor(Color.rgb(23,52,91));
            c.drawText(o.emoji, x, y + r*.25f, p);
            if (readerMode && !colorMode) {
                p.setTextSize(Math.max(18, r*.24f));
                p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(o.word == null ? o.label : o.word.value(targetLang), x, y+r*.82f, p);
                p.setTypeface(Typeface.DEFAULT);
            }
        }
    }

    private void drawCount(Canvas c) {
        for (Obj o : objects) {
            float x=o.x*getWidth(), y=o.y*getHeight();
            float rw=getWidth()*.30f, rh=getHeight()*.25f;
            RectF card=new RectF(x-rw/2,y-rh/2,x+rw/2,y+rh/2);
            p.setColor(Color.WHITE); p.setShadowLayer(8,0,4,Color.argb(35,0,50,90));
            c.drawRoundRect(card,28,28,p); p.clearShadowLayer();
            p.setTextAlign(Paint.Align.CENTER); p.setColor(Color.rgb(20,65,110));
            p.setTextSize(Math.min(58,getHeight()*.07f));
            StringBuilder stars=new StringBuilder();
            for(int i=0;i<o.count;i++) stars.append("⭐");
            c.drawText(stars.toString(),x,y,p);
            if(readerMode) {
                p.setTextSize(Math.min(40,getHeight()*.05f)); p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText(o.label,x,y+rh*.34f,p); p.setTypeface(Typeface.DEFAULT);
            }
        }
    }

    private void drawFeed(Canvas c) {
        float w=getWidth(),h=getHeight();
        feedZone.set(w*.70f,h*.67f,w*.96f,h*.96f);
        p.setColor(Color.rgb(255,239,205)); c.drawOval(feedZone,p);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(Math.min(80,h*.12f));
        p.setColor(Color.rgb(80,55,35));
        c.drawText("😋",feedZone.centerX(),feedZone.centerY()+25,p);
    }

    private void drawSort(Canvas c) {
        float w=getWidth(),h=getHeight();
        sortLeft.set(w*.03f,h*.72f,w*.45f,h*.97f);
        sortRight.set(w*.55f,h*.72f,w*.97f,h*.97f);
        p.setColor(Color.rgb(255,226,226)); c.drawRoundRect(sortLeft,30,30,p);
        p.setColor(Color.rgb(220,237,255)); c.drawRoundRect(sortRight,30,30,p);
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(Math.min(54,h*.07f));
        p.setColor(Color.DKGRAY); c.drawText("🔴",sortLeft.centerX(),sortLeft.centerY()+18,p);
        c.drawText("🔵",sortRight.centerX(),sortRight.centerY()+18,p);
    }

    private void drawBody(Canvas c) {
        float w=getWidth(), h=getHeight();
        float cx=w*.52f, top=h*.12f;
        p.setColor(Color.rgb(255,224,190));
        c.drawCircle(cx,top+h*.10f,h*.07f,p);
        p.setStrokeWidth(h*.07f); p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(Color.rgb(73,157,236));
        c.drawLine(cx,top+h*.20f,cx,top+h*.48f,p);
        c.drawLine(cx,top+h*.28f,cx-w*.18f,top+h*.39f,p);
        c.drawLine(cx,top+h*.28f,cx+w*.18f,top+h*.39f,p);
        c.drawLine(cx,top+h*.48f,cx-w*.12f,top+h*.69f,p);
        c.drawLine(cx,top+h*.48f,cx+w*.12f,top+h*.69f,p);
        p.setStrokeCap(Paint.Cap.BUTT);

        for(Obj o:objects) {
            float x=o.x*w,y=o.y*h,r=o.radius*Math.min(w,h);
            p.setColor(Color.argb(45,12,120,230)); c.drawCircle(x,y,r,p);
            if(readerMode) {
                p.setTextAlign(Paint.Align.CENTER); p.setTextSize(Math.max(18,r*.55f));
                p.setColor(Color.rgb(20,55,90));
                c.drawText(o.word.value(targetLang),x,y-r*1.1f,p);
            }
        }
    }

    private void drawMemory(Canvas c, long now) {
        boolean reveal = now-roundStarted < 1400;
        for(Obj o:objects) {
            float x=o.x*getWidth(),y=o.y*getHeight();
            float rw=getWidth()*.31f,rh=getHeight()*.28f;
            RectF card=new RectF(x-rw/2,y-rh/2,x+rw/2,y+rh/2);
            p.setColor(o.matched?Color.rgb(221,250,228):Color.WHITE);
            c.drawRoundRect(card,28,28,p);
            boolean face = reveal || o.flipped || o.matched;
            p.setTextAlign(Paint.Align.CENTER); p.setTextSize(Math.min(76,rh*.42f));
            p.setColor(Color.rgb(20,55,90));
            c.drawText(face?o.emoji:"❔",x,y+20,p);
            if(face && readerMode) {
                p.setTextSize(Math.min(28,rh*.17f));
                c.drawText(o.word.value(targetLang),x,y+rh*.33f,p);
            }
        }
    }

    private void setupPuzzle(Word target){
        float[] starts={.22f,.50f,.78f};float[] slots={.28f,.50f,.72f};
        for(int i=0;i<3;i++){Obj o=fromWord(target,true);o.pairId=i;o.radius=.105f;
            o.x=o.homeX=starts[i];o.y=o.homeY=.76f;o.slotX=slots[i];o.slotY=.38f;objects.add(o);}
    }

    private void drawPuzzle(Canvas c){
        float m=Math.min(getWidth(),getHeight());p.setTextAlign(Paint.Align.CENTER);
        for(Obj o:objects){
            float sx=o.slotX*getWidth(),sy=o.slotY*getHeight(),r=o.radius*m;
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(Color.rgb(160,190,225));
            c.drawCircle(sx,sy,r,p);p.setStyle(Paint.Style.FILL);
            float x=o.x*getWidth(),y=o.y*getHeight();
            p.setColor(o.placed?Color.rgb(218,249,228):Color.WHITE);c.drawCircle(x,y,r,p);
            p.setTextSize(r*.85f);p.setColor(Color.rgb(20,55,90));c.drawText(o.emoji,x,y+r*.28f,p);
            p.setTextSize(Math.max(15,r*.22f));c.drawText(String.valueOf(o.pairId+1),x,y+r*.82f,p);
        }
    }

    private void updateMoving(float dt) {
        for(Obj o:objects) {
            if(o==dragging) continue;
            o.x += o.vx*dt; o.y += o.vy*dt;
            if(o.x<.13f){o.x=.13f;o.vx=Math.abs(o.vx);}
            if(o.x>.87f){o.x=.87f;o.vx=-Math.abs(o.vx);}
            if(o.y<.17f){o.y=.17f;o.vy=Math.abs(o.vy);}
            if(o.y>.78f){o.y=.78f;o.vy=-Math.abs(o.vy);}
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float nx=e.getX()/Math.max(1f,getWidth()), ny=e.getY()/Math.max(1f,getHeight());
        if(e.getAction()==MotionEvent.ACTION_DOWN) {
            dragging = findHit(nx,ny);
            if(mode==Mode.DRAG_FEED || mode==Mode.SORT || mode==Mode.PUZZLE) return true;
        }
        if(e.getAction()==MotionEvent.ACTION_MOVE && dragging!=null &&
                (mode==Mode.DRAG_FEED || mode==Mode.SORT || mode==Mode.PUZZLE)) {
            dragging.x=nx; dragging.y=ny; invalidate(); return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP) {
            Obj hit = dragging!=null?dragging:findHit(nx,ny);
            if(hit==null){dragging=null;return true;}
            if(mode==Mode.DRAG_FEED) {
                boolean in = feedZone.contains(e.getX(),e.getY());
                finish(in && hit.correct);
            } else if(mode==Mode.SORT) {
                boolean inCorrect = hit.correct ? sortLeft.contains(e.getX(),e.getY())
                        : sortRight.contains(e.getX(),e.getY());
                finish(inCorrect);
            } else if(mode==Mode.PUZZLE) {
                float dx=nx-hit.slotX,dy=ny-hit.slotY;
                if(dx*dx+dy*dy<.035f){
                    hit.x=hit.slotX;hit.y=hit.slotY;hit.placed=true;
                    boolean all=true;for(Obj o:objects)if(!o.placed)all=false;
                    if(all)finish(true);else invalidate();
                }else{hit.x=hit.homeX;hit.y=hit.homeY;invalidate();if(listener!=null)listener.onWrong();}
            } else if(mode==Mode.MEMORY) {
                handleMemory(hit);
            } else {
                finish(hit.correct);
            }
            dragging=null; return true;
        }
        return true;
    }

    private void handleMemory(Obj hit) {
        if(hit.matched || hit.flipped) return;
        hit.flipped=true;
        if(firstMemory==null){firstMemory=hit;invalidate();return;}
        Obj a=firstMemory; firstMemory=null;
        if(a.pairId==hit.pairId) {
            a.matched=hit.matched=true;
            memoryMatched++;
            if(a.correct && hit.correct) finish(true);
            else {
                postDelayed(()->{ if(listener!=null) listener.onWrong(); },300);
            }
        } else {
            postDelayed(()->{a.flipped=false;hit.flipped=false;invalidate(); if(listener!=null)listener.onWrong();},650);
        }
        invalidate();
    }

    private void finish(boolean correct) {
        if(listener!=null) {
            if(correct) listener.onCorrect(); else listener.onWrong();
        }
    }

    private Obj findHit(float nx,float ny) {
        Obj best=null; float bd=Float.MAX_VALUE;
        for(Obj o:objects) {
            if(o.hidden)continue;
            float dx=nx-o.x,dy=ny-o.y,d=dx*dx+dy*dy;
            float rr=Math.max(.10f,o.radius);
            if(d<rr*rr && d<bd){best=o;bd=d;}
        }
        return best;
    }

    private Obj fromWord(Word w, boolean correct) {
        Obj o=new Obj();o.word=w;o.correct=correct;o.label=w.en;o.emoji=w.emoji;o.radius=.115f;
        return o;
    }

    private void addMemory(Word w, boolean correct, int pairId) {
        Obj o=fromWord(w,correct);o.pairId=pairId;o.radius=.16f;objects.add(o);
    }

    private void layoutGrid(List<Obj> list) {
        float[][] ps={{.24f,.29f},{.70f,.29f},{.24f,.65f},{.70f,.65f}};
        Collections.shuffle(list,random);
        for(int i=0;i<list.size()&&i<4;i++){list.get(i).x=ps[i][0];list.get(i).y=ps[i][1];}
    }

    private void setupBody(Word target) {
        String[] names={"head","hand","arm","leg","foot","nose"};
        float[][] pos={{.52f,.22f},{.34f,.50f},{.39f,.43f},{.43f,.69f},{.38f,.83f},{.52f,.23f}};
        for(int i=0;i<names.length;i++) {
            Obj o=new Obj();
            o.word = new Word(-100-i,names[i],bodyDe(names[i]),bodyRu(names[i]),"body","",true);
            o.correct=names[i].equalsIgnoreCase(target.en);
            o.label=names[i];o.emoji="";o.x=pos[i][0];o.y=pos[i][1];o.radius=.09f;
            objects.add(o);
        }
    }

    private static String bodyDe(String s){
        switch(s){case"head":return"Kopf";case"hand":return"Hand";case"arm":return"Arm";case"leg":return"Bein";case"foot":return"Fuß";default:return"Nase";}
    }
    private static String bodyRu(String s){
        switch(s){case"head":return"голова";case"hand":return"кисть";case"arm":return"рука";case"leg":return"нога";case"foot":return"ступня";default:return"нос";}
    }

    private static int numberValue(String s) {
        switch(s.toLowerCase(Locale.ROOT)){case"one":return 1;case"two":return 2;case"three":return 3;
            case"four":return 4;case"five":return 5;case"six":return 6;default:return 1;}
    }

    private int[] uniqueCounts(int wanted) {
        LinkedHashSet<Integer> s=new LinkedHashSet<>();s.add(wanted);
        while(s.size()<4)s.add(1+random.nextInt(6));
        int[] a=new int[4];int i=0;for(int x:s)a[i++]=x;
        for(i=a.length-1;i>0;i--){int j=random.nextInt(i+1);int t=a[i];a[i]=a[j];a[j]=t;}
        return a;
    }

    private static int colorFor(String s) {
        s=s.toLowerCase(Locale.ROOT);
        if(s.contains("red"))return Color.rgb(239,72,72);
        if(s.contains("blue"))return Color.rgb(72,134,239);
        if(s.contains("green"))return Color.rgb(73,185,99);
        if(s.contains("yellow"))return Color.rgb(250,204,60);
        if(s.contains("purple"))return Color.rgb(152,92,210);
        if(s.contains("black"))return Color.rgb(55,55,65);
        if(s.contains("white"))return Color.WHITE;
        return Color.rgb(190,220,250);
    }

    private static final class Obj {
        Word word;String label="",emoji="✨";boolean correct,hidden,flipped,matched;
        int pairId,count;float x=.5f,y=.5f,vx,vy,radius=.12f,slotX,slotY,homeX,homeY;boolean placed;
    }
}
