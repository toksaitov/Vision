package com.example.toksaitov.vision.networking;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

class DownloadTask extends AsyncTask<DownloadTask.Parameters, Integer, DownloadTask.Result> {

    static class Parameters {
        String serviceURL; Uri imageUri;
        String requestTemplate;

        Parameters(String serviceURL, Uri imageUri, String requestTemplate) {
            this.serviceURL = serviceURL;
            this.imageUri = imageUri;
            this.requestTemplate = requestTemplate;
        }
    }

    static class Result {
        JSONObject value;
        Exception exception;

        Result(JSONObject value) {
            this.value = value;
        }
        Result(Exception exception) {
            this.exception = exception;
        }
    }

    private Context context;
    private DownloadCallback callback;

    DownloadTask(Context context, DownloadCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        if (callback != null) {
            NetworkInfo networkInfo = callback.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected() ||
                (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                    && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                callback.updateFromDownload(null);
                callback.finishDownloading();

                cancel(true);
            }
        }
    }

    @Override
    protected DownloadTask.Result doInBackground(DownloadTask.Parameters... parameters) {
        Result result = null;
        if (!isCancelled() && parameters != null && parameters.length > 0) {
            String urlString = parameters[0].serviceURL;
            Uri imageUri = parameters[0].imageUri;
            String requestTemplate = parameters[0].requestTemplate;

            try {
                URL url = new URL(urlString);
                JSONObject resultObject = downloadUrl(url,  imageUri, requestTemplate);

                if (resultObject != null) {
                    result = new Result(resultObject);
                } else {
                    throw new IOException("No response received.");
                }
            } catch(Exception e) {
                result = new Result(e);
            }
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (callback != null && progress.length > 1) {
            callback.onProgressUpdate(progress[0], progress[1]);
        }
    }

    private JSONObject downloadUrl(URL url, Uri imageUri, String requestTemplate) throws IOException {
        final int READ_TIMEOUT = 3000, CONNECT_TIMEOUT = 3000, STREAM_SIZE_LIMIT = 131072;

        InputStream stream = null;
        OutputStream outputStream;

        HttpsURLConnection connection = null;
        JSONObject result = null;
        try {
            String requestBody =
                createRequestBody(imageUri, requestTemplate);
            byte[] requestBytes =
                requestBody.getBytes();

            connection = (HttpsURLConnection) url.openConnection();
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "*/*");
            connection.setFixedLengthStreamingMode(requestBytes.length);
            connection.setDoOutput(true);
            connection.setDoInput(true);

            outputStream = connection.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
            bufferedWriter.write(requestBody);
            bufferedWriter.flush();
            bufferedWriter.close();

            connection.connect();

            publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode);
            }

            stream = connection.getInputStream();

            publishProgress(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);

            if (stream != null) {
                String content = readStream(stream, STREAM_SIZE_LIMIT);
                result = parseJSON(content);
            }
        } finally {
            if (stream != null) {
                stream.close();
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return result;
    }

    private String readStream(InputStream stream, int maxLength) throws IOException {
        String result = null;

        InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[maxLength];

        int numChars = 0;
        int readSize = 0;
        while (numChars < maxLength && readSize != -1) {
            numChars += readSize;
            int pct = (100 * numChars) / maxLength;
            publishProgress(DownloadCallback.Progress.PROCESS_INPUT_STREAM_IN_PROGRESS, pct);
            readSize = reader.read(buffer, numChars, buffer.length - numChars);
        }

        if (numChars != -1) {
            numChars = Math.min(numChars, maxLength);
            result = new String(buffer, 0, numChars);
        }

        return result;
    }

    private JSONObject parseJSON(String content) {
        JSONObject result = null;

        try {
            result = new JSONObject(content);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    private String createRequestBody(Uri imageUri, String requestTemplate) {
        String base64EncodedPicture =
            getBase64EncodedFile(imageUri);

        return requestTemplate.replace("ENCODED_IMAGE", base64EncodedPicture);
    }

    private String getBase64EncodedFile(Uri fileUri) {
        String result = "";

        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(fileUri);
            try {
                byte[] imageData = readBytes(inputStream);
                result = Base64.encodeToString(imageData, Base64.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        final int BUFFER_SIZE = 1024;
        byte[] buffer = new byte[BUFFER_SIZE];

        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, bytesRead);
        }

        return byteBuffer.toByteArray();
    }

    @Override
    protected void onPostExecute(Result result) {
        if (result != null && callback != null) {
            if (result.exception != null) {
                callback.updateFromDownload(result.exception);
            } else if (result.value != null) {
                callback.updateFromDownload(result.value);
            }
            callback.finishDownloading();
        }
    }

    @Override
    protected void onCancelled(Result result) { }

}
