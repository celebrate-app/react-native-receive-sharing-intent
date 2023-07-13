"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.clearReceivedFiles = void 0;

var _reactNative = require("react-native");

const {
  ReceiveSharingIntent
} = _reactNative.NativeModules;
const clearReceivedFiles = ReceiveSharingIntent.clearFileNames;
exports.clearReceivedFiles = clearReceivedFiles;
//# sourceMappingURL=ReceiveSharingIntent.js.map