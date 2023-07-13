import { useCallback, useEffect } from 'react';
import { AppState, Linking, NativeModules, Platform } from 'react-native';
import { sortData } from './utils';
const {
  ReceiveSharingIntent
} = NativeModules;
const isIos = Platform.OS === 'ios';
export function useSharingIntent(handler, errorHandler, protocol = 'ShareMedia') {
  const getFileNames = useCallback(url => {
    if (isIos) {
      ReceiveSharingIntent.getFileNames(url).then(data => {
        const files = sortData(data);
        handler(files);
      }).catch(e => errorHandler(e));
    } else {
      ReceiveSharingIntent.getFileNames().then(fileObject => {
        const files = Object.keys(fileObject).map(k => fileObject[k]);
        handler(files);
      }).catch(e => errorHandler(e));
    }
  }, [handler, errorHandler]);
  useEffect(() => {
    if (isIos) {
      Linking.getInitialURL().then(res => {
        if (res !== null && res !== void 0 && res.startsWith(`${protocol}://dataUrl`)) {
          getFileNames(res);
        }
      }).catch(() => {});
      const listener = Linking.addEventListener('url', res => {
        const url = res ? res.url : '';

        if (url.startsWith(`${protocol}://dataUrl`)) {
          getFileNames(res.url);
        }
      });
      return () => {
        listener === null || listener === void 0 ? void 0 : listener.remove();
      };
    } else {
      const listener = AppState.addEventListener('change', status => {
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