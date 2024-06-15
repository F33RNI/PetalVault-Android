# 🌸 PetalVault-Android

| ![Project logo](https://github.com/F33RNI/PetalVault-Android/blob/assets/logo.svg) | <h1>Secure offline multi-vault password manager with QR code-based synchronization</h1> |
|------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------:|

![Project banner](https://github.com/F33RNI/PetalVault-Android/blob/assets/banner.png)

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
        <a href="https://github.com/F33RNI/PetalVault-Android/blob/main/README_RU.md"><img alt="Русская версия README" src="https://img.shields.io/badge/На_русском-CF6775?style=flat&logo=github&logoColor=white" style="height:28pt"></a>
        <a href="https://github.com/F33RNI/PetalVault"><img alt="PetalVault desktop version" src="https://img.shields.io/badge/Desktop_version-CF6775?style=flat&logo=python&logoColor=white" style="height:28pt"></a>
        <a href="https://github.com/F33RNI/PetalVault-Android/releases/latest"><img alt="Download app" src="https://img.shields.io/badge/Download-A888C0?style=flat&logo=data:image/svg%2bxml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIiB2ZXJzaW9uPSIxLjEiIGlkPSJMYXllcl8xIiB4PSIwcHgiIHk9IjBweCIgdmlld0JveD0iMCAwIDExNS4yOCAxMjIuODgiIHN0eWxlPSJlbmFibGUtYmFja2dyb3VuZDpuZXcgMCAwIDExNS4yOCAxMjIuODgiIHhtbDpzcGFjZT0icHJlc2VydmUiPjxzdHlsZSB0eXBlPSJ0ZXh0L2NzcyI+LnN0MHtmaWxsLXJ1bGU6ZXZlbm9kZDtjbGlwLXJ1bGU6ZXZlbm9kZDt9PC9zdHlsZT48Zz48cGF0aCBjbGFzcz0ic3QwIiBkPSJNMjUuMzgsNTdoNjQuODhWMzcuMzRINjkuNTljLTIuMTcsMC01LjE5LTEuMTctNi42Mi0yLjZjLTEuNDMtMS40My0yLjMtNC4wMS0yLjMtNi4xN1Y3LjY0bDAsMEg4LjE1IGMtMC4xOCwwLTAuMzIsMC4wOS0wLjQxLDAuMThDNy41OSw3LjkyLDcuNTUsOC4wNSw3LjU1LDguMjR2MTA2LjQ1YzAsMC4xNCwwLjA5LDAuMzIsMC4xOCwwLjQxYzAuMDksMC4xNCwwLjI4LDAuMTgsMC40MSwwLjE4IGMyMi43OCwwLDU4LjA5LDAsODEuNTEsMGMwLjE4LDAsMC4xNy0wLjA5LDAuMjctMC4xOGMwLjE0LTAuMDksMC4zMy0wLjI4LDAuMzMtMC40MXYtMTEuMTZIMjUuMzhjLTQuMTQsMC03LjU2LTMuNC03LjU2LTcuNTYgVjY0LjU1QzE3LjgyLDYwLjQsMjEuMjIsNTcsMjUuMzgsNTdMMjUuMzgsNTd6IE00NS4zNyw4OC4xNmgtOC4yMmwtMS4xOCwzLjg3aC03LjRsOC44My0yMy40N2g3Ljk0bDguOCwyMy40N2gtNy41OUw0NS4zNyw4OC4xNiBMNDUuMzcsODguMTZ6IE00My44Myw4My4wOGwtMi41Ni04LjQ0bC0yLjU3LDguNDRINDMuODNMNDMuODMsODMuMDh6IE01NC44MSw2OC41NWgxMi4wNmMyLjYzLDAsNC41OSwwLjYyLDUuOTEsMS44NyBjMS4zMSwxLjI1LDEuOTYsMy4wMywxLjk2LDUuMzRjMCwyLjM3LTAuNzIsNC4yMy0yLjE0LDUuNTZjLTEuNDMsMS4zNC0zLjYxLDIuMDEtNi41MywyLjAxaC0zLjk4djguNjloLTcuMjhWNjguNTVMNTQuODEsNjguNTV6IE02Mi4wOSw3OC41OWgxLjc5YzEuNDEsMCwyLjQtMC4yNSwyLjk3LTAuNzNjMC41Ny0wLjQ5LDAuODUtMS4xMSwwLjg1LTEuODdjMC0wLjc0LTAuMjUtMS4zNi0wLjc0LTEuODcgYy0wLjQ5LTAuNTEtMS40Mi0wLjc3LTIuNzktMC43N2gtMi4wOFY3OC41OUw2Mi4wOSw3OC41OXogTTc2Ljk3LDY4LjU1aDcuMjR2OC44N2w3LjYtOC44N2g5LjY0bC04LjU3LDguODJsOC45NiwxNC42NWgtOC45MiBsLTQuOTUtOS42NGwtMy43NiwzLjkydjUuNzFoLTcuMjRWNjguNTVMNzYuOTcsNjguNTV6IE05Ny43OSw1N2g5LjkzYzQuMTYsMCw3LjU2LDMuNDEsNy41Niw3LjU2djMxLjQyIGMwLDQuMTUtMy40MSw3LjU2LTcuNTYsNy41NmgtOS45M3YxMy41NWMwLDEuNjEtMC42NSwzLjA0LTEuNyw0LjFjLTEuMDYsMS4wNi0yLjQ5LDEuNy00LjEsMS43Yy0yOS40NCwwLTU2LjU5LDAtODYuMTgsMCBjLTEuNjEsMC0zLjA0LTAuNjQtNC4xLTEuN2MtMS4wNi0xLjA2LTEuNy0yLjQ5LTEuNy00LjFWNS44NWMwLTEuNjEsMC42NS0zLjA0LDEuNy00LjFjMS4wNi0xLjA2LDIuNTMtMS43LDQuMS0xLjdoNTguNzIgQzY0LjY2LDAsNjQuOCwwLDY0Ljk0LDBjMC42NCwwLDEuMjksMC4yOCwxLjc1LDAuNjloMC4wOWMwLjA5LDAuMDUsMC4xNCwwLjA5LDAuMjMsMC4xOGwyOS45OSwzMC4zNmMwLjUxLDAuNTEsMC44OCwxLjIsMC44OCwxLjk4IGMwLDAuMjMtMC4wNSwwLjQxLTAuMDksMC42NVY1N0w5Ny43OSw1N3ogTTY3LjUyLDI3Ljk3VjguOTRsMjEuNDMsMjEuN0g3MC4xOWMtMC43NCwwLTEuMzgtMC4zMi0xLjg5LTAuNzggQzY3Ljg0LDI5LjQsNjcuNTIsMjguNzEsNjcuNTIsMjcuOTdMNjcuNTIsMjcuOTd6IiBmaWxsPSIjZmZmZmZmIi8+PC9nPjwvc3ZnPg==" style="height:28pt"></a>
    </p>
</div>

----------

## 😋 Support Project

> 💜 Please support the project

- BTC: `bc1qaj2ef2jlrt2uafn4kc9cmscuu8yqkjkvxxr5zu`
- ETH: `0x284E6121362ea1C69528eDEdc309fC8b90fA5578`
- ZEC: `t1Jb5tH61zcSTy2QyfsxftUEWHikdSYpPoz`

- Or by my music on [🔷 bandcamp](https://f3rni.bandcamp.com/)

- Or [message me](https://t.me/f33rni) if you would like to donate in other way 💰

----------

## ⚠️ Disclaimer

### PetalVault is under development

> Use at your own risk. The author of the repository is **not** responsible for any damage caused by the repository, the
> application, or its components

----------

## ❓ Getting Started

> PetalVault is a **secure** **offline** password manager with **AES256** + **Scrypt** encryption, **mnemonic phrase**
> as the primary key, and offline synchronization using **QR codes**
>
> PetalVault allows you to create multiple vaults on one device, each protected by different keys. For example, multiple
> people can use one device, or you can use separate vaults for different tasks
>
> Each vault is securely encrypted with a master key derived from **mnemonic phrase (12 words)**. You can
> **scan or show** this phrase using a QR code. Additionally, to simplify access to the vault, you can create
> **your own master password**. In this case, the mnemonic phrase will be encrypted with this password and stored
> in the vault. If you forget the master password, you can restore access to the vault by entering the mnemonic phrase
>
> Each vault can be **exported** and **imported** using **QR codes**. You can also set up
> **synchronization between two devices** to sync only changes, not the entire vault.
> Synchronization can be performed in public places, as the data in QR codes is **securely encrypted**.
> Note that both devices must have the **same mnemonic phrase** since the synchronization key is also derived from it

### 📦 Download and Installation

- To download `.apk` or `.aab` file, go to the
  link <https://github.com/F33RNI/PetalVault-Android/releases/latest> and download the file for your device
- Alternatively, you can build the app yourself. For this, refer to the `🏗️ Building from Source` section

### 🆕 Creating or Importing a Vault

1. First, create or import a vault. To do this, click on the `+ New` or `↓ Import` button
2. Choose any name for your vault. You can **rename** it later
3. Create a mnemonic phrase or choose a randomly generated one. Write this phrase down **in a secure place** because if
   you lose the master password, you can only restore access to the vault using this mnemonic phrase.
    - **For import**, scan the **QR code** of the mnemonic phrase from another device. Keep in mind that the phrase is
      in plain text, so during the scanning, no one should see the QR code
4. Choose whether you want to save the mnemonic phrase encrypted with your master password. If your password is
   **strong** you can safely use the master password.
5. Enter the master password twice. If they do not match, you will be prompted to enter it again
6. Wait for the vault to be created. On weaker devices, this might not happen **instantly**
7. **For import**, scan all QR codes from another device in sequence, after selecting export

### 🔐 Adding Entries

1. To add entries, click `+`. You have 4 fields available: website address (or name for which the password is used),
   username, password, and notes. The data in **all fields are encrypted**, so you can store sensitive data in any of
   them
    - To add an entry, you must fill in the site and/or username fields. PetalVault also automatically
      **generates a strong password** that you can use for this entry
    - All 4 fields have a copy to clipboard function. To do this, click the button to the left of the field
    - The password field is hidden by default. To show the text, click the `👁️` button on the right
2. Confirm by clicking the `✔️ Done` button

### ✏️ Editing and Deleting Entries

- To edit and **copy** data of each entry, click on the text in the list. You can change any field
- To delete an entry, click on the `🗑️` button at the top of the editing dialog and confirm the deletion

### 🔄 Synchronization and Export

1. Click the `Sync` button and select `Sync to`. Choose an existing device or create a new one. This is necessary to
   sync only the changes in the future, not the entire vault
2. On the other device, select `Sync from` and scan the QR codes
3. If the mnemonic phrases do not match (the master password, unlike the mnemonic phrase, **can differ**), an error will
   occur because the synchronization data is encrypted
4. During the first synchronization, especially if there are many entries, there may be **several** QR codes. Scan them
   all sequentially. If you missed a QR code, you can scan it later, as the **order** of scanning **does not matter**

- To export the vault to a new device (**without creating a device**), click the `Sync` button and select `Export`. On
  the other device, click `Import` / `Sync from` and scan all the QR codes

> ⚠️ For synchronization/import/export, the mnemonic phrase on both devices must be **the same**. However, the master
> password, unlike the mnemonic phrase, **can differ**

### 📝 Editing and Deleting the Vault

- You can **rename the vault**. To do this, click the `✏️` button at the top of the dialog and enter a new name
- To **delete the vault**, click the `🗑️` button at the top of the dialog and confirm the deletion

> ⚠️ For security reasons, the vault can only be deleted after it is decrypted

----------

## 🏗️ Build from Source

```shell
# Install JDK 17
$ sudo apt-get install openjdk-17-jdk
$ javac -version
Picked up _JAVA_OPTIONS: -Dawt.useSystemAAFontSettings=on -Dswing.aatext=true
javac 17.0.11

# Clone repo
$ git clone https://github.com/F33RNI/PetalVault-Android.git
$ cd PetalVault-Android

# Install unzip tool
$ sudo apt-get install unzip

# Download SDK manager (CLI version) <https://developer.android.com/studio>
$ wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
$ mkdir sdk && unzip -d sdk commandlinetools-*.zip

# Download Gradle
$ wget https://services.gradle.org/distributions/gradle-4.1-all.zip
$ unzip gradle-*.zip

# Set env variables
$ export ANDROID_HOME=$PWD/sdk
$ export GRADLE_HOME=$PWD/gradle-4.1/bin
$ export PATH=$PATH:$GRADLE_HOME:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools:$ANDROID_HOME/cmdline-tools/bin:$ANDROID_HOME/build-tools/34.0.0

# Update if possible
$ sdkmanager --sdk_root="./sdk" --update

# Install tools (run sdkmanager --sdk_root="./sdk" --list to show available tools and their versions)
$ sdkmanager --sdk_root="./sdk" "build-tools;34.0.0" "platform-tools" "platforms;android-34"

# Finally, build Debug version
# APKs will be in ./app/build/outputs/apk/debug/
$ ./gradlew assembleDebug --stacktrace

# Build release version
# APKs will be in ./app/build/outputs/apk/release/
$ ./gradlew assembleRelease --stacktrace

# Build bundle
# AABs will be in ./app/build/outputs/bundle/release/
$ ./gradlew bundleRelease --stacktrace
```

----------

## ✨ Contribution

- Anyone can contribute! Just create a **pull request**
