package com.esafirm.imagepicker.features.imageloader;

import android.widget.ImageView;

import java.io.Serializable;

public interface ImageLoader extends Serializable {
    /**
     * Load image by uri into image view
     * @param path image uri
     * @param imageView image view
     * @param imageType type of image (folder or gallery)
     */
    void loadImage(String path, ImageView imageView, ImageType imageType);
}
