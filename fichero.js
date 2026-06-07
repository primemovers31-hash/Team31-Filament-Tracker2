var __defProp = Object.defineProperty;
var __defNormalProp = (obj, key, value) => key in obj ? __defProp(obj, key, { enumerable: true, configurable: true, writable: true, value }) : obj[key] = value;
var __publicField = (obj, key, value) => __defNormalProp(obj, typeof key !== "symbol" ? key + "" : key, value);

// ../lib/fichero-printer/web/src/lib/fichero/constants.ts
var SERVICE_UUID = "000018f0-0000-1000-8000-00805f9b34fb";
var WRITE_CHAR_UUID = "00002af1-0000-1000-8000-00805f9b34fb";
var NOTIFY_CHAR_UUID = "00002af0-0000-1000-8000-00805f9b34fb";
var BYTES_PER_ROW = 12;
var CHUNK_SIZE = 200;
var CHUNK_DELAY_MS = 20;
var CMD = {
  getModel: [16, 255, 32, 240],
  getFirmware: [16, 255, 32, 241],
  getSerial: [16, 255, 32, 242],
  getBattery: [16, 255, 80, 241],
  getStatus: [16, 255, 64],
  getShutdownTime: [16, 255, 19],
  setDensity: (level) => [16, 255, 16, 0, level],
  setPaperType: (type) => [16, 255, 132, type],
  setShutdownTime: (mins) => [16, 255, 18, mins >> 8 & 255, mins & 255],
  enablePrinter: [16, 255, 254, 1],
  stopPrint: [16, 255, 254, 69],
  formFeed: [29, 12],
  factoryReset: [16, 255, 4]
};
var FICHERO_CLIENT_DEFAULTS = {
  packetIntervalMs: 20
};

// ../lib/fichero-printer/web/src/lib/fichero/types.ts
var LabelType = /* @__PURE__ */ ((LabelType2) => {
  LabelType2[LabelType2["WithGaps"] = 1] = "WithGaps";
  LabelType2[LabelType2["Black"] = 2] = "Black";
  LabelType2[LabelType2["Continuous"] = 3] = "Continuous";
  return LabelType2;
})(LabelType || {});
var SoundSettingsItemType = /* @__PURE__ */ ((SoundSettingsItemType2) => {
  SoundSettingsItemType2[SoundSettingsItemType2["BluetoothConnectionSound"] = 1] = "BluetoothConnectionSound";
  SoundSettingsItemType2[SoundSettingsItemType2["PowerSound"] = 2] = "PowerSound";
  return SoundSettingsItemType2;
})(SoundSettingsItemType || {});
var RequestCommandId = /* @__PURE__ */ ((RequestCommandId2) => {
  RequestCommandId2[RequestCommandId2["GetModel"] = 8432] = "GetModel";
  RequestCommandId2[RequestCommandId2["GetFirmware"] = 8433] = "GetFirmware";
  RequestCommandId2[RequestCommandId2["GetSerial"] = 8434] = "GetSerial";
  RequestCommandId2[RequestCommandId2["GetBattery"] = 20721] = "GetBattery";
  RequestCommandId2[RequestCommandId2["GetStatus"] = 64] = "GetStatus";
  RequestCommandId2[RequestCommandId2["SetDensity"] = 4096] = "SetDensity";
  RequestCommandId2[RequestCommandId2["SetPaperType"] = 132] = "SetPaperType";
  RequestCommandId2[RequestCommandId2["EnablePrinter"] = 65025] = "EnablePrinter";
  RequestCommandId2[RequestCommandId2["StopPrint"] = 65093] = "StopPrint";
  RequestCommandId2[RequestCommandId2["FormFeed"] = 7436] = "FormFeed";
  RequestCommandId2[RequestCommandId2["RasterData"] = 30256] = "RasterData";
  return RequestCommandId2;
})(RequestCommandId || {});
var ResponseCommandId = /* @__PURE__ */ ((ResponseCommandId2) => {
  ResponseCommandId2[ResponseCommandId2["Ok"] = 20299] = "Ok";
  ResponseCommandId2[ResponseCommandId2["Ack"] = 170] = "Ack";
  ResponseCommandId2[ResponseCommandId2["Error"] = 255] = "Error";
  return ResponseCommandId2;
})(ResponseCommandId || {});
var printTaskNames = ["B1", "D110M_V4"];

// ../lib/fichero-printer/web/src/lib/fichero/emitter.ts
var TypedEventEmitter = class {
  constructor() {
    __publicField(this, "listeners", /* @__PURE__ */ new Map());
  }
  on(event, listener) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, /* @__PURE__ */ new Set());
    }
    this.listeners.get(event).add(listener);
  }
  off(event, listener) {
    this.listeners.get(event)?.delete(listener);
  }
  emit(event, data) {
    this.listeners.get(event)?.forEach((fn) => {
      try {
        fn(data);
      } catch (e) {
        console.error(`Event listener error [${String(event)}]:`, e);
      }
    });
  }
};

// ../lib/fichero-printer/web/src/lib/fichero/utils.ts
var Utils = class {
  static bufToHex(buf, separator = " ") {
    const arr = buf instanceof Uint8Array ? Array.from(buf) : buf;
    return arr.map((b) => b.toString(16).padStart(2, "0")).join(separator);
  }
  static sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }
  static getAvailableTransports() {
    return {
      webBluetooth: typeof navigator !== "undefined" && "bluetooth" in navigator
    };
  }
};

// ../lib/fichero-printer/web/src/lib/fichero/image_encoder.ts
var ImageEncoder = class _ImageEncoder {
  static encodeCanvas(canvas, direction) {
    let source = canvas;
    if (direction === "left") {
      source = _ImageEncoder.rotateCW90(canvas);
    }
    const rowsData = _ImageEncoder.canvasToRaster(source);
    return {
      cols: BYTES_PER_ROW,
      rows: source.height,
      rowsData
    };
  }
  static rotateCW90(canvas) {
    const rotated = document.createElement("canvas");
    rotated.width = canvas.height;
    rotated.height = canvas.width;
    const ctx = rotated.getContext("2d");
    ctx.translate(rotated.width, 0);
    ctx.rotate(Math.PI / 2);
    ctx.drawImage(canvas, 0, 0);
    return rotated;
  }
  static canvasToRaster(canvas) {
    const ctx = canvas.getContext("2d");
    const data = ctx.getImageData(0, 0, canvas.width, canvas.height);
    const px = data.data;
    const rows = canvas.height;
    const out = new Uint8Array(rows * BYTES_PER_ROW);
    for (let y = 0; y < rows; y++) {
      for (let byteIdx = 0; byteIdx < BYTES_PER_ROW; byteIdx++) {
        let byte = 0;
        for (let bit = 0; bit < 8; bit++) {
          const x = byteIdx * 8 + bit;
          if (x < canvas.width) {
            const i = (y * canvas.width + x) * 4;
            if (px[i] === 0) {
              byte |= 128 >> bit;
            }
          }
        }
        out[y * BYTES_PER_ROW + byteIdx] = byte;
      }
    }
    return out;
  }
};

// ../lib/fichero-printer/web/src/lib/fichero/print_task.ts
var AbstractPrintTask = class {
};
var FicheroPrintTask = class extends AbstractPrintTask {
  constructor(opts, sendCmd, sendChunked, emitProgress) {
    super();
    __publicField(this, "opts");
    __publicField(this, "sendCmd");
    __publicField(this, "sendChunked");
    __publicField(this, "emitProgress");
    this.opts = opts;
    this.sendCmd = sendCmd;
    this.sendChunked = sendChunked;
    this.emitProgress = emitProgress;
  }
  async printInit() {
    const statusResp = await this.sendCmd(CMD.getStatus, true);
    if (statusResp.length > 0) {
      const sb = statusResp[statusResp.length - 1];
      if (sb & 2) throw new Error("Cover is open");
      if (sb & 4) throw new Error("No paper loaded");
      if (sb & 80) throw new Error("Printer overheated");
    }
    await this.sendCmd(CMD.setDensity(this.opts.density), true);
    await Utils.sleep(100);
  }
  async printPage(image, quantity) {
    const paperTypeMap = { 1: 0, 2: 1, 3: 2 };
    const paperByte = paperTypeMap[this.opts.labelType] ?? 0;
    for (let copy = 0; copy < quantity; copy++) {
      await this.sendCmd(CMD.setPaperType(paperByte), true);
      await Utils.sleep(50);
      await this.sendCmd(new Array(12).fill(0));
      await Utils.sleep(50);
      await this.sendCmd(Array.from(CMD.enablePrinter));
      await Utils.sleep(50);
      const yL = image.rows & 255;
      const yH = image.rows >> 8 & 255;
      const header = new Uint8Array([29, 118, 48, 0, BYTES_PER_ROW, 0, yL, yH]);
      const payload = new Uint8Array(header.length + image.rowsData.length);
      payload.set(header, 0);
      payload.set(image.rowsData, header.length);
      await this.sendChunked(payload);
      await Utils.sleep(500);
      await this.sendCmd(Array.from(CMD.formFeed));
      await Utils.sleep(300);
      this.emitProgress({
        page: copy + 1,
        pagePrintProgress: 100,
        pageFeedProgress: 0
      });
      await this.sendCmd(Array.from(CMD.stopPrint), true, 6e4);
      this.emitProgress({
        page: copy + 1,
        pagePrintProgress: 100,
        pageFeedProgress: 100
      });
    }
  }
  async waitForFinished() {
  }
  async printEnd() {
  }
};

// ../lib/fichero-printer/web/src/lib/fichero/client.ts
function makePacket(data) {
  const bytes = data instanceof Uint8Array ? data : new Uint8Array(data);
  const cmd = bytes.length >= 2 ? bytes[0] << 8 | bytes[1] : bytes[0] ?? 0;
  return {
    command: cmd,
    toBytes: () => bytes
  };
}
var FicheroClient = class extends TypedEventEmitter {
  constructor() {
    super(...arguments);
    __publicField(this, "device", null);
    __publicField(this, "writeChar", null);
    __publicField(this, "notifyChar", null);
    __publicField(this, "notifyBuf", []);
    __publicField(this, "notifyResolve", null);
    __publicField(this, "heartbeatTimer");
    __publicField(this, "heartbeatFailCount", 0);
    __publicField(this, "packetIntervalMs", 20);
    __publicField(this, "info", {});
    __publicField(this, "abstraction", {
      newPrintTask: (_name, opts) => {
        return new FicheroPrintTask(
          opts,
          (data, wait, timeout) => this.sendCommand(data, wait, timeout),
          (data) => this.sendChunked(data),
          (e) => this.emit("printprogress", e)
        );
      },
      printEnd: async () => {
      },
      printerReset: async () => {
        await this.sendCommand(CMD.factoryReset, true);
      },
      rfidInfo: async () => {
        return {};
      },
      rfidInfo2: async () => {
        return {};
      },
      setSoundEnabled: async (_type, _enabled) => {
      },
      firmwareUpgrade: async (_data, _version) => {
        throw new Error("Firmware upgrade not implemented");
      }
    });
  }
  async connect(opts) {
    let device;
    try {
      device = await navigator.bluetooth.requestDevice({
        filters: [{ namePrefix: "FICHERO" }, { namePrefix: "D11s_" }],
        optionalServices: [SERVICE_UUID]
      });
    } catch {
      throw new Error("No device selected");
    }
    device.addEventListener("gattserverdisconnected", () => this.onDisconnected());
    const server = await device.gatt.connect();
    const service = await server.getPrimaryService(SERVICE_UUID);
    this.writeChar = await service.getCharacteristic(WRITE_CHAR_UUID);
    this.notifyChar = await service.getCharacteristic(NOTIFY_CHAR_UUID);
    await this.notifyChar.startNotifications();
    this.notifyChar.addEventListener("characteristicvaluechanged", (e) => this.onNotify(e));
    this.device = device;
    this.emit("connect", { info: { deviceName: device.name } });
    await this.fetchPrinterInfo();
    this.startHeartbeat();
  }
  disconnect() {
    this.stopHeartbeat();
    if (this.device?.gatt?.connected) {
      this.device.gatt.disconnect();
    }
    this.onDisconnected();
  }
  onDisconnected() {
    this.stopHeartbeat();
    this.device = null;
    this.writeChar = null;
    this.notifyChar = null;
    this.info = {};
    this.emit("disconnect", void 0);
  }
  onNotify(event) {
    const target = event.target;
    const val = new Uint8Array(target.value.buffer);
    this.notifyBuf.push(...val);
    if (this.notifyResolve) {
      this.notifyResolve();
      this.notifyResolve = null;
    }
  }
  waitForNotify(timeout = 2e3) {
    return new Promise((resolve) => {
      const timer = setTimeout(() => {
        this.notifyResolve = null;
        resolve();
      }, timeout);
      this.notifyResolve = () => {
        clearTimeout(timer);
        resolve();
      };
    });
  }
  async sendCommand(data, wait = false, timeout = 2e3) {
    if (!this.writeChar) throw new Error("Not connected");
    const bytes = data instanceof Uint8Array ? data : new Uint8Array(data);
    this.emit("packetsent", { packet: makePacket(bytes) });
    if (wait) {
      this.notifyBuf = [];
    }
    await this.writeChar.writeValueWithoutResponse(bytes);
    if (wait) {
      await this.waitForNotify(timeout);
      await Utils.sleep(50);
    }
    const response = new Uint8Array(this.notifyBuf);
    if (wait && response.length > 0) {
      this.emit("packetreceived", { packet: makePacket(response) });
    }
    return response;
  }
  async sendChunked(data) {
    if (!this.writeChar) throw new Error("Not connected");
    for (let i = 0; i < data.length; i += CHUNK_SIZE) {
      const chunk = data.slice(i, i + CHUNK_SIZE);
      await this.writeChar.writeValueWithoutResponse(chunk);
      await Utils.sleep(CHUNK_DELAY_MS);
    }
  }
  decodeResponse(buf) {
    return new TextDecoder().decode(new Uint8Array(buf)).trim();
  }
  async fetchPrinterInfo() {
    const info = {};
    let r = await this.sendCommand(CMD.getModel, true);
    info.modelId = this.decodeResponse(r);
    r = await this.sendCommand(CMD.getFirmware, true);
    info.firmware = this.decodeResponse(r);
    r = await this.sendCommand(CMD.getSerial, true);
    info.serial = this.decodeResponse(r);
    r = await this.sendCommand(CMD.getBattery, true);
    if (r.length >= 2) {
      info.battery = r[r.length - 1];
      info.charging = r[r.length - 2] !== 0;
    }
    r = await this.sendCommand(CMD.getStatus, true);
    if (r.length > 0) {
      const sb = r[r.length - 1];
      info.status = this.parseStatusByte(sb);
    }
    this.info = info;
    this.emit("printerinfofetched", { info });
  }
  parseStatusByte(byte) {
    const flags = [];
    if (byte & 1) flags.push("printing");
    if (byte & 2) flags.push("cover open");
    if (byte & 4) flags.push("no paper");
    if (byte & 8) flags.push("low battery");
    if (byte & 16 || byte & 64) flags.push("overheated");
    if (byte & 32) flags.push("charging");
    return flags.length ? flags.join(", ") : "ready";
  }
  getModelMetadata() {
    return {
      model: this.info.modelId ?? "D11s",
      printheadPixels: 96,
      printDirection: "left",
      densityMin: 0,
      densityMax: 2,
      densityDefault: 2,
      paperTypes: [1, 2, 3]
      // LabelType values
    };
  }
  getPrintTaskType() {
    return "B1";
  }
  setPacketInterval(ms) {
    this.packetIntervalMs = ms;
  }
  startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatFailCount = 0;
    this.heartbeatTimer = setInterval(async () => {
      try {
        const r = await this.sendCommand(CMD.getBattery, true);
        if (r.length >= 2) {
          const data = {
            chargeLevel: r[r.length - 1],
            charging: r[r.length - 2] !== 0
          };
          this.heartbeatFailCount = 0;
          this.emit("heartbeat", { data });
        }
      } catch {
        this.heartbeatFailCount++;
        this.emit("heartbeatfailed", { failedAttempts: this.heartbeatFailCount });
      }
    }, 5e3);
  }
  stopHeartbeat() {
    if (this.heartbeatTimer !== void 0) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = void 0;
    }
  }
};
var FicheroBluetoothClient = class extends FicheroClient {
};
function instantiateClient(_type) {
  return new FicheroBluetoothClient();
}
export {
  AbstractPrintTask,
  FICHERO_CLIENT_DEFAULTS,
  FicheroBluetoothClient,
  FicheroClient,
  ImageEncoder,
  LabelType,
  RequestCommandId,
  ResponseCommandId,
  SoundSettingsItemType,
  TypedEventEmitter,
  Utils,
  instantiateClient,
  printTaskNames
};
