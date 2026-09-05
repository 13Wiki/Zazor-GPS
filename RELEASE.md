# Выпуск в Google Play

## 1. Ключ подписи

Ключ создаётся **один раз** и хранится у владельца приложения. Google Play привязывает
приложение к нему навсегда: потеряешь ключ — обновлять приложение больше нельзя, придётся
публиковать новое с нуля и терять всех установивших.

Ключ не должен попадать в git и не должен создаваться на чужой машине.

```bash
keytool -genkeypair -v \
  -keystore ~/zazor-upload.jks \
  -alias zazor \
  -keyalg RSA -keysize 4096 \
  -validity 10000 \
  -storetype PKCS12
```

`keytool` входит в JDK; на macOS с установленной Android Studio он лежит в
`/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool`.

Дальше — файл `keystore.properties` в корне проекта (он в `.gitignore`):

```properties
storeFile=/Users/<имя>/zazor-upload.jks
storePassword=<пароль хранилища>
keyAlias=zazor
keyPassword=<пароль ключа>
```

**Сделай резервную копию `zazor-upload.jks` и паролей в двух местах**, не на том же
диске. Менеджер паролей подходит, переписка — нет.

## 2. Сборка

```bash
./gradlew clean bundleRelease
```

Готовый файл: `app/build/outputs/bundle/release/app-release.aab` — именно его принимает Play.

`assembleRelease` даёт APK и работает без ключа (подпишет отладочным) — это только для
установки на телефон при тестировании. В Play такой файл не пройдёт, и `bundleRelease`
остановится с понятной ошибкой, если `keystore.properties` не найден.

## 3. Перед каждой загрузкой

- Поднять `versionCode` в `app/build.gradle` — Play не принимает повторный номер.
- Прогнать проверки: `./gradlew testDebugUnitTest lintDebug`.
- Проверить, что в собранном APK нет метаданных в снимках (см. раздел ниже).

## 4. Проверка обещания о метаданных

Обещание «в файлах нет метаданных устройства» проверяется на живом телефоне, а не на слово:

```bash
adb pull /sdcard/Android/data/com.gps.zazor/files/Pictures/<файл>.jpg
exiftool <файл>.jpg
```

В выводе не должно быть ни `GPS*`, ни `Make`/`Model`, ни `DateTimeOriginal`.
Координаты видны только на самом изображении.

## 5. Что заполняется в Play Console

Тексты листинга, ответы для формы Data Safety и анкеты возрастного рейтинга —
в `docs/play-listing.md`. Политика приватности — `docs/privacy-policy.html`.
