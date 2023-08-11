package com.reactnativereceivesharingintent;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableNativeMap;

import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RNMediaInfoUtils {

    private static final String TAG = "MEDIA_INFO_UTILS";

    private static int getExifOrientation(String path,Boolean isImage) {
        try {
            if(!isImage){
                Log.d(TAG, "Media is not an image. Returning ORIENTATION_UNDEFINED.");
                //If media is not image, return fallback value
                return ExifInterface.ORIENTATION_UNDEFINED;
            }
            ExifInterface exif = new ExifInterface(path);
            String orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            if(orientation == null){
                Log.d(TAG, "Orientation is null. Returning ORIENTATION_UNDEFINED.");
                //If media is not image, return fallback value
                return ExifInterface.ORIENTATION_UNDEFINED;
            }
            Log.d(TAG, "Exif orientation: " + orientation);
            return Integer.parseInt(orientation);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error getting exif orientation", e);
            return ExifInterface.ORIENTATION_UNDEFINED;
        }
    }

    private static Map<String, String> getMediaInfo(Context context, Uri contentUri) {
        Map<String, String> resultMap = new HashMap<>();
        ContentResolver contentResolver = context.getContentResolver();
        try {
            Cursor queryCursor = contentResolver.query(contentUri, null, null, null, null);

            if (queryCursor != null) {
                queryCursor.moveToFirst();
                int displayNameIndex = queryCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = queryCursor.getColumnIndex(OpenableColumns.SIZE);
                int dateTakenIndex = queryCursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN);
                int dateModifiedIndex = queryCursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED);

                if (displayNameIndex != -1) {
                    resultMap.put("fileName", queryCursor.getString(displayNameIndex));
                }
                if (sizeIndex != -1) {
                    resultMap.put("fileSize", queryCursor.getString(sizeIndex));
                }
                if (dateTakenIndex != -1) {
                    resultMap.put("DateTimeTaken", queryCursor.getString(dateTakenIndex));
                }
                if(dateModifiedIndex != -1){
                    resultMap.put("DateTimeModified", queryCursor.getString(dateModifiedIndex));
                }
                queryCursor.close();
            }
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error getting media info", e);
            return new HashMap<>();
        }

    }

    private static Map<String, String> getMediaDimensions(Context context, Uri contentUri, Boolean isImage) {
        try {
            Map<String, String> resultMap = new HashMap<>();
            String[] projection = {
                    MediaStore.MediaColumns.WIDTH,
                    MediaStore.MediaColumns.HEIGHT
            };
            ContentResolver contentResolver = context.getContentResolver();
            Cursor cursor = contentResolver.query(contentUri, projection, null, null, null);

            if (cursor != null) {
                cursor.moveToFirst();
                try {
                    if (!cursor.isNull(0) && !cursor.isNull(1)) {  // check if dimensions columns are not null
                        resultMap.put("width", cursor.getString(0));
                        resultMap.put("height", cursor.getString(1));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cursor.close();
            }

            // If we didn't get dimensions from MediaStore and the media is an image, try ExifInterface
            if (!resultMap.containsKey("width") && !resultMap.containsKey("height") && isImage) {
                try {
                    ExifInterface exif = new ExifInterface(getAbsolutePath(context,contentUri));
                    String width = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
                    String height = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);

                    if (width != null && height != null ) {
                        resultMap.put("width",width);
                        resultMap.put("height",height);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error getting media dimensions", e);
            return new HashMap<>();
        }
    }

    // TODO: Expand with MediaMetadataRetriever for video metadata
    // https://developer.android.com/reference/android/media/MediaMetadataRetriever
    public static WritableMap getCompleteMediaInfo(Context context, Uri contentUri, String subject) {
        Log.d(TAG, "Starting getCompleteMediaInfo with URI: " + contentUri.toString() + " and subject: " + subject);

        WritableMap completeInfoMap = new WritableNativeMap();
        WritableMap exifMap = new WritableNativeMap();
        try {
            String absolutePath = getAbsolutePath(context, contentUri);
            completeInfoMap.putString("filePath", absolutePath);
            completeInfoMap.putString("contentUri", contentUri.toString());

            // Other string mappings
            completeInfoMap.putString("text", null);
            completeInfoMap.putString("weblink", null);
            completeInfoMap.putString("subject", subject);

            String mimeType = context.getContentResolver().getType(contentUri);

            if (mimeType != null) {
                completeInfoMap.putString("mimeType", mimeType);
            }

            Boolean isImage = mimeType != null && mimeType.startsWith("image/");

            // Fetch Media Info
            Map<String, String> mediaInfo = getMediaInfo(context, contentUri);
            if (mediaInfo.containsKey("fileName")) {
                completeInfoMap.putString("fileName", mediaInfo.get("fileName"));
            }
            if (mediaInfo.containsKey("fileSize")) {
                completeInfoMap.putString("fileSize", mediaInfo.get("fileSize"));
            }
            if (mediaInfo.containsKey("DateTimeTaken")) {
                exifMap.putString("DateTimeTaken", mediaInfo.get("DateTimeTaken"));
            }
            if (mediaInfo.containsKey("DateTimeModified")) {
                exifMap.putString("DateTimeModified", mediaInfo.get("DateTimeModified"));
            }

            // Fetch Media Dimensions
            Map<String, String> dimensions = getMediaDimensions(context, contentUri, isImage);
            if (dimensions.containsKey("width")) {
                exifMap.putString("width", dimensions.get("width"));
            }
            if (dimensions.containsKey("height")) {
                exifMap.putString("height", dimensions.get("height"));
            }

            // Fetch Media Orientation
            int orientation = getExifOrientation(getAbsolutePath(context,contentUri), isImage);
            exifMap.putInt("orientation", orientation);


            completeInfoMap.putMap("exif", exifMap);
            return completeInfoMap;
        } catch (Exception e) {
            e.printStackTrace();
            return new WritableNativeMap();
        }
    }

    private static String getAbsolutePath(Context context, Uri uri) {
        String path = FileDirectory.INSTANCE.getAbsolutePath(context, uri);
        Log.d(TAG, "Resolved absolute path: " + path);
        return path;
    }
}
