package com.esafirm.imagepicker.features.camera;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.provider.MediaStore;

import com.esafirm.imagepicker.features.common.BaseConfig;
import com.esafirm.imagepicker.helper.ImagePickerUtils;
import com.esafirm.imagepicker.model.ImageFactory;

import java.io.File;
import java.io.Serializable;
import java.util.Locale;

import androidx.core.content.FileProvider;
import timber.log.Timber;

public class DefaultCameraModule implements CameraModule, Serializable {
    private String mCurrentMediaPath;

    @Override
    public Intent getCameraPhotoIntent(Context context, BaseConfig config) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = ImagePickerUtils.createImageFile(config.getImageDirectory());
        if (imageFile != null) {
            Context appContext = context.getApplicationContext();
            String providerName = String.format(Locale.getDefault(), "%s%s", appContext.getPackageName(), ".imagepicker.provider");
            Uri uri = FileProvider.getUriForFile(appContext, providerName, imageFile);
            mCurrentMediaPath = "file:" + imageFile.getAbsolutePath();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            ImagePickerUtils.grantAppPermission(context, intent, uri);

            return intent;
        }
        return null;
    }

    @Override
    public Intent getCameraVideoIntent(Context context, BaseConfig config) {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        File videoFile = ImagePickerUtils.createVideoFile(config.getImageDirectory());
        if (videoFile != null) {
            Context appContext = context.getApplicationContext();
            String providerName = String.format(Locale.getDefault(), "%s%s", appContext.getPackageName(), ".imagepicker.provider");
            Uri uri = FileProvider.getUriForFile(appContext, providerName, videoFile);
            mCurrentMediaPath = "file:" + videoFile.getAbsolutePath();
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

            ImagePickerUtils.grantAppPermission(context, intent, uri);

            return intent;
        }
        return null;
    }

    @Override
    public void getVideo(final Context context, Intent intent, final OnImageReadyListener imageReadyListener) {
        if (imageReadyListener == null) {
            throw new IllegalStateException("OnImageReadyListener must not be null");
        }

        if (mCurrentMediaPath == null) {
            Timber.w("mCurrentMediaPath null. " +
                    "This happen if you haven't call #getCameraVideoIntent() or the activity is being recreated");
            imageReadyListener.onImageReady(null);
            return;
        }

        final Uri videoUri = Uri.parse(mCurrentMediaPath);
        if (videoUri != null) {
            MediaScannerConnection.scanFile(context.getApplicationContext(),
                    new String[]{videoUri.getPath()}, null, (path, uri) -> {

                        Timber.d("File " + path + " was scanned successfully: " + uri);

                        if (path == null) {
                            Timber.d("This should not happen, go back to Immediate implemenation");
                            path = mCurrentMediaPath;
                        }

                        imageReadyListener.onImageReady(ImageFactory.singleListFromPath(path));
                        ImagePickerUtils.revokeAppPermission(context, videoUri);
                    });
        }
    }

    @Override
    public void getImage(final Context context, Intent intent, final OnImageReadyListener imageReadyListener) {
        if (imageReadyListener == null) {
            throw new IllegalStateException("OnImageReadyListener must not be null");
        }

        if (mCurrentMediaPath == null) {
            Timber.w("mCurrentMediaPath null. " +
                    "This happen if you haven't call #getCameraPhotoIntent() or the activity is being recreated");
            imageReadyListener.onImageReady(null);
            return;
        }

        final Uri imageUri = Uri.parse(mCurrentMediaPath);
        if (imageUri != null) {
            MediaScannerConnection.scanFile(context.getApplicationContext(),
                    new String[]{imageUri.getPath()}, null, (path, uri) -> {

                        Timber.d("File " + path + " was scanned successfully: " + uri);

                        if (path == null) {
                            Timber.d("This should not happen, go back to Immediate implemenation");
                            path = mCurrentMediaPath;
                        }

                        imageReadyListener.onImageReady(ImageFactory.singleListFromPath(path));
                        ImagePickerUtils.revokeAppPermission(context, imageUri);
                    });
        }
    }

    @Override
    public void removeCaptured() {
        if (mCurrentMediaPath != null) {
            File file = new File(mCurrentMediaPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
