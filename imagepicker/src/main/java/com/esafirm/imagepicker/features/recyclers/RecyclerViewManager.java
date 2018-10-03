package com.esafirm.imagepicker.features.recyclers;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.view.View;
import android.widget.Toast;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.adapter.FolderPickerAdapter;
import com.esafirm.imagepicker.adapter.ImageDetailsLookup;
import com.esafirm.imagepicker.adapter.ImageKeyProvider;
import com.esafirm.imagepicker.adapter.ImagePickerAdapter;
import com.esafirm.imagepicker.features.ImagePickerConfig;
import com.esafirm.imagepicker.features.ReturnMode;
import com.esafirm.imagepicker.features.imageloader.ImageLoader;
import com.esafirm.imagepicker.helper.ConfigUtils;
import com.esafirm.imagepicker.helper.ImagePickerUtils;
import com.esafirm.imagepicker.listeners.OnFolderClickListener;
import com.esafirm.imagepicker.listeners.OnImageClickListener;
import com.esafirm.imagepicker.listeners.OnImageSelectedListener;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;
import com.esafirm.imagepicker.view.GridSpacingItemDecoration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import static com.esafirm.imagepicker.features.IpCons.MAX_LIMIT;
import static com.esafirm.imagepicker.features.IpCons.MODE_MULTIPLE;
import static com.esafirm.imagepicker.features.IpCons.MODE_SINGLE;

public class RecyclerViewManager {

    private final WeakReference<Context> mContext;
    private final WeakReference<RecyclerView> mRecyclerView;
    private final ImagePickerConfig mConfig;

    private GridLayoutManager mLayoutManager;
    private GridSpacingItemDecoration mItemOffsetDecoration;

    private ImagePickerAdapter mImageAdapter;
    private FolderPickerAdapter mFolderAdapter;

    private Parcelable mFoldersState;

    private int mImageColumns;
    private int mFolderColumns;

    private SelectionTracker<Image> mSelectionTracker;
    private boolean mLastSelectionState = false;

    public RecyclerViewManager(RecyclerView recyclerView, ImagePickerConfig config, int orientation) {
        mRecyclerView = new WeakReference<>(recyclerView);
        mConfig = config;
        mContext = new WeakReference<>(recyclerView.getContext());
        changeOrientation(orientation);
    }

    /**
     * Set item size, column size base on the screen orientation
     */
    public void changeOrientation(int orientation) {
        mImageColumns = orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5;
        mFolderColumns = orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 4;

        boolean shouldShowFolder = mConfig.isFolderMode() && isDisplayingFolderView();
        int columns = shouldShowFolder ? mFolderColumns : mImageColumns;
        mLayoutManager = new GridLayoutManager(mContext.get(), columns);
        mRecyclerView.get().setLayoutManager(mLayoutManager);
        mRecyclerView.get().setHasFixedSize(true);
        setItemDecoration(columns);
    }

    public void setupAdapters(OnImageClickListener onImageClickListener, OnFolderClickListener onFolderClickListener) {
        ArrayList<Image> selectedImages = null;
        if (mConfig.getMode() == MODE_MULTIPLE && !mConfig.getSelectedImages().isEmpty()) {
            selectedImages = mConfig.getSelectedImages();
        }

        /* Init folder and image adapter */
        final ImageLoader imageLoader = mConfig.getImageLoader();
        mImageAdapter = new ImagePickerAdapter(mContext.get(), imageLoader, selectedImages, onImageClickListener);
        mFolderAdapter = new FolderPickerAdapter(mContext.get(), imageLoader, bucket -> {
            mFoldersState = mRecyclerView.get().getLayoutManager().onSaveInstanceState();
            onFolderClickListener.onFolderClick(bucket);
        });
    }

    public void handleBack(OnBackAction action) {
        if (mConfig.isFolderMode() && !isDisplayingFolderView()) {
            setFolderAdapter(null);
            action.onBackToFolder();
            return;
        }
        action.onFinishImagePicker();
    }

    public String getTitle() {
        if (isDisplayingFolderView()) {
            return ConfigUtils.getFolderTitle(mContext.get(), mConfig);
        }

        if (mConfig.getMode() == MODE_SINGLE) {
            return ConfigUtils.getImageTitle(mContext.get(), mConfig);
        }

        final int imageSize = mImageAdapter.getSelectedImages().size();
        final boolean useDefaultTitle = !ImagePickerUtils.isStringEmpty(mConfig.getImageTitle()) && imageSize == 0;

        if (useDefaultTitle) {
            return ConfigUtils.getImageTitle(mContext.get(), mConfig);
        }
        return mConfig.getLimit() == MAX_LIMIT
                ? String.format(mContext.get().getString(R.string.ef_selected), imageSize)
                : String.format(mContext.get().getString(R.string.ef_selected_with_limit), imageSize, mConfig.getLimit());
    }

    public void setImageAdapter(List<Image> images) {
        mImageAdapter.setData(images);
        setItemDecoration(mImageColumns);
        mRecyclerView.get().setAdapter(mImageAdapter);
        mSelectionTracker = new SelectionTracker.Builder<>(
                "picker",
                mRecyclerView.get(),
                new ImageKeyProvider(mImageAdapter, ItemKeyProvider.SCOPE_MAPPED),
                new ImageDetailsLookup(mRecyclerView.get()),
                StorageStrategy.createParcelableStorage(Image.class)
        )
                .build();
        mImageAdapter.setSelectionTracker(mSelectionTracker);

        mSelectionTracker.addObserver(new SelectionTracker.SelectionObserver<Image>() {
            @Override
            public void onItemStateChanged(@NonNull Image key, boolean selected) {
                super.onItemStateChanged(key, selected);
                final boolean hasSelection = mSelectionTracker.hasSelection();
                if (mLastSelectionState != hasSelection) {
                    mLastSelectionState = hasSelection;
                    for (int i = 0; i < mImageAdapter.getItemCount(); i++) {
                        ImagePickerAdapter.ImageViewHolder holder = (ImagePickerAdapter.ImageViewHolder) mRecyclerView.get().findViewHolderForAdapterPosition(i);
                        if (holder != null) {
                            holder.selection.setVisibility(mLastSelectionState ? View.VISIBLE : View.GONE);
                        }
                    }
                }

//                mRecyclerView.get().post(()->{
//                    mImageAdapter.enableSelections(mSelectionTracker.hasSelection());
//                });
                Timber.d("OnSelection state changed: %b", selected);
            }

            @Override
            public void onSelectionRefresh() {
                super.onSelectionRefresh();
                Timber.d("OnSelection refresh");
            }

            @Override
            public void onSelectionChanged() {
                super.onSelectionChanged();
                Timber.d("OnSelection changed");
            }

            @Override
            public void onSelectionRestored() {
                super.onSelectionRestored();
                Timber.d("OnSelection restored");
            }
        });
    }

    public void setFolderAdapter(List<Folder> folders) {
        mFolderAdapter.setData(folders);
        setItemDecoration(mFolderColumns);
        mRecyclerView.get().setAdapter(mFolderAdapter);

        if (mFoldersState != null) {
            mLayoutManager.setSpanCount(mFolderColumns);
            mRecyclerView.get().getLayoutManager().onRestoreInstanceState(mFoldersState);
        }
    }

    public Selection<Image> getSelectedImages() {
        checkAdapterIsInitialized();
        return mImageAdapter.getSelectedImages();
    }

    public void setImageSelectedListener(OnImageSelectedListener listener) {
        checkAdapterIsInitialized();
        mImageAdapter.setImageSelectedListener(listener);
    }

    /* --------------------------------------------------- */
    /* > Images */
    /* --------------------------------------------------- */

    public boolean selectImage(boolean isSelected) {
        if (mConfig.getMode() == MODE_MULTIPLE) {
            if (mImageAdapter.getSelectedImages().size() >= mConfig.getLimit() && !isSelected) {
                Toast.makeText(mContext.get(), R.string.ef_msg_limit_images, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else if (mConfig.getMode() == MODE_SINGLE) {
            if (mImageAdapter.getSelectedImages().size() > 0) {
                mImageAdapter.removeAllSelectedSingleClick();
            }
        }
        return true;
    }

    public boolean isShowDoneButton() {
        return !isDisplayingFolderView()
                && !mImageAdapter.getSelectedImages().isEmpty()
                && (mConfig.getReturnMode() != ReturnMode.ALL && mConfig.getReturnMode() != ReturnMode.GALLERY_ONLY);
    }

    public SelectionTracker<Image> getSelectionTracker() {
        return mSelectionTracker;
    }

    private void setItemDecoration(int columns) {
        if (mItemOffsetDecoration != null) {
            mRecyclerView.get().removeItemDecoration(mItemOffsetDecoration);
        }
        mItemOffsetDecoration = new GridSpacingItemDecoration(
                columns,
                mContext.get().getResources().getDimensionPixelSize(R.dimen.ef_item_padding),
                false
        );
        mRecyclerView.get().addItemDecoration(mItemOffsetDecoration);

        mLayoutManager.setSpanCount(columns);
    }

    private boolean isDisplayingFolderView() {
        return mRecyclerView.get().getAdapter() == null || mRecyclerView.get().getAdapter() instanceof FolderPickerAdapter;
    }

    private void checkAdapterIsInitialized() {
        if (mImageAdapter == null) {
            throw new IllegalStateException("Must call setupAdapters first!");
        }
    }

}
