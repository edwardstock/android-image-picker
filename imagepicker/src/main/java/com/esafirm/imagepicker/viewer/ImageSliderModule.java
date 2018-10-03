package com.esafirm.imagepicker.viewer;

import android.app.FragmentManager;
import android.net.Uri;
import android.view.MenuItem;

import com.arellomobile.mvp.MvpView;
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy;
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType;

import java.util.List;

import dagger.Module;
import dagger.Provides;
import me.dogsy.app.slider.models.SlideImage;
import me.dogsy.app.slider.ui.ImageSliderActivity;
import me.dogsy.app.slider.views.ImageSliderPresenter;

/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@Module
public class ImageSliderModule {

    @Provides
    public FragmentManager provideFragmentManager(ImageSliderActivity activity) {
        return activity.getFragmentManager();
    }

    @StateStrategyType(AddToEndSingleStrategy.class)
    public interface ImageSliderView extends MvpView {
        void initSlider(List<SlideImage> imageUrls);
        void setTitle(String title);
        void setSubtitle(int from, int to);
        void setOnPageChangeListener(ImageSliderPresenter.SliderPageListener listener);
        void setCurrentPage(int page);
        void showSystemUI();
        void hideSystemUI();
        void setOnImageDownloadListener(MenuItem.OnMenuItemClickListener listener);
        void setOnImageAbuseListener(MenuItem.OnMenuItemClickListener listener);
        void setEnableAbuse(boolean enable);
        void showAbuseResult(CharSequence message);
        void downloadImageWithPermissions(String description, Uri toDownload);
    }
}
