package com.esafirm.imagepicker.adapter;

import android.content.Context;
import android.view.LayoutInflater;

import com.esafirm.imagepicker.features.imageloader.ImageLoader;

import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseListAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final ImageLoader mImageLoader;

    public BaseListAdapter(Context context, ImageLoader imageLoader) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mImageLoader = imageLoader;
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public Context getContext() {
        return mContext;
    }

    public LayoutInflater getInflater() {
        return mInflater;
    }
}
