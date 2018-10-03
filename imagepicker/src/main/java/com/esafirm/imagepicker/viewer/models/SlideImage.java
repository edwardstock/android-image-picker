package com.esafirm.imagepicker.viewer.models;


import android.net.Uri;

import com.annimon.stream.Objects;

import org.parceler.Parcel;

/**
 * Dogsy. 2018
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Parcel
public class SlideImage {
    String mOriginUrl;
    String mPreviewUrl;

    public SlideImage(SlideImageContainer container) {
        this(container.getOriginUrl(), container.getPreviewUrl());
    }

    public SlideImage(String originUrl, String previewUrl) {
        this(originUrl);
        mPreviewUrl = previewUrl;
    }

    public SlideImage(String originUrl) {
        mOriginUrl = originUrl;
    }

    public SlideImage(Uri uri) {
        this(uri.getPath());
    }

    SlideImage() {
    }

    @Override
    public int hashCode() {
        return Objects.hash(mOriginUrl, mPreviewUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlideImage that = (SlideImage) o;
        return Objects.equals(mOriginUrl, that.mOriginUrl) &&
                Objects.equals(mPreviewUrl, that.mPreviewUrl);
    }

    public String getUrl() {
        return mOriginUrl;
    }

    public boolean hasPreview() {
        return getPreviewUrl() != null;
    }

    public String getPreviewUrl() {
        return mPreviewUrl;
    }

    public interface SlideImageContainer {
        String getPreviewUrl();
        String getOriginUrl();
    }

}
