#!/usr/bin/env python3
import csv, json, os, sys, wave
from pathlib import Path
from piper import PiperVoice, SynthesisConfig

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'app/src/main/assets'
MODELS=ROOT/'.voice-models'
OUT=ASSETS/'voice'
VOICES={
 'en':('en_US-amy-medium.onnx','en_US-amy-medium.onnx.json'),
 'de':('de_DE-ramona-low.onnx','de_DE-ramona-low.onnx.json'),
 'ru':('ru_RU-irina-medium.onnx','ru_RU-irina-medium.onnx.json'),
}
UI={
 'en':{'home_welcome':'Hi! I am Lumi. Pick a game and let us learn together!','tickle':'Hee hee! That tickles!','generic_prompt':'Find it!','generic_success':'Yes! Great job!','generic_retry':'Oops! Try again.','generic_joke':'My glowing heart is happy!','generic_speech_prompt':'Now say the word.','generic_speech_success':'I heard you! Awesome!','review_intro':'Let us explore a thousand words!','review_closing':'Great adventure! We will continue next time.','custom_intro':'Let us play with your words!','custom_closing':'Great job!'},
 'de':{'home_welcome':'Hallo! Ich bin Lumi. Wähle ein Spiel und lass uns zusammen lernen!','tickle':'Hihi! Das kitzelt!','generic_prompt':'Finde es!','generic_success':'Ja! Super!','generic_retry':'Hoppla! Versuch es noch einmal.','generic_joke':'Mein leuchtendes Herz freut sich!','generic_speech_prompt':'Jetzt sag das Wort.','generic_speech_success':'Ich höre dich! Klasse!','review_intro':'Wir entdecken tausend Wörter!','review_closing':'Tolles Abenteuer! Nächstes Mal geht es weiter.','custom_intro':'Wir spielen mit deinen Wörtern!','custom_closing':'Super gemacht!'},
 'ru':{'home_welcome':'Привет! Я Lumi. Выбери игру, и давай учиться вместе!','tickle':'Хи-хи! Щекотно!','generic_prompt':'Найди!','generic_success':'Да! Молодец!','generic_retry':'Ой! Попробуй ещё раз.','generic_joke':'Моё светящееся сердечко радуется!','generic_speech_prompt':'Теперь повтори слово.','generic_speech_success':'Я тебя слышу! Здорово!','review_intro':'Отправляемся в приключение по тысяче слов!','review_closing':'Отличное приключение! Продолжим в следующий раз.','custom_intro':'Поиграем с твоими словами!','custom_closing':'Отличная работа!'},
}

def scenario_lines():
    out={l:{} for l in VOICES}
    for fp in sorted((ASSETS/'scenarios').glob('*.json')):
        d=json.loads(fp.read_text(encoding='utf-8')); sid=d['id']
        for field in ('intro','closing'):
            for lang,text in d[field].items(): out[lang][f'{sid}_{field}']=text
        for i,r in enumerate(d['rounds'],1):
            for field,keypart in [('prompt','prompt'),('success','success'),('retry','retry'),('joke','joke'),('speechPrompt','speech_prompt'),('speechSuccess','speech_success')]:
                if field not in r: continue
                for lang,text in r[field].items(): out[lang][f'{sid}_r{i}_{keypart}']=text
    for lang,vals in UI.items(): out[lang].update(vals)
    return out

def synth(voice,text,path,cfg):
    path.parent.mkdir(parents=True,exist_ok=True)
    if path.exists() and path.stat().st_size>1000:return
    with wave.open(str(path),'wb') as f: voice.synthesize_wav(text,f,syn_config=cfg)

def main():
    rows=list(csv.DictReader((ASSETS/'dictionary.csv').open(encoding='utf-8')))
    lines=scenario_lines()
    for lang,(model,config) in VOICES.items():
        print('loading',lang,flush=True)
        voice=PiperVoice.load(str(MODELS/model),config_path=str(MODELS/config))
        cfg=SynthesisConfig(length_scale=1.08,noise_scale=0.60,noise_w_scale=0.72,normalize_audio=True)
        for i,(key,text) in enumerate(lines[lang].items(),1): synth(voice,text,OUT/lang/(key+'.wav'),cfg)
        for i,r in enumerate(rows,1):
            synth(voice,r[lang],OUT/lang/(f"word_{r['id']}.wav"),cfg)
            if i%100==0: print(lang,'words',i,flush=True)
    print('voice pack ready')
if __name__=='__main__':main()
