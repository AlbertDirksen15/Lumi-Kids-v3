#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/app/src/main/assets/vosk"
mkdir -p "$OUT"
fetch(){ lang="$1"; name="$2"; url="https://alphacephei.com/vosk/models/${name}.zip"; tmp="$(mktemp -d)"; echo "Downloading $name"; curl -L --fail --retry 3 "$url" -o "$tmp/m.zip"; unzip -q "$tmp/m.zip" -d "$tmp/u"; rm -rf "$OUT/$lang"; mkdir -p "$OUT/$lang"; cp -a "$tmp/u/$name"/. "$OUT/$lang"/; rm -rf "$tmp"; }
[ -f "$OUT/en/conf/model.conf" ] || fetch en vosk-model-small-en-us-0.15
[ -f "$OUT/de/conf/model.conf" ] || fetch de vosk-model-small-de-0.15
[ -f "$OUT/ru/conf/model.conf" ] || fetch ru vosk-model-small-ru-0.22
