#!/usr/bin/env python3
import csv,json,glob,re,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; AS=ROOT/'app/src/main/assets'
rows=list(csv.DictReader((AS/'dictionary.csv').open(encoding='utf-8')))
assert len(rows)==1000, len(rows)
assert len({r['en'].casefold() for r in rows})==1000
ix={r['en'].casefold():r for r in rows}
expected={'mom':('Mama','мама'),'dad':('Papa','папа'),'sorry':('Entschuldigung','простите'),'ball':('Ball','мяч'),'arm':('Arm','рука'),'sound':('Ton','звук'),'page':('Seite','страница')}
for k,(de,ru) in expected.items(): assert ix[k]['de']==de and ix[k]['ru']==ru,(k,ix.get(k))
bad={'beer','wine','cigarette','casino','porn','weapon','bier','wein','zigarette','waffe','оружие','пиво','вино','сигарета','казино'}
for r in rows:
    tokens=set(re.findall(r"[\w-]+", ' '.join([r['en'],r['de'],r['ru']]).casefold()))
    assert not (tokens & bad),r
required={'animals','colors','numbers','family','food','body'}
seen=set()
for fp in (AS/'scenarios').glob('*.json'):
    d=json.loads(fp.read_text(encoding='utf-8'));seen.add(d['id']); assert len(d['rounds'])>=6
    for r in d['rounds']:
        assert r['word'].casefold() in ix,(fp,r['word'])
        for w in r.get('distractors',[]): assert w.casefold() in ix,(fp,w)
        for k in ['prompt','success','retry','speechPrompt','speechSuccess']:
            assert set(r[k])=={'en','de','ru'},(fp,k)
assert required<=seen
manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text()
assert 'android.permission.INTERNET' not in manifest
for p in ROOT.rglob('*'):
    if p.is_file() and p.suffix in {'.java','.gradle','.xml'}:
        s=p.read_text(errors='ignore').lower(); assert 'firebase' not in s and 'com.google.android.gms' not in s,p
print('OK: 1000 unique words, scenarios valid, no Internet/Google services')
