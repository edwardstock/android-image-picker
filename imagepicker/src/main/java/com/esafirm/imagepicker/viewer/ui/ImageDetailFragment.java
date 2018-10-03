package com.esafirm.imagepicker.viewer.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.chrisbanes.photoview.PhotoView;
import com.squareup.picasso.Callback;

import org.parceler.Parcels;

import me.dogsy.app.R;
import me.dogsy.app.internal.app.Dogsy;
import me.dogsy.app.internal.helpers.ShowHideViewRequestListener;
import me.dogsy.app.slider.models.SlideImage;
import me.dogsy.app.slider.views.TouchImageViewPager;
import timber.log.Timber;

import static me.dogsy.app.internal.app.Dogsy.app;
import static me.dogsy.app.internal.helpers.ExceptionHelper.doubleTryOOM;

/**
 * Dogsy. 2017
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */

public class ImageDetailFragment extends Fragment {
    final static String ARG_IMAGE = "ARG_IMAGE";
    final static String ARG_POSITION = "ARG_POSITION";

    public static ImageDetailFragment newInstance(final SlideImage image, int position) {
        final ImageDetailFragment in = new ImageDetailFragment();
        final Bundle args = new Bundle();

        in.setArguments(args);

        return in;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof ImageViewerActivity)) {
            throw new RuntimeException(
                    getClass().getSimpleName() + " can be attached only to " + ImageViewerActivity.class.getSimpleName());
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pager_item_image, container, false);

        final PhotoView photoView = view.findViewById(R.id.image);
        final View progress = view.findViewById(R.id.progress);


        assert getArguments() != null;
        assert getActivity() != null;
        final ImageViewerActivity activity = ((ImageViewerActivity) getActivity());

        final SlideImage image = Parcels.unwrap(getArguments().getParcelable(ARG_IMAGE));
        final int position = getArguments().getInt(ARG_POSITION);
        photoView.setTag(TouchImageViewPager.VIEW_PAGER_OBJECT_TAG + String.valueOf(position));
        photoView.setOnClickListener(v -> {
            if (!activity.mHiddenControls) {
                activity.hideSystemUI();
            } else {
                activity.showSystemUI();
            }
            activity.mHiddenControls = !activity.mHiddenControls;
        });

        String transitionName = getResources().getString(R.string.transition_image_slide);
        ViewCompat.setTransitionName(photoView, transitionName + String.valueOf(position));

        progress.setVisibility(View.VISIBLE);

        if (image == null) {
            progress.setVisibility(View.GONE);
            photoView.setImageResource(R.drawable.ic_error_accent_24dp);
            Timber.w("Slide[%d]: image not found", position);
            return view;
        }

        Timber.d("Load slide[%d]: %s", position, image.getUrl());

        doubleTryOOM(() -> {
            app().image()
                    .load(image.getUrl())
                    .resize(Dogsy.app().display().getWidth(), Dogsy.app().display().getHeight())
                    .onlyScaleDown()
                    .centerInside()
                    .into(photoView, new ShowHideViewRequestListener(progress, new Callback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(Exception e) {
                            Timber.d(e);
                            photoView.setImageResource(R.drawable.ic_error_accent_24dp);
                        }
                    }));
        }, t -> photoView.setImageResource(R.drawable.ic_error_accent_24dp), String.format("Unable to load image %s", image.getUrl()));


        return view;
    }
}
