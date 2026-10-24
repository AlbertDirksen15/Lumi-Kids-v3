# Lumi Kids v3 architecture

## Why this stack

Lumi Kids v3 is deliberately a native Java Android app using Canvas/View rendering instead of Unity or a large 2D engine. The Galaxy Tab 3 GT-P5200 has an old Intel Atom x86 CPU and limited RAM, so the game loop keeps allocations low and renders simple vector shapes. There are no native game-engine binaries to maintain for x86. The same APK also includes ARM/x86 Vosk ABIs for modern Android and the old tablet.

## Modules

- `MainActivity` — onboarding, clear **I know → I learn**, reading mode, topics, settings.
- `GameActivity` — lesson state machine: intro → game → reaction → speech → SRS → next round.
- `LumiView` — vector Canvas mascot. No PNG sprite dependency.
- `GameView` — moving objects, color choice, counting, memory, drag-to-feed, body hotspots, sorting, puzzle.
- `VoiceEngine` / `BundledVoiceEngine` — plays Piper-generated local WAV files. No Android system TTS dependency for built-in content.
- `SpeechRecognizerEngine` / `VoskSpeechRecognizer` — replaceable speech engine interface. Vosk is the current implementation.
- `LumiDb` — SQLite progress, settings, rewards and user words.
- `SrsScheduler` — 15 min → 1 d → 3 d → 7 d → 14 d → 30 d.
- `DictionaryRepository` — 1000 built-in unique trilingual entries + user words.
- `ScenarioRepository` — JSON lesson scripts.

## Offline speech strategy

Three small Vosk models are packaged at build time (EN/DE/RU). Only the model for the current target language is copied/loaded into memory; switching the target language closes the previous model. Recognition uses a narrow grammar for the expected short word when possible, then Levenshtein similarity as a tolerant child-oriented acceptance layer.

## Voice strategy

Piper runs in CI, not on the tablet. It generates WAV clips for all 1000 built-in words in all three languages plus all scripted tutor lines. This makes the runtime cheap and deterministic on the old x86 tablet and avoids system/Google TTS. The trade-off is a larger APK. User-imported words are intentionally not sent to any service and do not get synthetic audio until an explicit local voice-generation workflow is added.

## Orientation

- `smallestScreenWidthDp >= 600`: sensor landscape (tablet).
- smaller devices: sensor portrait (phone).

## Privacy

Manifest permission: `RECORD_AUDIO` only. The mic is optional and requested at runtime. No INTERNET permission, advertising SDK, Firebase, analytics or Google Play Services.
