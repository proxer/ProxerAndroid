# Proxer.Me Android [![Latest Release](https://img.shields.io/github/release/proxer/ProxerAndroid.svg)](https://github.com/proxer/ProxerAndroid/releases/latest) [![Build status](https://circleci.com/gh/proxer/ProxerAndroid.svg?style=shield)](https://circleci.com/gh/proxer/ProxerAndroid)

![Showcase 1](/art/showcase_news_anime_manga.png?raw=true)
![Showcase 2](/art/showcase_profile_conferences_conference_info_chat.png?raw=true)

### What is this?

This is a client for the german Anime & Manga page [ProxerMe](https://proxer.me/).

### How to use it?

Build the project and install it on your device or download the
[latest release](https://github.com/proxer/ProxerAndroid/releases) and install
it on your device.

#### Building yourself

Assuming that you know how to use [Git](https://git-scm.com/), have the
[Android SDK](https://developer.android.com/sdk/index.html) and the
[Java SDK](http://www.oracle.com/technetwork/java/javase/overview/index.html)
installed, run the following commands:

- `git clone https://github.com/proxer/ProxerMe.git`
- `cd ProxerMe`

This App needs an API key to work. You can request one from the Admins at
Proxer. You then need to create a file `secrets.properties` in the root of the
project with the following contents:

```
PROXER_API_KEY = YourApiKey
```

After that you can build the App like this:

###### Windows

```bash
gradlew.bat assembleDebug
```

###### Linux

```bash
./gradlew assembleDebug
```

You can find the apk in `app/build/outputs/apk/`.

To install it directly on your device through ADB, you can run the following:

###### Windows

```bash
gradlew.bat installDebug
```

###### Linux

```bash
./gradlew installDebug
```

### Your own App

You want to create your own App? Check out
[ProxerLibAndroid](https://github.com/proxer/ProxerLibAndroid). This App depends
highly on it and it implements most of the available API.

### Contribute

Everybody is free to contribute to this project.
To contribute:  
Fork this project, make changes and create a Pull Request.
