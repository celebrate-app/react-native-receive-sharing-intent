declare namespace ReceiveSharingIntent {
  export interface ShareIntentFile {
    filePath: string | null;
    text: string | null;
    weblink: string | null;
    mimeType: string | null;
    contentUri: string | null;
    fileName: string | null;
    extension: string | null;
    extra: any;
  }

  export function useSharingIntent(
    handler: Function,
    errorHandler: Function,
    protocol?: string
  ): void;

  export function clearReceivedFiles(): void;
}

export = ReceiveSharingIntent;
