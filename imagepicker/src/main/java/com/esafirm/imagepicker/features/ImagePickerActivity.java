package com.esafirm.imagepicker.features;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.imagepicker.R;
import com.esafirm.imagepicker.features.camera.CameraHelper;
import com.esafirm.imagepicker.features.camera.DefaultCameraModule;
import com.esafirm.imagepicker.features.cameraonly.CameraOnlyConfig;
import com.esafirm.imagepicker.features.common.BaseConfig;
import com.esafirm.imagepicker.features.recyclers.OnBackAction;
import com.esafirm.imagepicker.features.recyclers.RecyclerViewManager;
import com.esafirm.imagepicker.helper.ConfigUtils;
import com.esafirm.imagepicker.helper.ImagePickerPreferences;
import com.esafirm.imagepicker.helper.LocaleManager;
import com.esafirm.imagepicker.helper.ViewUtils;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;
import com.esafirm.imagepicker.view.SnackBarView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

@RuntimePermissions
public class ImagePickerActivity extends AppCompatActivity implements ImagePickerView {

    private static final String STATE_KEY_CAMERA_MODULE = "Key.CameraModule";

    private static final int RC_CAPTURE_PHOTO = 2000;
    private static final int RC_CAPTURE_VIDEO = RC_CAPTURE_PHOTO + 1;

    private static final int RC_PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 23;
    private static final int RC_PERMISSION_REQUEST_CAMERA_FOR_PHOTO = 24;
    private static final int RC_PERMISSION_REQUEST_CAMERA_FOR_VIDEO = 25;

    private ActionBar actionBar;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private RecyclerView recyclerView;
    private SnackBarView snackBarView;

    private RecyclerViewManager recyclerViewManager;

    private ImagePickerPresenter presenter;
    private ImagePickerPreferences preferences;
    private ImagePickerConfig config;

    private Handler handler;
    private ContentObserver observer;

    private boolean isCameraOnly;

    /**
     * Create option menus and update title
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getImagePickerConfig() == null) {
            return true;
        }

        if (getImagePickerConfig().isShowCamera()) {
            if (getImagePickerConfig().isIncludePhotos() && getImagePickerConfig().isIncludeVideo()) {
                getMenuInflater().inflate(R.menu.ef_image_picker_menu_all_main, menu);
            } else if (getImagePickerConfig().isIncludeVideo()) {
                getMenuInflater().inflate(R.menu.ef_image_picker_menu_video_main, menu);
            } else if (getImagePickerConfig().isIncludePhotos()) {
                getMenuInflater().inflate(R.menu.ef_image_picker_menu_photo_main, menu);
            }
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        MenuItem menuDone = menu.findItem(R.id.menu_done);
//        if (menuDone != null) {
//            menuDone.setTitle(ConfigUtils.getDoneButtonText(this, config));
//            menuDone.setVisible(recyclerViewManager.isShowDoneButton());
//        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Handle option menu's click event
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

//        if (id == R.id.menu_done) {
//            onDone();
//            return true;
//        }
        if (id == R.id.menu_camera) {
            captureImageWithPermission();
            return true;
        }

        if (id == R.id.menu_video) {
            captureVideoWithPermission();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Config recyclerView when configuration changed
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (recyclerViewManager != null) {
            // recyclerViewManager can be null here if we use cameraOnly mode
            recyclerViewManager.changeOrientation(newConfig.orientation);
        }
    }

    /**
     * Handle permission results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        ImagePickerActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (isCameraOnly) {
            super.onBackPressed();
            return;
        }

        recyclerViewManager.handleBack(new OnBackAction() {
            @Override
            public void onBackToFolder() {
                invalidateTitle();
            }

            @Override
            public void onFinishImagePicker() {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public void finishPickImages(List<Image> images) {
        Intent data = new Intent();
        data.putParcelableArrayListExtra(IpCons.EXTRA_SELECTED_IMAGES, (ArrayList<? extends Parcelable>) images);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void showCapturedImage() {
        getDataWithPermission();
    }

    @Override
    public void showFetchCompleted(List<Image> images, List<Folder> folders) {
        ImagePickerConfig config = getImagePickerConfig();
        if (config != null && config.isFolderMode()) {
            setFolderAdapter(folders);
        } else {
            setImageAdapter(images);
        }
    }

    @Override
    public void showError(Throwable throwable) {
        String message = "Unknown Error";
        if (throwable != null && throwable instanceof NullPointerException) {
            message = "Images not exist";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
    }

    @Override
    public void showEmpty() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
    }

    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void getData() {
        ImagePickerConfig config = getImagePickerConfig();
        presenter.abortLoad();
        if (config != null) {
            presenter.loadImages(config);
        }
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void requestWriteExternalPermission(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ef_permission_request_title)
                .setMessage(R.string.ef_msg_no_write_external_permission)
                .setPositiveButton(R.string.ef_permission_request_grant, (d, w) -> {
                    request.proceed();
                    d.dismiss();
                })
                .setNegativeButton(R.string.ef_permission_request_deny, (d, w) -> {
                    request.cancel();
                    d.dismiss();
                })
                .create().show();
    }

    @SuppressLint("NoCorrespondingNeedsPermission")
    @OnShowRationale(Manifest.permission.CAMERA)
    void requestCameraPermissionRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ef_permission_request_title)
                .setMessage(R.string.ef_msg_no_camera_permission)
                .setPositiveButton(R.string.ef_permission_request_grant, (d, w) -> {
                    request.proceed();
                    d.dismiss();
                })
                .setNegativeButton(R.string.ef_permission_request_deny, (d, w) -> {
                    request.cancel();
                    d.dismiss();
                })
                .create().show();
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void showDeniedForCamera() {
        snackBarView.show(R.string.ef_msg_no_camera_permission, v -> openAppSettings());
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDeniedForWriteExternalStorage() {
        snackBarView.show(R.string.ef_msg_no_write_external_permission, v -> openAppSettings());
    }

    /**
     * Start camera intent
     * Create a temporary file and pass file Uri to camera intent
     */
    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void captureImage() {
        if (!CameraHelper.checkCameraAvailability(this)) {
            return;
        }
        presenter.captureImage(this, getBaseConfig(), RC_CAPTURE_PHOTO);
    }

    /**
     * Start camera intent
     * Create a temporary file and pass file Uri to camera intent
     */
    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void captureVideo() {
        if (!CameraHelper.checkCameraAvailability(this)) {
            return;
        }
        presenter.captureVideo(this, getBaseConfig(), RC_CAPTURE_VIDEO);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.updateResources(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        /* This should not happen */
        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            Timber.e("This should not happen. Please open an issue!");
            finish();
            return;
        }

        isCameraOnly = getIntent().hasExtra(CameraOnlyConfig.class.getSimpleName());

        setupComponents();

        if (isCameraOnly) {
            if (savedInstanceState == null) {
                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.ef_camera)
                        .setMessage(R.string.ef_camera_only_message)
                        .setPositiveButton(R.string.ef_capture_photo, (d, w) -> {
                            captureImageWithPermission();
                            d.dismiss();
                        })
                        .setNegativeButton(R.string.ef_capture_video, (d, w) -> {
                            captureVideoWithPermission();
                            d.dismiss();
                        })
                        .setNeutralButton(R.string.ef_cancel, (d, w) -> {
                            d.dismiss();
                            setResult(RESULT_CANCELED);
                            finish();
                        })
                        .create();

                dialog.setOnDismissListener(d -> {
                    setResult(RESULT_CANCELED);
                    finish();
                });
                dialog.setCancelable(false);
                dialog.show();
            }
        } else {
            ImagePickerConfig config = getImagePickerConfig();
            if (config != null) {
                setTheme(config.getTheme());
                setContentView(R.layout.ef_activity_image_picker);
                setupView(config);
                setupRecyclerView(config);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isCameraOnly) {
            getDataWithPermission();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(STATE_KEY_CAMERA_MODULE, presenter.getCameraModule());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        presenter.setCameraModule((DefaultCameraModule) savedInstanceState.getSerializable(STATE_KEY_CAMERA_MODULE));
    }

    /**
     * Check if the captured image is stored successfully
     * Then reload data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_CAPTURE_PHOTO) {
            if (resultCode == RESULT_OK) {
                presenter.finishCaptureImage(this, data, getBaseConfig());
            } else if (resultCode == RESULT_CANCELED && isCameraOnly) {
                presenter.abortCaptureImage();
                finish();
            }
        } else if (requestCode == RC_CAPTURE_VIDEO) {
            if (resultCode == RESULT_OK) {
                presenter.finishCaptureVideo(this, data, getBaseConfig());
            } else if (resultCode == RESULT_CANCELED && isCameraOnly) {
                presenter.abortCaptureVideo();
                finish();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isCameraOnly) {
            return;
        }

        if (handler == null) {
            handler = new Handler();
        }
        observer = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                getData();
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false, observer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.abortLoad();
            presenter.detachView();
        }

        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    private BaseConfig getBaseConfig() {
        return isCameraOnly
                ? getCameraOnlyConfig()
                : getImagePickerConfig();
    }

    private CameraOnlyConfig getCameraOnlyConfig() {
        return getIntent().getParcelableExtra(CameraOnlyConfig.class.getSimpleName());
    }

    @Nullable
    private ImagePickerConfig getImagePickerConfig() {
        if (config == null) {
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                throw new IllegalStateException("This should not happen. Please open an issue!");
            }
            config = bundle.getParcelable(ImagePickerConfig.class.getSimpleName());
        }
        return config;
    }

    private void setupView(ImagePickerConfig config) {
        progressBar = findViewById(R.id.progress_bar);
        emptyTextView = findViewById(R.id.tv_empty_images);
        recyclerView = findViewById(R.id.recyclerView);
        snackBarView = findViewById(R.id.ef_snackbar);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {

            final Drawable arrowDrawable = ViewUtils.getArrowIcon(this);
            final int arrowColor = config.getArrowColor();
            if (arrowColor != ImagePickerConfig.NO_COLOR && arrowDrawable != null) {
                arrowDrawable.setColorFilter(arrowColor, PorterDuff.Mode.SRC_ATOP);
            }
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(arrowDrawable);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    private void setupRecyclerView(ImagePickerConfig config) {
        recyclerViewManager = new RecyclerViewManager(
                recyclerView,
                config,
                getResources().getConfiguration().orientation
        );

        recyclerViewManager.setupAdapters((isSelected) -> recyclerViewManager.selectImage(isSelected)
                , bucket -> setImageAdapter(bucket.getImages()));

        recyclerViewManager.setImageSelectedListener(selectedImage -> {
            invalidateTitle();
            if (ConfigUtils.shouldReturn(config, false) && !selectedImage.isEmpty()) {
                onDone();
            }
        });

    }

    /* --------------------------------------------------- */
    /* > View Methods */
    /* --------------------------------------------------- */

    private void setupComponents() {
        preferences = new ImagePickerPreferences(this);
        presenter = new ImagePickerPresenter(new ImageFileLoader(this));
        presenter.attachView(this);
    }

    /**
     * Set image adapter
     * 1. Set new data
     * 2. Update item decoration
     * 3. Update title
     */
    private void setImageAdapter(List<Image> images) {
        recyclerViewManager.setImageAdapter(images);
        invalidateTitle();
    }

    private void setFolderAdapter(List<Folder> folders) {
        recyclerViewManager.setFolderAdapter(folders);
        invalidateTitle();
    }

    private void invalidateTitle() {
        supportInvalidateOptionsMenu();
        actionBar.setTitle(recyclerViewManager.getTitle());
    }

    /**
     * On finish selected image
     * Get all selected images then return image to caller activity
     */
    private void onDone() {
        presenter.onDoneSelectImages(recyclerViewManager.getSelectionTracker());
    }

    /**
     * Check permission
     */
    private void getDataWithPermission() {
        ImagePickerActivityPermissionsDispatcher.getDataWithPermissionCheck(this);
    }

    private void requestCameraPermissionsForPhoto() {
        Timber.w("Write External permission is not granted. Requesting permission");

        ArrayList<String> permissions = new ArrayList<>(2);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (checkForRationale(permissions)) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), RC_PERMISSION_REQUEST_CAMERA_FOR_PHOTO);
        } else {
            final String permission = ImagePickerPreferences.PREF_CAMERA_REQUESTED;
            if (!preferences.isPermissionRequested(permission)) {
                preferences.setPermissionRequested(permission);
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), RC_PERMISSION_REQUEST_CAMERA_FOR_PHOTO);
            } else {
                if (isCameraOnly) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.ef_msg_no_camera_permission), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    snackBarView.show(R.string.ef_msg_no_camera_permission, v -> openAppSettings());
                }
            }
        }
    }

    //todo combine
    private void requestCameraPermissionsForVideo() {
        Timber.w("Write External permission is not granted. Requesting permission");

        ArrayList<String> permissions = new ArrayList<>(2);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (checkForRationale(permissions)) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), RC_PERMISSION_REQUEST_CAMERA_FOR_PHOTO);
        } else {
            final String permission = ImagePickerPreferences.PREF_CAMERA_REQUESTED;
            if (!preferences.isPermissionRequested(permission)) {
                preferences.setPermissionRequested(permission);
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[permissions.size()]), RC_PERMISSION_REQUEST_CAMERA_FOR_PHOTO);
            } else {
                if (isCameraOnly) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.ef_msg_no_camera_permission), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    snackBarView.show(R.string.ef_msg_no_camera_permission, v -> openAppSettings());
                }
            }
        }
    }

    private boolean checkForRationale(List<String> permissions) {
        for (int i = 0, size = permissions.size(); i < size; i++) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Open app settings screen
     */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * Request for camera permission
     */
    private void captureVideoWithPermission() {
        ImagePickerActivityPermissionsDispatcher.captureVideoWithPermissionCheck(this);
    }

    /**
     * Request for camera permission
     */
    private void captureImageWithPermission() {
        ImagePickerActivityPermissionsDispatcher.captureImageWithPermissionCheck(this);
    }

}
