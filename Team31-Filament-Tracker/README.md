# Team31 Filament Tracker

Team31 Filament Tracker is an Android + web app for tracking filament spool inventory. The Android app reads NFC tags on compatible spools, and the web app provides a dashboard plus label printing support for a Fichero D11s thermal printer.

![All filament spools labeled and organized in containers](images/labeled-filament-spools-collection.jpg)

## Features

### Android App
- Read NFC tag data from supported filament spools
- Track spool status, notes, and usage
- Search and filter inventory
- Sync data with Firebase

### Web App
- View inventory in a dashboard
- Search and filter spools
- Print labels with a Fichero D11s printer
- Host the site from GitHub Pages using `docs/`
- Use Google Apps Script as the lightweight backend

## Repository Setup

```bash
git clone https://github.com/primemovers31/Team31-Filament-Tracker.git
cd Team31-Filament-Tracker
```

## Android Setup

Prerequisites:
- Android Studio
- JDK 17+
- A compatible Android phone with NFC

Firebase example setup:

```bash
npm install -g firebase-tools
firebase login
firebase projects:create your-project-id --display-name "Team31 Filament Tracker"
firebase apps:create android --project your-project-id --package-name com.bambu.nfc
firebase apps:sdkconfig android --project your-project-id --out app/google-services.json
```

Build and install:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Web Setup

The GitHub Pages version of the site lives in `docs/`.

### 1. Set up the Apps Script backend

- Create a Google Sheet with a `Spools` tab.
- Copy the files from `appscript/` into a new Apps Script project.
- Deploy it as a `Web app` with access set to `Anyone`.
- Copy the deployed `/exec` URL.

Detailed setup notes are in [appscript/README.md](appscript/README.md).

### 2. Point the site at your Apps Script URL

Edit `docs/config.js`:

```js
window.APP_CONFIG = {
  appsScriptUrl: "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec"
}
```

### 3. Publish with GitHub Pages

- Push the repo to GitHub.
- In GitHub, open `Settings` > `Pages`.
- Set the source to `Deploy from a branch`.
- Choose the `main` branch and the `/docs` folder.

## Notes

- `app/` contains the Android app.
- `docs/` contains the GitHub Pages web dashboard.
- `appscript/` contains the Google Apps Script backend template.
- `web/` is the older Firebase-hosted dashboard copy.
- `app/google-services.json` should stay out of version control.
