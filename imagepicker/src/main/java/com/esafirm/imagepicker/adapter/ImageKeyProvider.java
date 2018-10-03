package com.esafirm.imagepicker.adapter;

import com.esafirm.imagepicker.model.Image;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;

/**
 * android-image-picker. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class ImageKeyProvider extends ItemKeyProvider<Image> {
    private LazyValue<List<Image>> mImages;

    /**
     * Creates a new provider with the given scope.
     * @param scope Scope can't be changed at runtime.
     */
    public ImageKeyProvider(ImagePickerAdapter imagePickerAdapter, int scope) {
        super(scope);
        mImages = imagePickerAdapter::getItems;
    }

    @Nullable
    @Override
    public Image getKey(int position) {
        return mImages.get().get(position);
    }

    @Override
    public int getPosition(@NonNull Image key) {
        return mImages.get().indexOf(key);
    }
}
