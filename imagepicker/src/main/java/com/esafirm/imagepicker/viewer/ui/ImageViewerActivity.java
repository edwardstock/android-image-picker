package com.esafirm.imagepicker.viewer.ui;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.annimon.stream.Stream;
import com.arellomobile.mvp.presenter.InjectPresenter;
import com.arellomobile.mvp.presenter.ProvidePresenter;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.dogsy.app.BuildConfig;
import me.dogsy.app.R;
import me.dogsy.app.dogs.models.Dog;
import me.dogsy.app.internal.BaseMvpInjectActivity;
import me.dogsy.app.internal.app.Dogsy;
import me.dogsy.app.internal.common.DeferredCall;
import me.dogsy.app.internal.system.ActivityBuilder;
import me.dogsy.app.sitters.models.Sitter;
import me.dogsy.app.slider.ImageSliderModule;
import me.dogsy.app.slider.models.SlideImage;
import me.dogsy.app.slider.views.ImageSliderPresenter;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
public class ImageViewerActivity extends AppCompatActivity {

    public final static String EXTRA_URI = "EXTRA_URI";

    Toolbar toolbar;
    boolean mHiddenControls = false;
    boolean mHasTitle = false;


    private void setTitle(String title) {
        if (title != null && !title.isEmpty()) {
            mHasTitle = true;
            toolbar.setTitle(title);
            super.setTitle(title);
        }
    }

    private void setSubtitle(int from, int to) {
        if (!mHasTitle) {

            setTitle(String.format(Locale.getDefault(), "%d из %d", from, to));
        } else {
            toolbar.setSubtitle(String.format(Locale.getDefault(), "%d из %d", from, to));
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

//        toolbar.animate()
//                .alpha(0F).setDuration(300)
//                .setInterpolator(new AccelerateInterpolator())
//                .start();
    }

    @Override
    public void setOnImageDownloadListener(MenuItem.OnMenuItemClickListener listener) {
        mOnImageDownloadListener = listener;
    }

    @Override
    public void setOnImageAbuseListener(MenuItem.OnMenuItemClickListener listener) {
        mOnImageAbuseListener = listener;
    }

    @Override
    public void setEnableAbuse(boolean enable) {
        mMenuDefer.call(menu -> {
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getItemId() == R.id.image_abuse) {
                    menu.getItem(i).setEnabled(enable);
                }
            }
        });
    }

    @Override
    public void showAbuseResult(CharSequence message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @Override
    public void downloadImageWithPermissions(String description, Uri toDownload) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            downloadImage(description, toDownload);
            return;
        }
        ImageSliderActivityPermissionsDispatcher.downloadImageWithPermissionsWithPermissionCheck(this, description, toDownload);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ImageSliderActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    public void downloadImage(String description, Uri toDownload) {
        final DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) {
            Timber.w("Download manager is unavailable!");
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(toDownload)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle("Dogsy: сохранение фото")
                .setDescription(description)
                .setVisibleInDownloadsUi(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "dogsy")
                .setMimeType("image/jpeg");

        dm.enqueue(request);
    }

    @ProvidePresenter
    public ImageSliderPresenter providePresenter() {
        return presenterProvider.get();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void downloadImageOpenSettings() {
//        Dogsy.app().prefs().edit().putLong(SitterTabModule.PREF_DELAY_GEO_PERMISSION,
//                TimeHelper.timestamp() + TimeHelper.YEAR_SECONDS).apply();

        new AlertDialog.Builder(this)
                .setTitle("Разрешите доступ на запись фото")
                .setMessage("Для того чтобы разрешить доступ к хранилищу, откройте настройки приложения, выберите пункт \"Разрешения\" и поставьте галочку \"Память\".")
                .setPositiveButton("Открыть настройки", (d, v) -> {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Закрыть", (d, v) -> {
                    d.dismiss();
                })
                .show();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMenuDefer.detach();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_slider);
//        supportPostponeEnterTransition();
        ButterKnife.bind(this);
        setupToolbar(toolbar);
        imagePager.setPageMargin(Dogsy.app().display().dpToPx(4));

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == View.VISIBLE) {
                    toolbar.startAnimation(AnimationUtils.loadAnimation(ImageViewerActivity.this, R.anim.slide_down));
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().show();
                    }

                } else {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().hide();
                    }
                    toolbar.startAnimation(AnimationUtils.loadAnimation(ImageViewerActivity.this, R.anim.slide_up));
                }
            }
        });

        showSystemUI();
        final ViewGroup.MarginLayoutParams lp = ((ViewGroup.MarginLayoutParams) toolbar.getLayoutParams());
        lp.topMargin = Dogsy.app().display().getStatusBarHeight();
        toolbar.setLayoutParams(lp);

        presenter.handleExtras(getIntent().getExtras());
    }

    private FragmentStatePagerAdapter createAdapter(List<SlideImage> data) {
        return new FragmentStatePagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return ImageDetailFragment.newInstance(data.get(position), position);
            }

            @Override
            public int getCount() {
                return data.size();
            }
        };
    }

    public static class Builder extends ActivityBuilder {
        private final long mUserId;
        private List<SlideImage> mImageList = new ArrayList<>();
        private CharSequence mTitle;
        private Boolean mEnableCounter = false;
        private int mPosition = 0;

        public Builder(@NonNull Activity from, long userId) {
            super(from);
            mUserId = userId;
        }

        public Builder(Fragment from, long userId) {
            super(from);
            mUserId = userId;
        }

        public Builder fromSitter(Sitter sitter) {
            if (!sitter.hasPhotos()) {
                return this;
            }

            mImageList = Stream.of(sitter.getPhotos())
                    .map(SlideImage::new)
                    .toList();

            return this;
        }

        public Builder fromDog(Dog dog) {
            if (!dog.hasPhotos()) {
                return this;
            }

            mImageList = Stream.of(dog.photos)
                    .map(SlideImage::new)
                    .toList();

            return this;
        }

        public Builder addImage(SlideImage image) {
            mImageList.add(image);
            return this;
        }

        public Builder addImage(SlideImage.SlideImageContainer imageContainer) {
            mImageList.add(new SlideImage(imageContainer));
            return this;
        }

        public Builder addImage(String imageUrl) {
            mImageList.add(new SlideImage(imageUrl));
            return this;
        }

        public Builder addImagesSlides(List<SlideImage> images) {
            mImageList.addAll(images);
            return this;
        }

        public Builder addImagesContainers(List<SlideImage.SlideImageContainer> containers) {
            mImageList.addAll(Stream.of(containers).map(SlideImage::new).toList());
            return this;
        }

        public Builder addImagesUrls(List<String> imageUrls) {
            mImageList.addAll(Stream.of(imageUrls).map(SlideImage::new).toList());
            return this;
        }

        public Builder setEnableCounter(boolean enable) {
            mEnableCounter = enable;
            return this;
        }

        public Builder setTitle(final CharSequence title) {
            mTitle = title;
            return this;
        }

        public Builder setPosition(int position) {
            mPosition = position;
            return this;
        }

        @Override
        protected void onBeforeStart(Intent intent) {
            super.onBeforeStart(intent);
            intent.putExtra(EXTRA_IMAGE_LIST, Parcels.wrap(mImageList));
            intent.putExtra(EXTRA_TITLE, mTitle);
            intent.putExtra(EXTRA_ENABLE_COUNTER, mEnableCounter);
            intent.putExtra(EXTRA_START_POSITION, mPosition);
            intent.putExtra(EXTRA_USER_ID, mUserId);
        }

        @Override
        protected Class<?> getActivityClass() {
            return ImageViewerActivity.class;
        }
    }
}
