package com.example.toksaitov.vision.networking;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

public class NetworkFragment extends Fragment {

    public static final String TAG = "NetworkFragment";
    private static final String SERVICE_URL_KEY = "ServiceURLKey";
    private static final String IMAGE_URI_KEY = "ImageUriKey";
    private static final String REQUEST_TEMPLATE_KEY = "RequestTemplateKey";

    private DownloadCallback callback;

    private DownloadTask downloadTask;

    private String serviceURL;
    private Uri imageUri;
    private String requestTemplate;

    private boolean hasPendingDownloadOperations = false;

    public static NetworkFragment getInstance(FragmentManager fragmentManager, String serviceURL, Uri imageUri, String requestTemplate) {
        NetworkFragment networkFragment =
            (NetworkFragment) fragmentManager.findFragmentByTag(NetworkFragment.TAG);

        if (networkFragment == null) {
            networkFragment = new NetworkFragment();

            Bundle args = new Bundle();
            args.putString(SERVICE_URL_KEY, serviceURL);
            args.putParcelable(IMAGE_URI_KEY, imageUri);
            args.putString(REQUEST_TEMPLATE_KEY, requestTemplate);
            networkFragment.setArguments(args);

            fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        }

        return networkFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        serviceURL = getArguments().getString(SERVICE_URL_KEY);
        imageUri = getArguments().getParcelable(IMAGE_URI_KEY);
        requestTemplate = getArguments().getString(REQUEST_TEMPLATE_KEY);

        callback = (DownloadCallback) context;
        if (hasPendingDownloadOperations) {
            startDownload();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        callback = null;
    }

    @Override
    public void onDestroy() {
        cancelDownload();

        super.onDestroy();
    }

    public void startDownload() {
        if (hasPendingDownloadOperations = (callback == null)) {
            return;
        }

        cancelDownload();

        downloadTask = new DownloadTask((Context) callback, callback);
        downloadTask.execute(new DownloadTask.Parameters(serviceURL, imageUri, requestTemplate));
    }

    public void cancelDownload() {
        if (downloadTask != null) {
            downloadTask.cancel(true);
        }
    }

}