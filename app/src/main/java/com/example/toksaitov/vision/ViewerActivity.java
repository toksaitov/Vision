package com.example.toksaitov.vision;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.toksaitov.vision.networking.DownloadCallback;
import com.example.toksaitov.vision.networking.NetworkFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ViewerActivity extends AppCompatActivity implements DownloadCallback {

    private static final String GOOGLE_VISION_URL =
        "https://vision.googleapis.com/v1/images:annotate?key=";
    private static final String GOOGLE_VISION_API_KEY =
        "AIzaSyDdWRV8qYlvMvIpEvrNa5zYf_P0ajJjQQU";

    private static final String GOOGLE_VISION_REQUEST_TEMPLATE =
            "{"                                              +
              "\"requests\":["                               +
                "{"                                          +
                  "\"image\":{"                              +
                    "\"content\":\"ENCODED_IMAGE\""          +
                  "},"                                       +
                  "\"features\":["                           +
                    "{"                                      +
                      "\"type\": \"LABEL_DETECTION\","       +
                      "\"maxResults\":5"                     +
                    "}"                                      +
                  "]"                                        +
                "}"                                          +
              "]"                                            +
            "}";
//            "{"                                              +
//              "\"requests\":["                               +
//                "{"                                          +
//                  "\"image\":{"                              +
//                    "\"content\":\"ENCODED_IMAGE\""          +
//                  "},"                                       +
//                  "\"features\":["                           +
//                    "{"                                      +
//                      "\"type\": \"FACE_DETECTION\","        +
//                      "\"maxResults\":5"                     +
//                    "},"                                     +
//                    "{"                                      +
//                      "\"type\": \"LANDMARK_DETECTION\","    +
//                      "\"maxResults\":5"                     +
//                    "},"                                     +
//                    "{"                                      +
//                      "\"type\": \"LOGO_DETECTION\","        +
//                      "\"maxResults\":5"                     +
//                    "},"                                     +
//                    "{"                                      +
//                      "\"type\": \"LABEL_DETECTION\","       +
//                      "\"maxResults\":5"                     +
//                    "},"                                     +
//                    "{"                                      +
//                      "\"type\": \"TEXT_DETECTION\","        +
//                      "\"maxResults\":5"                     +
//                    "},"                                     +
//                    "{"                                      +
//                      "\"type\": \"SAFE_SEARCH_DETECTION\"," +
//                      "\"maxResults\":5"                     +
//                    "},"                                     +
//                    "{"                                      +
//                      "\"type\": \"IMAGE_PROPERTIES\","      +
//                      "\"maxResults\":5"                     +
//                    "}"                                      +
//                  "]"                                        +
//                "}"                                          +
//              "]"                                            +
//            "}";

    private Uri imageUri;

    private ImageView imageView;
    private EditText responseEditText;

    private ProgressDialog progressDialog;

    private NetworkFragment networkFragment;
    private boolean downloading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewer);

        imageView = (ImageView) findViewById(R.id.imageView);
        responseEditText = (EditText) findViewById(R.id.responseEditText);

        progressDialog = new ProgressDialog(ViewerActivity.this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);

        Intent intent = getIntent();
        imageUri = intent.getData();
        imageView.setImageURI(imageUri);

        networkFragment = createNetworkFragment();
        downloading = false;

        processImage();
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
            (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return connectivityManager.getActiveNetworkInfo();
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        /*
            Demonstration on how you can respond to networking events on the
            main thread
         */
        switch(progressCode) {
            case Progress.ERROR:
                // ToDo
                break;
            case Progress.CONNECT_SUCCESS:
                // ToDo
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                // ToDo
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                // ToDo
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                // ToDo
                break;
        }
    }

    @Override
    public void updateFromDownload(Object result) {
        if (result == null || result instanceof Exception) {
            reportError(getString(R.string.failed_to_connect_error_message));

            if (result != null) {
                ((Exception) result).printStackTrace();
            }
        } else {
            try {
                JSONObject jsonObject =
                    (JSONObject) result;
                JSONArray labelAnnotations =
                    jsonObject.getJSONArray("responses").getJSONObject(0).getJSONArray("labelAnnotations");
                String firstLabel =
                    labelAnnotations.getJSONObject(0).getString("description");
                String secondLabel =
                    labelAnnotations.getJSONObject(1).getString("description");
                String thirdLabel =
                    labelAnnotations.getJSONObject(2).getString("description");

                Bitmap bitmap = generateAnnotations(firstLabel, secondLabel, thirdLabel);

                imageView.setImageBitmap(bitmap);
                responseEditText.setText(jsonObject.toString(2));
            } catch (JSONException e) {
                reportError(getString(R.string.failed_to_read_the_response_error_message));

                e.printStackTrace();
            }
        }
    }

    @Override
    public void finishDownloading() {
        downloading = false;
        if (networkFragment != null) {
            networkFragment.cancelDownload();
        }

        hideProgressDialog();
    }

    private NetworkFragment createNetworkFragment() {
        return NetworkFragment.getInstance(
                   getSupportFragmentManager(),
                   GOOGLE_VISION_URL + GOOGLE_VISION_API_KEY,
                   imageUri,
                   GOOGLE_VISION_REQUEST_TEMPLATE
               );
    }

    private void processImage() {
        showProgressDialog();
        startDownload();
    }

    private void showProgressDialog() {
        progressDialog.setMessage(getString(R.string.please_wait_message));
        progressDialog.show();
    }

    private void startDownload() {
        if (!downloading && networkFragment != null) {
            networkFragment.startDownload();
            downloading = true;
        }
    }

    @NonNull
    private Bitmap generateAnnotations(String firstLabel, String secondLabel, String thirdLabel) {
        Bitmap bitmap =
            Bitmap.createBitmap(
                imageView.getWidth(),
                imageView.getHeight(),
                Bitmap.Config.ARGB_8888
            );
        Canvas canvas =
            new Canvas(bitmap);

        imageView.draw(canvas);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);

        if (firstLabel != null) {
            paint.setTextSize(80);
            canvas.drawText(firstLabel, bitmap.getWidth() / 2 - 70, bitmap.getHeight() / 2 - 100, paint);
        }

        if (secondLabel != null) {
            paint.setTextSize(40);
            canvas.drawText(secondLabel, bitmap.getWidth() / 2 - 70, bitmap.getHeight() / 2, paint);
        }

        if (thirdLabel != null) {
            paint.setTextSize(20);
            canvas.drawText(thirdLabel, bitmap.getWidth() / 2 - 70, bitmap.getHeight() / 2 + 100, paint);
        }

        return bitmap;
    }

    private void hideProgressDialog() {
        final int HANDLER_DELAY = 1000;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressDialog.hide();
            }
        }, HANDLER_DELAY);
    }

    private void reportError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
