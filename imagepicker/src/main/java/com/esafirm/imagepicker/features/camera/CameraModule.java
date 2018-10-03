package com.esafirm.imagepicker.features.camera;

import android.content.Context;
import android.content.Intent;

import com.esafirm.imagepicker.features.common.BaseConfig;

public interface CameraModule {
    Intent getCameraPhotoIntent(Context context, BaseConfig config);
    Intent getCameraVideoIntent(Context context, BaseConfig config);
    void getImage(Context context, Intent intent, OnImageReadyListener imageReadyListener);
    void getVideo(Context context, Intent intent, OnImageReadyListener imageReadyListener);
    void removeCaptured();
}
