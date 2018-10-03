package com.esafirm.imagepicker.viewer.views;

import android.net.Uri;
import android.os.Bundle;

import com.arellomobile.mvp.InjectViewState;

import org.parceler.Parcels;

import java.util.List;

import javax.inject.Inject;

import me.dogsy.app.auth.AuthSession;
import me.dogsy.app.internal.mvp.MvpBasePresenter;
import me.dogsy.app.slider.ImageSliderModule;
import me.dogsy.app.slider.models.SlideImage;
import me.dogsy.app.slider.ui.ImageSliderActivity;
import me.dogsy.app.user.repo.UserRepository;
import timber.log.Timber;

import static me.dogsy.app.internal.common.Preconditions.checkNotNull;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@InjectViewState
public class ImageSliderPresenter extends MvpBasePresenter<ImageSliderModule.ImageSliderView> {

    @Inject AuthSession session;
    @Inject UserRepository userRepo;

    private int mLastPage = 0;
    private long mUserId = 0;
    private List<SlideImage> mImageUrls;
    private boolean mEnableCounter;
    private String mTitle;

    @Inject
    public ImageSliderPresenter() {
    }

    public void handleExtras(Bundle extras) {
        checkNotNull(extras, "Extras required");

        mImageUrls = Parcels.unwrap(extras.getParcelable(ImageSliderActivity.EXTRA_IMAGE_LIST));
        mTitle = extras.getString(ImageSliderActivity.EXTRA_TITLE, "");
        mEnableCounter = extras.getBoolean(ImageSliderActivity.EXTRA_ENABLE_COUNTER, false);
        mLastPage = extras.getInt(ImageSliderActivity.EXTRA_START_POSITION, mLastPage);
        mUserId = extras.getLong(ImageSliderActivity.EXTRA_USER_ID);

        getViewState().setTitle(mTitle);
        if (mEnableCounter) {
            getViewState().setSubtitle(mLastPage + 1, mImageUrls.size());
        }

        getViewState().initSlider(mImageUrls);
        getViewState().setCurrentPage(mLastPage);

        getViewState().setOnImageAbuseListener(menuItem -> {
            abuse(0, mImageUrls.get(0).getUrl());
            return true;
        });

        getViewState().setOnImageDownloadListener(menuItem -> {
            downloadImage(1, mImageUrls.get(0).getUrl());
            return true;
        });

        getViewState().setOnPageChangeListener(page -> {
            mLastPage = page;
            if (mEnableCounter) {
                getViewState().setSubtitle(page + 1, mImageUrls.size());
            }
            getViewState().setOnImageAbuseListener(menuItem -> {
                abuse(page, mImageUrls.get(page).getUrl());
                return true;
            });

            getViewState().setOnImageDownloadListener(menuItem -> {
                downloadImage(page + 1, mImageUrls.get(page).getUrl());
                return true;
            });
        });
    }

    @Override
    public void attachView(ImageSliderModule.ImageSliderView view) {
        super.attachView(view);
        getViewState().setCurrentPage(mLastPage);
    }

    private void abuse(int pos, String url) {
        if (mUserId <= 0) {
            Timber.w("Trying to abuse on photo %s with user id: 0", url);
            return;
        }

        if (mUserId == session.getUser().getId()) {
            getViewState().showAbuseResult("Нельзя пожаловаться на себя");
            return;
        }

        safeSubscribeIoToUi(userRepo.abuse(mUserId, url))
                .subscribe(res -> {
                    if (!res.isSuccess()) {
                        getViewState().showAbuseResult(res.getDisplayMessage());
                    } else {
                        getViewState().showAbuseResult("Жалоба отправлена!");
                    }
                });
    }

    private void downloadImage(int pos, String url) {


        Uri toDownload = Uri.parse(url);
        /*
        DownloadManager.Request request = new DownloadManager.Request(toDownload)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle("Dogsy: сохранение фото")
                .setDescription(mTitle + " " + String.valueOf(pos))
                .setVisibleInDownloadsUi(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "dogsy")
                .setMimeType("image/jpeg");

        long did = dm.enqueue(request);
         */
        getViewState().downloadImageWithPermissions(mTitle + " " + String.valueOf(pos), toDownload);
    }

    private void abuseImage(String url) {

    }

    public interface SliderPageListener {
        void onPageChanged(int page);
    }
}
