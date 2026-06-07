var SHEET_NAME = 'Spools';
var NUMERIC_FIELDS = [
  'id',
  'colorRgba',
  'weightGrams',
  'diameterMm',
  'lengthMeters',
  'dateLastScanned',
  'minHotendTempC',
  'maxHotendTempC',
  'bedTempC',
  'dryingTempC',
  'dryingTimeHours'
];

function doGet(e) {
  var action = (e && e.parameter && e.parameter.action) || 'list';

  try {
    if (action === 'list') {
      return respond_(e, { ok: true, spools: listSpools_() });
    }

    if (action === 'updateStatus') {
      updateStatus_(e.parameter.id, e.parameter.status);
      return respond_(e, { ok: true });
    }

    if (action === 'updateNotes') {
      updateNotes_(e.parameter.id, e.parameter.notes || '');
      return respond_(e, { ok: true });
    }

    if (action === 'ping') {
      return respond_(e, { ok: true, message: 'Apps Script is live.' });
    }

    return respond_(e, { ok: false, error: 'Unknown action.' });
  } catch (err) {
    return respond_(e, {
      ok: false,
      error: err && err.message ? err.message : String(err)
    });
  }
}

function respond_(e, payload) {
  var callback = e && e.parameter && e.parameter.callback;
  if (callback) {
    return ContentService
      .createTextOutput(callback + '(' + JSON.stringify(payload) + ');')
      .setMimeType(ContentService.MimeType.JAVASCRIPT);
  }

  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

function getSheet_() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getSheetByName(SHEET_NAME);
  if (!sheet) {
    throw new Error('Missing "' + SHEET_NAME + '" sheet.');
  }
  return sheet;
}

function getHeaders_(sheet) {
  var lastColumn = sheet.getLastColumn();
  if (!lastColumn) {
    throw new Error('Sheet has no headers.');
  }
  return sheet.getRange(1, 1, 1, lastColumn).getValues()[0];
}

function listSpools_() {
  var sheet = getSheet_();
  var headers = getHeaders_(sheet);
  var lastRow = sheet.getLastRow();
  if (lastRow < 2) return [];

  var values = sheet.getRange(2, 1, lastRow - 1, headers.length).getValues();
  return values
    .filter(function(row) {
      return row.some(function(cell) { return cell !== ''; });
    })
    .map(function(row) {
      var spool = {};
      headers.forEach(function(header, index) {
        spool[header] = normalizeValue_(header, row[index]);
      });
      return spool;
    });
}

function updateStatus_(id, status) {
  if (!id) throw new Error('Missing spool id.');
  if (!status) throw new Error('Missing status.');

  var sheet = getSheet_();
  var headers = getHeaders_(sheet);
  var rowIndex = findRowById_(sheet, headers, id);

  setCell_(sheet, headers, rowIndex, 'status', status);
  setCell_(sheet, headers, rowIndex, 'dateLastScanned', Date.now());
}

function updateNotes_(id, notes) {
  if (!id) throw new Error('Missing spool id.');

  var sheet = getSheet_();
  var headers = getHeaders_(sheet);
  var rowIndex = findRowById_(sheet, headers, id);

  setCell_(sheet, headers, rowIndex, 'notes', notes);
}

function findRowById_(sheet, headers, id) {
  var idColumn = headers.indexOf('id');
  if (idColumn === -1) {
    throw new Error('Missing "id" column.');
  }

  var lastRow = sheet.getLastRow();
  if (lastRow < 2) {
    throw new Error('No spool rows found.');
  }

  var ids = sheet.getRange(2, idColumn + 1, lastRow - 1, 1).getValues();
  var needle = String(id);

  for (var i = 0; i < ids.length; i++) {
    if (String(ids[i][0]) === needle) {
      return i + 2;
    }
  }

  throw new Error('Spool id ' + id + ' not found.');
}

function setCell_(sheet, headers, rowIndex, headerName, value) {
  var colIndex = headers.indexOf(headerName);
  if (colIndex === -1) {
    throw new Error('Missing "' + headerName + '" column.');
  }
  sheet.getRange(rowIndex, colIndex + 1).setValue(value);
}

function normalizeValue_(header, value) {
  if (value === '') return '';
  if (NUMERIC_FIELDS.indexOf(header) !== -1) return Number(value) || 0;
  return value;
}
