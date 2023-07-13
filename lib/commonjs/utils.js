"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.sortData = sortData;

var _mimeDb = _interopRequireDefault(require("mime-db"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const getFileName = file => {
  return file.replace(/^.*(\\|\/|:)/, '');
};

const getExtension = fileName => {
  return fileName.substring(fileName.lastIndexOf('.') + 1);
};

const getMimeType = file => {
  const ext = getExtension(file);
  const extension = '.' + ext.toLowerCase();
  const type = Object.entries(_mimeDb.default).find(mime => {
    var _mime$, _mime$2;

    return ((_mime$ = mime[1]) === null || _mime$ === void 0 ? void 0 : _mime$.extensions) && ((_mime$2 = mime[1]) === null || _mime$2 === void 0 ? void 0 : _mime$2.extensions.includes(extension));
  });
  if (type) return type[0];
  return '';
};

function sortData(data) {
  const objects = {
    filePath: null,
    text: null,
    weblink: null,
    mimeType: null,
    contentUri: null,
    fileName: null,
    extension: null
  };
  const file = data;

  if (file.startsWith('text:')) {
    const text = file.replace('text:', '');

    if (text.startsWith('http')) {
      const object = [{ ...objects,
        weblink: text
      }];
      return object;
    }

    const object = [{ ...objects,
      text
    }];
    return object;
  } else if (file.startsWith('webUrl:')) {
    const weblink = file.replace('webUrl:', '');
    const object = [{ ...objects,
      weblink
    }];
    return object;
  } else {
    try {
      const files = JSON.parse(file);
      const object = [];

      for (let i = 0; i < files.length; i++) {
        const path = files[i].path;
        const obj = { ...objects,
          fileName: getFileName(path),
          extension: getExtension(path),
          mimeType: getMimeType(path),
          filePath: path,
          extra: files[i].extra
        };
        object.push(obj);
      }

      return object;
    } catch (error) {
      return [{ ...objects
      }];
    }
  }
}
//# sourceMappingURL=utils.js.map