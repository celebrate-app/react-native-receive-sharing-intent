package com.reactnativereceivesharingintent;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import com.facebook.react.bridge.*;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Objects;

public class ReceiveSharingIntentHelper {

  private Context context;

  public ReceiveSharingIntentHelper(Application context) {
    this.context = context;
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  public void sendFileNames(Context context, Intent intent, Promise promise) {
    try {
      String action = intent.getAction();
      String type = intent.getType();
      if (type == null) {
        return;
      }
      if (
        !type.startsWith("text") &&
        (
          Objects.equals(action, Intent.ACTION_SEND) ||
          Objects.equals(action, Intent.ACTION_SEND_MULTIPLE)
        )
      ) {
        WritableMap files = getMediaUris(intent, context);
        if (files == null) return;
        promise.resolve(files);
      } else if (
        type.startsWith("text") && Objects.equals(action, Intent.ACTION_SEND)
      ) {
        String text = null;
        String subject = null;
        try {
          text = intent.getStringExtra(Intent.EXTRA_TEXT);
          subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        } catch (Exception ignored) {}
        if (text == null) {
          WritableMap files = getMediaUris(intent, context);
          if (files == null) return;
          promise.resolve(files);
        } else {
          WritableMap files = new WritableNativeMap();
          WritableMap file = new WritableNativeMap();
          file.putString("contentUri", null);
          file.putString("filePath", null);
          file.putString("fileName", null);
          file.putString("extension", null);
          if (text.startsWith("http")) {
            file.putString("weblink", text);
            file.putString("text", null);
          } else {
            file.putString("weblink", null);
            file.putString("text", text);
          }
          file.putString("subject", subject);
          files.putMap("0", file);
          promise.resolve(files);
        }
      } else if (Objects.equals(action, Intent.ACTION_VIEW)) {
        String link = intent.getDataString();
        WritableMap files = new WritableNativeMap();
        WritableMap file = new WritableNativeMap();
        file.putString("contentUri", null);
        file.putString("filePath", null);
        file.putString("mimeType", null);
        file.putString("text", null);
        file.putString("weblink", link);
        file.putString("fileName", null);
        file.putString("extension", null);
        files.putMap("0", file);
        promise.resolve(files);
      } else if (Objects.equals(action, "android.intent.action.PROCESS_TEXT")) {
        String text = null;
        try {
          text = intent.getStringExtra(intent.EXTRA_PROCESS_TEXT);
        } catch (Exception e) {}
        WritableMap files = new WritableNativeMap();
        WritableMap file = new WritableNativeMap();
        file.putString("contentUri", null);
        file.putString("filePath", null);
        file.putString("fileName", null);
        file.putString("extension", null);
        file.putString("weblink", null);
        file.putString("text", text);
        files.putMap("0", file);
        promise.resolve(files);
      } else {
        WritableMap errorInfo = new WritableNativeMap();
        WritableMap fileInfo = new WritableNativeMap();
        fileInfo.putString("userInfo", action + "" + type);
        errorInfo.putMap("fileInfo", fileInfo);
        promise.reject("error", "file type is Invalid", errorInfo);
      }
    } catch (Exception e) {
      promise.reject("error", e.toString());
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  public WritableMap getMediaUris(Intent intent, Context context) {
    if (intent == null) return null;

    String subject = null;
    try {
      subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
    } catch (Exception ignored) {}

    WritableMap files = new WritableNativeMap();

    if (Objects.equals(intent.getAction(), Intent.ACTION_SEND)) {
      Uri contentUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
      if (contentUri == null) return null;
      WritableMap file = RNMediaInfoUtils.getCompleteMediaInfo(context, contentUri, subject);
      files.putMap("0", file);
    } else if (
      Objects.equals(intent.getAction(), Intent.ACTION_SEND_MULTIPLE)
    ) {
      ArrayList<Uri> contentUris = intent.getParcelableArrayListExtra(
        Intent.EXTRA_STREAM
      );
      if (contentUris != null) {
        int index = 0;
        for (Uri uri : contentUris) {
          WritableMap file = RNMediaInfoUtils.getCompleteMediaInfo(context, uri, subject);
          files.putMap(Integer.toString(index), file);

          index++;
        }
      }
    }

    return files;
  }

  public static Uri compatUriFromFile(final Context context, final File file) {
    Uri result = null;

    final String packageName = context.getApplicationContext().getPackageName();
    final String authority = new StringBuilder(packageName)
      .append(".provider")
      .toString();
    try {
      result = FileProvider.getUriForFile(context, authority, file);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    }

    return result;
  }

  @SuppressLint("NewApi")
  public static String getRealPathFromURI(
    final Context context,
    final Uri uri
  ) {
    // MediaStore (and general)
    if ("content".equalsIgnoreCase(uri.getScheme())) {
      // Return the remote address
      if (isGooglePhotosUri(uri)) return uri.getLastPathSegment();

      if (isFileProviderUri(context, uri)) return getFileProviderPath(
        context,
        uri
      );

      return getDataColumn(context, uri, null, null);
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return null;
  }

  private ExifInterface createExifInterface(String uri) throws Exception {
    if (uri.startsWith("content://")) {
      uri = getRealPathFromURI(this.context, Uri.parse(uri));
    }

    return new ExifInterface(uri);
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @param selection (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   */
  public static String getDataColumn(
    Context context,
    Uri uri,
    String selection,
    String[] selectionArgs
  ) {
    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = { column };

    try {
      cursor =
        context
          .getContentResolver()
          .query(uri, projection, selection, selectionArgs, null);
      if (cursor != null && cursor.moveToFirst()) {
        final int index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(index);
      }
    } finally {
      if (cursor != null) cursor.close();
    }
    return null;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(
        uri.getAuthority()
      );
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is Google Photos.
   */
  public static boolean isGooglePhotosUri(final Uri uri) {
    return "com.google.android.apps.photos.content".equals(uri.getAuthority());
  }

  /**
   * @param context The Application context
   * @param uri The Uri is checked by functions
   * @return Whether the Uri authority is FileProvider
   */
  public static boolean isFileProviderUri(
    final Context context,
    final Uri uri
  ) {
    final String packageName = context.getPackageName();
    final String authority = new StringBuilder(packageName)
      .append(".provider")
      .toString();
    return authority.equals(uri.getAuthority());
  }

  /**
   * @param context The Application context
   * @param uri The Uri is checked by functions
   * @return File path or null if file is missing
   */
  public static String getFileProviderPath(
    final Context context,
    final Uri uri
  ) {
    final File appDir = context.getExternalFilesDir(
      Environment.DIRECTORY_PICTURES
    );
    final File file = new File(appDir, uri.getLastPathSegment());
    return file.exists() ? file.toString() : null;
  }

  private String getMediaType(String url) {
    String mimeType = URLConnection.guessContentTypeFromName(url);
    return mimeType;
  }

  public void clearFileNames(Intent intent) {
    String type = intent.getType();
    if (type == null) return;
    if (type.startsWith("text")) {
      intent.removeExtra(Intent.EXTRA_TEXT);
    } else if (
      type.startsWith("image") ||
      type.startsWith("video") ||
      type.startsWith("application") ||
      // */* covers case when Video and Images were shared together
      type.startsWith("*/*")
    ) {
      intent.removeExtra(Intent.EXTRA_STREAM);
    }
  }

  public String getFileName(String file) {
    return file.substring(file.lastIndexOf('/') + 1);
  }

  public String getExtension(String file) {
    return file.substring(file.lastIndexOf('.') + 1);
  }
}
