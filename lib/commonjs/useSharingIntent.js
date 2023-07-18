"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.useSharingIntent = useSharingIntent;

var _react = require("react");

var _reactNative = require("react-native");

var _utils = require("./utils");

const {
  ReceiveSharingIntent
} = _reactNative.NativeModules;
const isIos = _reactNative.Platform.OS === 'ios';

function useSharingIntent(handler, errorHandler, protocol = 'ShareMedia') {
  const getFileNames = (0, _react.useCallback)(url => {
    if (isIos) {
      ReceiveSharingIntent.getFileNames(url).then(data => {
        const files = (0, _utils.sortData)(data);
        handler(files);
      }).catch(e => errorHandler(e));
    } else {
      ReceiveSharingIntent.getFileNames().then(fileObject => {
        const files = Object.keys(fileObject).map(k => fileObject[k]);
        handler(files);
      }).catch(e => errorHandler(e));
    }
  }, [handler, errorHandler]);
  (0, _react.useEffect)(() => {
    if (isIos) {
      _reactNative.Linking.getInitialURL().then(res => {
        if (res !== null && res !== void 0 && res.startsWith(`${protocol}://dataUrl`)) {
          getFileNames(res);
        }
      }).catch(() => {});

      const listener = _reactNative.Linking.addEventListener('url', res => {
        const url = res ? res.url : '';

        if (url.startsWith(`${protocol}://dataUrl`)) {
          getFileNames(res.url);
        }
      });

      return () => {
        listener === null || listener === void 0 ? void 0 : listener.remove();
      };
    } else {
      const listener = _reactNative.AppState.addEventListener('change', status => {
        if (status === 'active') {
          getFileNames('');
        }
      });

      return () => {
        listener === null || listener === void 0 ? void 0 : listener.remove();
      };
    }
  }, [getFileNames, protocol]);
}
//# sourceMappingURL=useSharingIntent.js.map