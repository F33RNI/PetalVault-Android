# 🌸 PetalVault-Android

| ![Логотип проекта](https://github.com/F33RNI/PetalVault-Android/blob/assets/logo.svg) | <h1>Конфиденциальный оффлайн-менеджер паролей с поддержкой нескольких хранилищ и синхронизацией через QR-коды</h1> |
|---------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------:|

![Баннер проекта](https://github.com/F33RNI/PetalVault-Android/blob/assets/banner.png)

----------

<div style="width:100%;text-align:center;">
    <p align="center">
        <img src="https://badges.frapsoft.com/os/v1/open-source.png?v=103" >
        <img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/F33RNI/PetalVault-Android/tests.yml">
        <img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/F33RNI/PetalVault-Android/release.yml">
        <img alt="GitHub License" src="https://img.shields.io/github/license/F33RNI/PetalVault-Android">
    </p>
</div>

<div style="width:100%;text-align:center;">
    <p align="center">
        <a href="https://github.com/F33RNI/PetalVault-Android/blob/main/README.md"><img alt="English version of README" src="https://img.shields.io/badge/English-CF6775?style=flat&logo=github&logoColor=white" style="height:28pt"></a>
        <a href="https://github.com/F33RNI/PetalVault"><img alt="Десктопная версия PetalVault" src="https://img.shields.io/badge/Десктопная_версия-CF6775?style=flat&logo=python&logoColor=white" style="height:28pt"></a>
        <a href="https://github.com/F33RNI/PetalVault-Android/releases/latest"><img alt="Скачать собранное приложение" src="https://img.shields.io/badge/Скачать-A888C0?style=flat&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIiB2ZXJzaW9uPSIxLjEiIGlkPSJMYXllcl8xIiB4PSIwcHgiIHk9IjBweCIgdmlld0JveD0iMCAwIDExNS4yOCAxMjIuODgiIHN0eWxlPSJlbmFibGUtYmFja2dyb3VuZDpuZXcgMCAwIDExNS4yOCAxMjIuODgiIHhtbDpzcGFjZT0icHJlc2VydmUiPjxzdHlsZSB0eXBlPSJ0ZXh0L2NzcyI+LnN0MHtmaWxsLXJ1bGU6ZXZlbm9kZDtjbGlwLXJ1bGU6ZXZlbm9kZDt9PC9zdHlsZT48Zz48cGF0aCBjbGFzcz0ic3QwIiBkPSJNMjUuMzgsNTdoNjQuODhWMzcuMzRINjkuNTljLTIuMTcsMC01LjE5LTEuMTctNi42Mi0yLjZjLTEuNDMtMS40My0yLjMtNC4wMS0yLjMtNi4xN1Y3LjY0bDAsMEg4LjE1IGMtMC4xOCwwLTAuMzIsMC4wOS0wLjQxLDAuMThDNy41OSw3LjkyLDcuNTUsOC4wNSw3LjU1LDguMjR2MTA2LjQ1YzAsMC4xNCwwLjA5LDAuMzIsMC4xOCwwLjQxYzAuMDksMC4xNCwwLjI4LDAuMTgsMC40MSwwLjE4IGMyMi43OCwwLDU4LjA5LDAsODEuNTEsMGMwLjE4LDAsMC4xNy0wLjA5LDAuMjctMC4xOGMwLjE0LTAuMDksMC4zMy0wLjI4LDAuMzMtMC40MXYtMTEuMTZIMjUuMzhjLTQuMTQsMC03LjU2LTMuNC03LjU2LTcuNTYgVjY0LjU1QzE3LjgyLDYwLjQsMjEuMjIsNTcsMjUuMzgsNTdMMjUuMzgsNTd6IE00NS4zNyw4OC4xNmgtOC4yMmwtMS4xOCwzLjg3aC03LjRsOC44My0yMy40N2g3Ljk0bDguOCwyMy40N2gtNy41OUw0NS4zNyw4OC4xNiBMNDUuMzcsODguMTZ6IE00My44Myw4My4wOGwtMi41Ni04LjQ0bC0yLjU3LDguNDRINDMuODNMNDMuODMsODMuMDh6IE01NC44MSw2OC41NWgxMi4wNmMyLjYzLDAsNC41OSwwLjYyLDUuOTEsMS44NyBjMS4zMSwxLjI1LDEuOTYsMy4wMywxLjk2LDUuMzRjMCwyLjM3LTAuNzIsNC4yMy0yLjE0LDUuNTZjLTEuNDMsMS4zNC0zLjYxLDIuMDEtNi41MywyLjAxaC0zLjk4djguNjloLTcuMjhWNjguNTVMNTQuODEsNjguNTV6IE02Mi4wOSw3OC41OWgxLjc5YzEuNDEsMCwyLjQtMC4yNSwyLjk3LTAuNzNjMC41Ny0wLjQ5LDAuODUtMS4xMSwwLjg1LTEuODdjMC0wLjc0LTAuMjUtMS4zNi0wLjc0LTEuODcgYy0wLjQ5LTAuNTEtMS40Mi0wLjc3LTIuNzktMC43N2gtMi4wOFY3OC41OUw2Mi4wOSw3OC41OXogTTc2Ljk3LDY4LjU1aDcuMjR2OC44N2w3LjYtOC44N2g5LjY0bC04LjU3LDguODJsOC45NiwxNC42NWgtOC45MiBsLTQuOTUtOS42NGwtMy43NiwzLjkydjUuNzFoLTcuMjRWNjguNTVMNzYuOTcsNjguNTV6IE05Ny43OSw1N2g5LjkzYzQuMTYsMCw3LjU2LDMuNDEsNy41Niw3LjU2djMxLjQyIGMwLDQuMTUtMy40MSw3LjU2LTcuNTYsNy41NmgtOS45M3YxMy41NWMwLDEuNjEtMC42NSwzLjA0LTEuNyw0LjFjLTEuMDYsMS4wNi0yLjQ5LDEuNy00LjEsMS43Yy0yOS40NCwwLTU2LjU5LDAtODYuMTgsMCBjLTEuNjEsMC0zLjA0LTAuNjQtNC4xLTEuN2MtMS4wNi0xLjA2LTEuNy0yLjQ5LTEuNy00LjFWNS44NWMwLTEuNjEsMC42NS0zLjA0LDEuNy00LjFjMS4wNi0xLjA2LDIuNTMtMS43LDQuMS0xLjdoNTguNzIgQzY0LjY2LDAsNjQuOCwwLDY0Ljk0LDBjMC42NCwwLDEuMjksMC4yOCwxLjc1LDAuNjloMC4wOWMwLjA5LDAuMDUsMC4xNCwwLjA5LDAuMjMsMC4xOGwyOS45OSwzMC4zNmMwLjUxLDAuNTEsMC44OCwxLjIsMC44OCwxLjk4IGMwLDAuMjMtMC4wNSwwLjQxLTAuMDksMC42NVY1N0w5Ny43OSw1N3ogTTY3LjUyLDI3Ljk3VjguOTRsMjEuNDMsMjEuN0g3MC4xOWMtMC43NCwwLTEuMzgtMC4zMi0xLjg5LTAuNzggQzY3Ljg0LDI5LjQsNjcuNTIsMjguNzEsNjcuNTIsMjcuOTdMNjcuNTIsMjcuOTd6IiBmaWxsPSIjZmZmZmZmIi8+PC9nPjwvc3ZnPg==" style="height:28pt"></a>
    </p>
</div>

----------

## 😋 Поддержите Проект

> 💜 Пожалуйста, поддержите проект

- BTC: `bc1qaj2ef2jlrt2uafn4kc9cmscuu8yqkjkvxxr5zu`
- ETH: `0x284E6121362ea1C69528eDEdc309fC8b90fA5578`
- ZEC: `t1Jb5tH61zcSTy2QyfsxftUEWHikdSYpPoz`

- Или, купив мою музыку на [🔷 bandcamp](https://f3rni.bandcamp.com/)

- Или [напишите мне](https://t.me/f33rni), если хотите поддержать проект другим образом 💰

----------

## ⚠️ Дисклеймер

### PetalVault находится в стадии разработке

> Используйте на свой страх и риск. Авторка данного репозитория **не** несёт ответственности за любой ущерб, причинённый
> данным репозиторием, приложением или его компонентами

----------

## ❓ Начало

> PetalVault - **конфиденциальный** **оффлайн** менеджер паролей с очень надёжным шифрованием (**AES256** + **Scrypt**),
**мнемонической фразой** в качестве основного ключа и оффлайн-синхронизацией, используя **QR-коды**.
>
> PetalVault позволяет создавать несколько хранилищ на одном устройстве, защищённых разными ключами. Например, одно
> устройство могут использовать несколько человек или, вы можете использовать отдельные хранилища под каждый круг задач
>
> Каждое хранилище надёжно шифруется мастер-ключом, сгенерированным из **мнемонической фразы (12 слов)**. Вы можете
> **отсканировать или передать** данную фразу при помощи QR-кода. Также, для упрощения доступа к хранилищу, вы можете
> создать **свой мастер-пароль**. В этом случае, мнемоническая фраза будет зашифрована данным паролем и сохранена в
> хранилище. Если вы забудете мастер-пароль, доступ к хранилищу можно восстановить, введя мнемоническую фразу
>
> Каждое хранилище можно **экспортировать** и **импортировать**, используя **QR-коды**. Также, вы можете настроить
> **синхронизацию между двумя устройствами**, чтобы синхронизировать только изменения, а не всё хранилище целиком. Вы
> можете выполнять синхронизацию в публичных местах, т.к. данные, передаваемые QR-кодами **надёжно зашифрованы
> зашифрованы**.
> Обратите внимание, что на обоих устройствах должна быть **одинаковая мнемоническая фраза**, т.к. ключ синхронизации
> генерируется тоже из неё.

----------

### 📦 Скачивание и Установка

- Для скачивания собранного `.apk` или `.aab` файла, перейдите по
  ссылке <https://github.com/F33RNI/PetalVault-Android/releases/latest> и скачайте файл для вашего устройства.
- Также, вы можете собрать приложение самостоятельно. Для этого, обратитесь к секции `🏗️ Сборка из исходного кода`

### 🆕 Создание или Импорт Хранилища

1. Для начала, создайте или импортируйте хранилище. Для этого, нажмите на кнопку `+ Добавить` или `↓ Импорт`
2. Придумайте любое название для вашего хранилища. В дальнейшем, **хранилище можно переименовать**
3. Придумайте мнемоническую фразу или выберите случайно сгенерированную. Вы можете записать эту фразу **в надёжном месте
   **, т.к. при потере мастер-пароля вы сможете восстановить доступ к хранилищу только при помощи данной мнемонической
   фразы
    - **В случае импорта**, отсканируйте **QR-код** мнемонической фразы с другого устройства. Учтите, что фраза
      находится в откытом виде, поэтому, во время сканирования, никто не должен видеть QR-код
4. Выберите, хотите ли вы сохранить мнемоническую фразу, зашифровав её своим мастер-паролем. Если ваш пароль
   **достаточно надёжный** и/или, вероятность того, что файл хранилища окажется в руках злоумышленника крайне
   низкая - вы можете спокойно использовать мастер-пароль
5. Дважды введите мастер-пароль. В случае несоотвествия, вам будет предложено ввести его ещё раз
6. Дождитесь создания хранилища. На слабых устройствах это может происходить **не моментально**
7. **В случае импорта**, отсканируйте по очереди все QR-коды с другого устройства, предварительно выбрав экспорт

### 🔐 Добавление Вхождений

1. Для добавления вхождений нажмите `+`. Вам доступно 4 поля для ввода: адрес сайта (или название, для чего пароль),
   имя пользователя, пароль и заметки. Данные **всех полей шифруются**, поэтому, хранить секретные данные можно в
   любом из них
    - Для добавления вхождения, вы должны заполнить поле адреса сайта и / или имя пользователя. PetalVault также
      автоматически **генерирует надёжный пароль**, который вы можете использовать для данного вхождения
    - Все 4 поля имеют функцию копирования в буфер обмена. Для этого нажмите кнопку, слева от поля
    - Поле пароля по умолчанию скрыто. Для того, чтобы показать текст - нажмите на кнопку `👁️` справа
2. Подтвердите добавление, нажав на кнопку `✔️ Готово`

### ✏️ Редактирование и Удаление Вхождений

- Для редактирования и **копирования** данных каждого вхождения, нажмите на текст в списке. Вы можете изменить любое
  поле
- Для удаления вхождения, нажмите на кнопку `🗑️` вверху диалога редактирования и подтвердите удаление вхождения

### 🔄 Синхронизация и Экспорт

1. Нажмите на кнопку `Синхронизация` и выберите `Синхронизировать на`. Выберите существующее устройство, или создайте
   новое. Это необходимо, чтобы в дальнейшем синхронизировать только изменения, а не всё хранилище целиком
2. На другом устройстве выберите `Синхронизировать с` и отсканируйте QR-коды
3. В случае, если на устройствах не совпадает мнемоническая фраза (мастер-пароль, в отличие от мнемонической фразы
   **может отличаться**), будет вызвана ошибка, т.к. передаваемые данные синхронизации зашифрованы
4. При первой синхронизации, особенно, в случае наличия большого количества вхождений, QR-кодов может быть
   **несколько**. Отсканируйте их все последовательно. Если вы пропустили какой-то QR-код, вы можете сканировать его
   далее, т.к. **порядок** сканирования **не имеет значения**

- Для эскпорта хранилища на новое устройство (**без создания устройства**), нажмите на кнопку `Синхронизация`
  и выберите `Экспорт`. На другом устройстве нажмите `Импорт` / `Синхронизировать с` и отсканируйте все QR-коды

> ⚠️ Для синхронизации / импорта / экспорта, на обоих устройствах должна быть **одинаковая** мнемоническая фраза.
> Однако, мастер-пароль, в отличие от мнемонической фразы **может отличаться**

### 📝 Редактирование и Удаление Хранилища

- Вы можете **переименовать хранилище**. Для этого, нажмите на кнопку `✏️` вверху диалога и введите новое название.
- Для **удаления хранилища**, нажмите на кнопку `🗑️` вверху диалога и подтвердите удаление хранилища

> ⚠️ Для повышения безопасности, удалить хранилище можно только после его расшифровки

----------

## 🏗️ Сборка из Исходного Кода

```shell
# Установите JDK 17
$ sudo apt-get install openjdk-17-jdk
$ javac -version
Picked up _JAVA_OPTIONS: -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true
javac 17.0.11

# Клонируйте репозиторий
$ git clone https://github.com/F33RNI/PetalVault-Android.git
$ cd PetalVault-Android

# Установите утилиту unzip
$ sudo apt-get install unzip

# Скачайте SDK manager (консольная версия) <https://developer.android.com/studio>
$ wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
$ mkdir sdk && unzip -d sdk commandlinetools-*.zip

# Скачайте Gradle
$ wget https://services.gradle.org/distributions/gradle-4.1-all.zip
$ unzip gradle-*.zip

# Установите переменные среды
$ export ANDROID_HOME=$PWD/sdk
$ export GRADLE_HOME=$PWD/gradle-4.1/bin
$ export PATH=$PATH:$GRADLE_HOME:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools:$ANDROID_HOME/cmdline-tools/bin:$ANDROID_HOME/build-tools/34.0.0

# Обновите пакеты, если возможно
$ sdkmanager --sdk_root="./sdk" --update

# Установите средства для сборки (запустите sdkmanager --sdk_root="./sdk" --list чтобы посмотреть доступные средства и их версии)
$ sdkmanager --sdk_root="./sdk" "build-tools;34.0.0" "platform-tools" "platforms;android-34"

# Наконец, соберите Debug версию
# APK-файлы будут в ./app/build/outputs/apk/debug/
$ ./gradlew assembleDebug --stacktrace

# Build release version
# APK-файлы будут в ./app/build/outputs/apk/release/
$ ./gradlew assembleRelease --stacktrace

# Build bundle
# AAB-файлы будут в ./app/build/outputs/bundle/release/
$ ./gradlew bundleRelease --stacktrace
```

----------

## ✨ Участие в Разработке

- Все могут участвовать! Просто создайте **пул-реквест**
