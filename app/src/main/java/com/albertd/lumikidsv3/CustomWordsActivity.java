package com.albertd.lumikidsv3;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CustomWordsActivity extends Activity {
    private static final int PICK_FILE=811;
    private LumiDb db;
    private EditText en,de,ru,emoji,bulk;
    private LinearLayout list;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);db=new LumiDb(this);build();
    }

    private void build(){
        ScrollView s=new ScrollView(this);LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(16),dp(18),dp(18));
        root.setBackgroundColor(Color.rgb(247,250,255));s.addView(root);
        TextView title=txt("Мои слова / Meine Wörter / My words",26,true);root.addView(title);

        en=input("English");de=input("Deutsch");ru=input("Русский");emoji=input("Emoji, например 🐱");
        root.addView(en);root.addView(de);root.addView(ru);root.addView(emoji);
        Button add=button("＋ Добавить слово");root.addView(add,new LinearLayout.LayoutParams(-1,dp(58)));
        add.setOnClickListener(v->addOne());

        TextView help=txt("Импорт списком: одна строка = English;Deutsch;Русский;🐱",15,false);
        root.addView(help);
        bulk=new EditText(this);bulk.setMinLines(6);bulk.setGravity(Gravity.TOP);
        bulk.setHint("cat;Katze;кошка;🐱\ndog;Hund;собака;🐶");bulk.setTextSize(16);
        bulk.setBackground(round(Color.WHITE));bulk.setPadding(dp(12),dp(10),dp(12),dp(10));root.addView(bulk);

        LinearLayout rr=new LinearLayout(this);rr.setOrientation(LinearLayout.HORIZONTAL);
        Button paste=button("Импортировать текст");Button file=button("TXT / CSV файл");
        rr.addView(paste,new LinearLayout.LayoutParams(0,dp(58),1));rr.addView(file,new LinearLayout.LayoutParams(0,dp(58),1));
        root.addView(rr);paste.setOnClickListener(v->importText(bulk.getText().toString()));
        file.setOnClickListener(v->pickFile());

        root.addView(txt("Сохранённые слова",18,true));
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);
        renderList();
        setContentView(s);
    }

    private void addOne(){
        if(en.getText().toString().trim().isEmpty()||de.getText().toString().trim().isEmpty()||ru.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Заполни EN / DE / RU",Toast.LENGTH_SHORT).show();return;
        }
        db.addCustomWord(en.getText().toString(),de.getText().toString(),ru.getText().toString(),emoji.getText().toString());
        en.setText("");de.setText("");ru.setText("");emoji.setText("");renderList();
    }

    private void importText(String text){
        int ok=0,bad=0;
        for(String raw:text.split("\\r?\\n")){
            String line=raw.trim();if(line.isEmpty())continue;
            String[] a=line.contains(";")?line.split(";",-1):parseCsv(line);
            if(a.length<3){bad++;continue;}
            if(a[0].trim().isEmpty()||a[1].trim().isEmpty()||a[2].trim().isEmpty()){bad++;continue;}
            db.addCustomWord(a[0],a[1],a[2],a.length>=4?a[3]:"✨");ok++;
        }
        Toast.makeText(this,"Добавлено: "+ok+(bad>0?" • пропущено: "+bad:""),Toast.LENGTH_LONG).show();
        renderList();
    }

    private String[] parseCsv(String line){
        ArrayList<String>x=new ArrayList<>();StringBuilder b=new StringBuilder();boolean q=false;
        for(int i=0;i<line.length();i++){char c=line.charAt(i);
            if(c=='"')q=!q;else if(c==','&&!q){x.add(b.toString());b.setLength(0);}else b.append(c);}
        x.add(b.toString());return x.toArray(new String[0]);
    }

    private void pickFile(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");startActivityForResult(i,PICK_FILE);
    }

    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==PICK_FILE&&res==RESULT_OK&&data!=null){
            Uri u=data.getData();if(u==null)return;
            try(InputStream in=getContentResolver().openInputStream(u)){
                ByteArrayOutputStream b=new ByteArrayOutputStream();byte[]buf=new byte[8192];int n;
                while((n=in.read(buf))>0)b.write(buf,0,n);
                String text=b.toString(StandardCharsets.UTF_8.name());
                bulk.setText(text);importText(text);
            }catch(Exception e){Toast.makeText(this,"Не удалось прочитать файл: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        }
    }

    private void renderList(){
        if(list==null)return;list.removeAllViews();
        for(Word w:db.getCustomWords()){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
            TextView t=txt(w.emoji+"  "+w.en+" • "+w.de+" • "+w.ru,15,false);
            Button del=button("✕");del.setOnClickListener(v->{db.deleteCustomWord(w.id);renderList();});
            row.addView(t,new LinearLayout.LayoutParams(0,-2,1));row.addView(del,new LinearLayout.LayoutParams(dp(54),dp(48)));list.addView(row);
        }
    }

    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(17);e.setSingleLine(true);
        e.setBackground(round(Color.WHITE));e.setPadding(dp(12),dp(8),dp(12),dp(8));e.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(56)));return e;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(Color.rgb(17,47,89));b.setBackground(round(Color.WHITE));return b;}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.rgb(17,47,89));if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);t.setPadding(dp(5),dp(6),dp(5),dp(6));return t;}
    private android.graphics.drawable.GradientDrawable round(int c){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(c);g.setCornerRadius(dp(18));g.setStroke(1,Color.rgb(215,228,243));return g;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override protected void onDestroy(){db.close();super.onDestroy();}
}
