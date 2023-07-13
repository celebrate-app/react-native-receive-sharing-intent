import { useCallback, useEffect } from 'react';
import { AppState, Linking, NativeModules, Platform } from 'react-native';

import { sortData } from './utils';
const { ReceiveSharingIntent } = NativeModules;

const isIos = Platform.OS === 'ios';

export function useSharingIntent(
  handler: Function,
  errorHandler: Function,
  protocol: string = 'ShareMedia'
) {
  const getFileNames = useCallback(
    (url: string) => {
      if (isIos) {
        ReceiveSharingIntent.getFileNames(url)
          .then((data: any) => {
            const files = sortData(data);
            handler(files);
          })
          .catch((e: any) => errorHandler(e));
      } else {
        ReceiveSharingIntent.getFileNames()
          .then((fileObject: any) => {
            const files = Object.keys(fileObject).map((k) => fileObject[k]);
            handler(files);
          })
          .catch((e: any) => errorHandler(e));
      }
    },
    [handler, errorHandler]
  );

  useEffect(() => {
    if (isIos) {
      Linking.getInitialURL()
        .then((res: any) => {
          if (res?.startsWith(`${protocol}://dataUrl`)) {
            getFileNames(res);
          }
        })
        .catch(() => {});

      const listener: any = Linking.addEventListener('url', (res: any) => {
        const url = res ? res.url : '';
        if (url.startsWith(`${protocol}://dataUrl`)) {
          getFileNames(res.url);
        }
      });

      return () => {
        listener?.remove();
      };
    } else {
      const listener: any = AppState.addEventListener(
        'change',
        (status: string) => {
          if (status === 'active') {
            getFileNames('');
          }
        }
      );

      return () => {
        listener?.remove();
      };
    }
  }, [getFileNames, protocol]);
}
