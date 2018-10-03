package com.esafirm.imagepicker.viewer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.helper.ActivityBuilder;
import com.esafirm.imagepicker.model.Image;
import com.github.chrisbanes.photoview.PhotoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Dogsy. 2017
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ImageViewerActivity extends AppCompatActivity {

    public final static String EXTRA_IMAGE = "EXTRA_IMAGE";
    boolean mHiddenControls = false;
    boolean mHasTitle = false;
    private Toolbar mToolbar;
    private PhotoView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ef_activity_image_viewer);
        mToolbar = findViewById(R.id.toolbar);
        mImageView = findViewById(R.id.image);

//        supportPostponeEnterTransition();
        setupToolbar(mToolbar);

        final Image image = getIntent().getParcelableExtra(EXTRA_IMAGE);
        mImageView.setImageURI(Uri.parse(image.getPath()));
        mImageView.setOnClickListener(v -> {
            if (!mHiddenControls) {
                hideSystemUI();
            } else {
                showSystemUI();
            }
            mHiddenControls = !mHiddenControls;
        });

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == View.VISIBLE) {
                mToolbar.startAnimation(AnimationUtils.loadAnimation(ImageViewerActivity.this, R.anim.ef_slide_down));
                if (getSupportActionBar() != null) {
                    getSupportActionBar().show();
                }

            } else {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().hide();
                }
                mToolbar.startAnimation(AnimationUtils.loadAnimation(ImageViewerActivity.this, R.anim.ef_slide_up));
            }
        });

        showSystemUI();
        final ViewGroup.MarginLayoutParams lp = ((ViewGroup.MarginLayoutParams) mToolbar.getLayoutParams());
//        lp.topMargin = Dogsy.app().display().getStatusBarHeight();
//        mToolbar.setLayoutParams(lp);
    }

    private void setupToolbar(@NonNull final Toolbar toolbar) {
        checkNotNull(toolbar, "Toolbar can't be null!");
        setSupportActionBar(toolbar);

        assert (getSupportActionBar() != null);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setTitle(String title) {
        if (title != null && !title.isEmpty()) {
            mHasTitle = true;
            mToolbar.setTitle(title);
            super.setTitle(title);
        }
    }

    private void showSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

    }

    private void hideSystemUI() {
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN; // hide status bar

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE;
        }

        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    public static class Builder extends ActivityBuilder {
        private Image mImage;

        public Builder(@NonNull Activity from, Image image) {
            super(from);
            mImage = image;
        }

        public Builder(Fragment from, Image image) {
            super(from);
            mImage = image;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(EXTRA_IMAGE, mImage);
        }

        @Override
        protected Class<?> getActivityClass() {
            return ImageViewerActivity.class;
        }
    }
}
