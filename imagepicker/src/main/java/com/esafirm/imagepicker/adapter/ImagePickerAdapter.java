package com.esafirm.imagepicker.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.features.imageloader.ImageLoader;
import com.esafirm.imagepicker.features.imageloader.ImageType;
import com.esafirm.imagepicker.helper.ImagePickerUtils;
import com.esafirm.imagepicker.listeners.OnImageClickListener;
import com.esafirm.imagepicker.listeners.OnImageSelectedListener;
import com.esafirm.imagepicker.model.Image;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

public class ImagePickerAdapter extends BaseListAdapter<ImagePickerAdapter.ImageViewHolder> {

    private List<Image> mItems = new ArrayList<>();
//    private List<Image> mSelected = new ArrayList<>();

    private OnImageClickListener mItemClickListener;
    private OnImageSelectedListener mImageSelectedListener;
    private SelectionTracker<Image> mSelectionTracker;
    private boolean mEnabledSelectionUI = false;

    public ImagePickerAdapter(Context context, ImageLoader imageLoader,
                              List<Image> selectedImages, OnImageClickListener itemClickListener) {
        super(context, imageLoader);
        this.mItemClickListener = itemClickListener;

        if (selectedImages != null && !selectedImages.isEmpty()) {
            mSelectionTracker.setItemsSelected(selectedImages, true);
        }
    }

    public SelectionTracker getSelectionTracker() {
        return mSelectionTracker;
    }

    public void setSelectionTracker(SelectionTracker selectionTracker) {
        mSelectionTracker = selectionTracker;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ImageViewHolder(
                getInflater().inflate(R.layout.ef_imagepicker_item_image, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder viewHolder, int position) {
        final Image image = mItems.get(position);
//        final boolean isSelected = isSelected(image);
        final boolean isSelected = mSelectionTracker.isSelected(image);

        getImageLoader().loadImage(
                image.getPath(),
                viewHolder.imageView,
                ImageType.GALLERY
        );

        boolean showFileTypeIndicator = false;
        String fileTypeLabel = "";
        if (ImagePickerUtils.isGifFormat(image)) {
            fileTypeLabel = getContext().getResources().getString(R.string.ef_gif);
            showFileTypeIndicator = true;
        }
        if (ImagePickerUtils.isVideoFormat(image)) {
            fileTypeLabel = getContext().getResources().getString(R.string.ef_video);
            showFileTypeIndicator = true;
        }

        viewHolder.fileTypeIndicator.setText(fileTypeLabel);
        viewHolder.fileTypeIndicator.setVisibility(showFileTypeIndicator
                ? View.VISIBLE
                : View.GONE);

        viewHolder.alphaView.setAlpha(isSelected
                ? 0.5f
                : 0f);

        viewHolder.itemView.setOnClickListener(v -> {
//            mSelectionTracker.select(image);
//            boolean shouldSelect = mItemClickListener.onImageClick(
//                    isSelected
//            );

//            if (isSelected) {
//                removeSelectedImage(image, position);
//            } else if (shouldSelect) {
//                addSelected(image, position);
//            }
        });

        viewHolder.selection.setSelected(isSelected);
        viewHolder.selection.setVisibility((isSelected || mSelectionTracker.getSelection().size() > 0) ? View.VISIBLE : View.GONE);
//        viewHolder.container.setForeground(isSelected
//                ? ContextCompat.getDrawable(getContext(), R.drawable.ef_ic_done_white)
//                : null);
    }

    public List<Image> getItems() {
        return mItems;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public void setData(List<Image> images) {
        mItems.clear();
        mItems.addAll(images);
    }

    @Deprecated
    public void removeAllSelectedSingleClick() {
        mSelectionTracker.clearSelection();
//        mutateSelection(() -> {
//            mSelected.clear();
//            notifyDataSetChanged();
//        });
    }

    public void setImageSelectedListener(OnImageSelectedListener imageSelectedListener) {
        this.mImageSelectedListener = imageSelectedListener;
    }

    public Image getItem(int position) {
        return mItems.get(position);
    }

    @Deprecated
    public Selection<Image> getSelectedImages() {
        return mSelectionTracker.getSelection();
    }

    @Deprecated
    public boolean isSelected(Image image) {
//        for (Image selectedImage : mSelected) {
//            if (selectedImage.getPath().equals(image.getPath())) {
//                return true;
//            }
//        }
//        return false;
        return mSelectionTracker.isSelected(image);
    }

    public void enableSelections(boolean b) {
        if (mEnabledSelectionUI != b) {
            mEnabledSelectionUI = b;

            notifyItemRangeChanged(0, mItems.size());
        }

    }

//    public void addSelected(final Image image, final int position) {
//        mutateSelection(() -> {
//            mSelected.add(image);
//            notifyItemChanged(position);
//        });
//    }
//
//    public void removeSelectedImage(final Image image, final int position) {
//        mutateSelection(() -> {
//            mSelected.remove(image);
//            notifyItemChanged(position);
//        });
//    }
//
//    private void mutateSelection(Runnable runnable) {
//        runnable.run();
//        if (mImageSelectedListener != null) {
//            mImageSelectedListener.onSelectionUpdate(mSelected);
//        }
//    }

    interface ViewHolderWithDetails<T> {
        ItemDetailsLookup.ItemDetails<T> getItemDetails(ImagePickerAdapter adapter, int position);
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithDetails<Image> {
        private ImageView imageView;
        private View alphaView;
        private TextView fileTypeIndicator;
        public View selection;

        ImageViewHolder(View itemView) {
            super(itemView);
//
//            container = (ConstraintLayout) itemView;
            imageView = itemView.findViewById(R.id.image_view);
            alphaView = itemView.findViewById(R.id.view_alpha);
            selection = itemView.findViewById(R.id.image_selection);
            fileTypeIndicator = itemView.findViewById(R.id.ef_item_file_type_indicator);
        }

        @Override
        public ItemDetailsLookup.ItemDetails<Image> getItemDetails(final ImagePickerAdapter adapter, final int position) {
            return new ItemDetailsLookup.ItemDetails<Image>() {
                @Override
                public int getPosition() {
                    return position;
                }

                @Nullable
                @Override
                public Image getSelectionKey() {
                    return adapter.getItem(position);
                }
            };
        }
    }


}
