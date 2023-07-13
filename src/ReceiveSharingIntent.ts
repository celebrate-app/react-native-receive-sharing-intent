import { NativeModules } from 'react-native';

const { ReceiveSharingIntent } = NativeModules;

export const clearReceivedFiles: () => void =
  ReceiveSharingIntent.clearFileNames;
