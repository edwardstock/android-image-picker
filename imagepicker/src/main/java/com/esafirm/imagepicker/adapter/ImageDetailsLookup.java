package com.esafirm.imagepicker.adapter;

import android.view.MotionEvent;
import android.view.View;

import com.esafirm.imagepicker.model.Image;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

/**
 * android-image-picker. 2018
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class ImageDetailsLookup extends ItemDetailsLookup<Image> {
    private WeakReference<RecyclerView> mRecyclerView;

    public ImageDetailsLookup(RecyclerView recyclerView) {
        mRecyclerView = new WeakReference<>(recyclerView);
    }

    @Nullable
    @Override
    public ItemDetails<Image> getItemDetails(@NonNull MotionEvent e) {
        View view = mRecyclerView.get().findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            RecyclerView.ViewHolder viewHolder = mRecyclerView.get().getChildViewHolder(view);
            if (viewHolder instanceof ImagePickerAdapter.ViewHolderWithDetails) {
                int position = viewHolder.getAdapterPosition();
                return ((ImagePickerAdapter.ViewHolderWithDetails<Image>) viewHolder).getItemDetails(((ImagePickerAdapter) mRecyclerView.get().getAdapter()), position);
            }
        }

        return null;

    }
}
