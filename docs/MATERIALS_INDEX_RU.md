# Индекс материалов Lumi Kids

## В репозитории

- Android-исходники в `app/src/main/java`;
- словарь в `app/src/main/assets/dictionary.csv`;
- сценарии Animals, Body, Colors, Family, Food, Numbers;
- AndroidManifest и Gradle-конфигурация;
- документация по архитектуре и урокам;
- скрипты проверки, Vosk и Piper;
- GitHub Actions для сборки.

## В локальной папке загрузок

Папка handoff содержит исходный проект Lumi Kids и большой ZIP-архив. Готового Lumi APK там не найдено.

Файл `LearningTracker-0.1.0-debug-reference.apk` относится к другому приложению и в Lumi Kids не включается.

## Не включать в Lumi Kids

- Claude Setup.exe;
- LearningTracker;
- Telegram Archive Manager;
- чужие APK и архивы;
- временные папки Android Studio и Gradle.

Такое разделение не даёт смешать несколько разных проектов при восстановлении.
