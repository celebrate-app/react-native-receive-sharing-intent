import MimeTypes from 'mime-db';

interface IReturnData {
  filePath?: any | string;
  text?: any | string;
  weblink?: any | string;
  mimeType?: any | string;
  contentUri?: any | string;
  fileName?: any | string;
  extension?: any | string;
  extra?: any;
}

const getFileName = (file: string): string => {
  return file.replace(/^.*(\\|\/|:)/, '');
};

const getExtension = (fileName: string): string => {
  return fileName.substring(fileName.lastIndexOf('.') + 1);
};

const getMimeType = (file: string): string => {
  const ext = getExtension(file);
  const extension = '.' + ext.toLowerCase();
  const type = Object.entries(MimeTypes).find(
    (mime) => mime[1]?.extensions && mime[1]?.extensions.includes(extension)
  );

  if (type) return type[0];
  return '';
};

export function sortData(data: any): IReturnData[] {
  const objects: IReturnData = {
    filePath: null,
    text: null,
    weblink: null,
    mimeType: null,
    contentUri: null,
    fileName: null,
    extension: null,
  };

  const file = data;

  if (file.startsWith('text:')) {
    const text = file.replace('text:', '');
    if (text.startsWith('http')) {
      const object: IReturnData[] = [{ ...objects, weblink: text }];
      return object;
    }
    const object = [{ ...objects, text }];
    return object;
  } else if (file.startsWith('webUrl:')) {
    const weblink: string = file.replace('webUrl:', '');
    const object: IReturnData[] = [{ ...objects, weblink }];
    return object;
  } else {
    try {
      const files = JSON.parse(file);
      const object = [];
      for (let i = 0; i < files.length; i++) {
        const path = files[i].path;
        const obj = {
          ...objects,
          fileName: getFileName(path),
          extension: getExtension(path),
          mimeType: getMimeType(path),
          filePath: path,
          extra: files[i].extra,
        };
        object.push(obj);
      }
      return object;
    } catch (error) {
      return [{ ...objects }];
    }
  }
}
