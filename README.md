# RPCMaker Android

An Android app to set a custom Discord Rich Presence from your phone. Log in inside the app, fill the form, hit START.

> ⚠️ Not affiliated with Discord. Your token = your password. Use at your own risk.

## Features

- In-app Discord login (WebView)
- Custom Name, Details, State, Activity Type
- Large/small images with hover texts
- Up to 2 buttons
- START / STOP toggle

## Build

```bash
git clone https://github.com/FlorestDevAndroid/RPCMaker-Android.git
cd RPCMaker-Android
./gradlew assembleDebug
```

## Usage
Log in to Discord in the app.

Fill the form (Name, Details, State, Type, Images, Buttons).

Tap START. Tap STOP to clear.


## Security
The token is kept in memory only, never logged or sent anywhere except Discord. Revoke it via Discord → User Settings → Authorized Apps if leaked.

## License
MIT — see LICENSE.
