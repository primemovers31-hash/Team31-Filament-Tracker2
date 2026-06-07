# Apps Script Setup

This folder contains a simple Google Apps Script backend for the GitHub Pages dashboard in `docs/`.

## Spreadsheet

Create a Google Sheet with a tab named `Spools`.

Recommended header row:

```text
id,status,filamentType,detailedType,colorRgba,weightGrams,productionDate,notes,diameterMm,lengthMeters,tagUid,dateLastScanned,minHotendTempC,maxHotendTempC,bedTempC,dryingTempC,dryingTimeHours
```

Each spool should be one row under that header row.

## Deploy

1. Open [script.google.com](https://script.google.com/).
2. Create a new Apps Script project.
3. Copy in `Code.gs`.
4. Replace the generated `appsscript.json` with the one in this folder if you want matching settings.
5. Bind the Apps Script project to your spreadsheet or update `getSheet_()` to open a spreadsheet by ID.
6. Click `Deploy` > `New deployment`.
7. Choose `Web app`.
8. Execute as: `Me`
9. Who has access: `Anyone`
10. Copy the `/exec` URL.

## Connect The Site

Paste your web app URL into:

`docs/config.js`

Example:

```js
window.APP_CONFIG = {
  appsScriptUrl: "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec"
};
```
