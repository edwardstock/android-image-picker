package com.esafirm.imagepicker.features;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.features.camera.DefaultCameraModule;
import com.esafirm.imagepicker.features.common.BaseConfig;
import com.esafirm.imagepicker.features.common.BasePresenter;
import com.esafirm.imagepicker.features.common.ImageLoaderListener;
import com.esafirm.imagepicker.helper.ConfigUtils;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.recyclerview.selection.SelectionTracker;

class ImagePickerPresenter extends BasePresenter<ImagePickerView> {

    private ImageFileLoader imageLoader;
    private DefaultCameraModule cameraModule;
    private Handler main = new Handler(Looper.getMainLooper());

    ImagePickerPresenter(ImageFileLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    DefaultCameraModule getCameraModule() {
        if (cameraModule == null) {
            cameraModule = new DefaultCameraModule();
        }
        return cameraModule;
    }

    /* Set the camera module in onRestoreInstance */
    void setCameraModule(DefaultCameraModule cameraModule) {
        this.cameraModule = cameraModule;
    }

    void abortLoad() {
        imageLoader.abortLoadImages();
    }

    void loadImages(ImagePickerConfig config) {
        if (!isViewAttached()) return;

        boolean isFolder = config.isFolderMode();
        boolean includeVideo = config.isIncludeVideo();
        boolean includePhotos = config.isIncludePhotos();

        ArrayList<File> excludedImages = config.getExcludedImages();

        runOnUiIfAvailable(() -> getView().showLoading(true));

        imageLoader.loadDeviceImages(isFolder, includeVideo, includePhotos, excludedImages, new ImageLoaderListener() {
            @Override
            public void onImageLoaded(final List<Image> images, final List<Folder> folders) {
                runOnUiIfAvailable(() -> {
                    getView().showFetchCompleted(images, folders);

                    final boolean isEmpty = folders != null
                            ? folders.isEmpty()
                            : images.isEmpty();

                    if (isEmpty) {
                        getView().showEmpty();
                    } else {
                        getView().showLoading(false);
                    }
                });
            }

            @Override
            public void onImageFailed(final Throwable throwable) {
                runOnUiIfAvailable(() -> getView().showError(throwable));
            }
        });
    }

    void onDoneSelectImages(SelectionTracker<Image> selectionTracker) {
        if (selectionTracker != null && selectionTracker.getSelection().size() > 0) {
            final Iterator<Image> iter = selectionTracker.getSelection().iterator();

            /* Scan selected images which not existed */
            List<Image> output = new ArrayList<>(selectionTracker.getSelection().size());
            while (iter.hasNext()) {
                Image img = iter.next();
                File file = new File(img.getPath());
                if (!file.exists()) {
                    selectionTracker.deselect(img);
                } else {
                    output.add(img);
                }
            }
            getView().finishPickImages(output);
        }
    }

    void captureVideo(Activity activity, BaseConfig config, int requestCode) {
        Context context = activity.getApplicationContext();
        Intent intent = getCameraModule().getCameraVideoIntent(activity, config);
        if (intent == null) {
            Toast.makeText(context, context.getString(R.string.ef_error_create_video_file), Toast.LENGTH_LONG).show();
            return;
        }
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            Toast.makeText(context, context.getString(R.string.ef_error_create_video_file), Toast.LENGTH_LONG).show();
        }
    }

    void captureImage(Activity activity, BaseConfig config, int requestCode) {
        Context context = activity.getApplicationContext();
        Intent intent = getCameraModule().getCameraPhotoIntent(activity, config);
        if (intent == null) {
            Toast.makeText(context, context.getString(R.string.ef_error_create_image_file), Toast.LENGTH_LONG).show();
            return;
        }
        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivityForResult(intent, requestCode);
        } else {
            Toast.makeText(context, context.getString(R.string.ef_error_create_image_file), Toast.LENGTH_LONG).show();
        }

    }

    void finishCaptureImage(Context context, Intent data, final BaseConfig config) {
        getCameraModule().getImage(context, data, images -> {
            if (ConfigUtils.shouldReturn(config, true)) {
                getView().finishPickImages(images);
            } else {
                getView().showCapturedImage();
            }
        });
    }

    void finishCaptureVideo(Context context, Intent data, final BaseConfig config) {
        getCameraModule().getVideo(context, data, videos -> {
            if (ConfigUtils.shouldReturn(config, true)) {
                getView().finishPickImages(videos);
            } else {
                getView().showCapturedImage();
            }
        });
    }

    void abortCaptureVideo() {
        getCameraModule().removeCaptured();
    }

    void abortCaptureImage() {
        getCameraModule().removeCaptured();
    }

    private void runOnUiIfAvailable(Runnable runnable) {
        main.post(() -> {
            if (isViewAttached()) {
                runnable.run();
            }
        });
    }
}
