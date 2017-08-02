package com.flurgle.camerakit.demo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.flurgle.camerakit.CameraKit;
import com.flurgle.camerakit.CameraListener;
import com.flurgle.camerakit.CameraView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.nio.ByteBuffer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.vision.text.Text;
import com.google.api.client.http.HttpTransport;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import static com.flurgle.camerakit.demo.R.id.startMonitoring;
import static com.flurgle.camerakit.demo.R.id.txtRes;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import android.media.MediaPlayer;

import org.json.JSONArray;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements View.OnLayoutChangeListener {

    @BindView(R.id.activity_main)
    ViewGroup parent;

    @BindView(R.id.camera)
    CameraView camera;


    //Declaring required variables
    private int mCameraWidth;
    private int mCameraHeight;
    byte[] currentImage;
    boolean clicked = false;
    private static final String CLOUD_VISION_API_KEY = "AIzaSyAeDtQ3Up0JgJKUZEJw3A-4aOj68PMovqM";
    MediaPlayer objMediaPlayer;
    boolean isMediaPlayerRunning = false;
    String initialResult = "";
    String currentResult = "";
    boolean hasAPICallFinished = true;
    TextView txtResult;
    Button help;
    ImageButton btn;
    String appendText = "Result: ";

    /*********
     * This method is executed when the MainActivity is launched
     *
     *********/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        camera.addOnLayoutChangeListener(this);
        btn = (ImageButton)findViewById(R.id.startMonitoring);
        txtResult = (TextView) findViewById(R.id.txtRes);
        //txtResult.setText(appendText);
        objMediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.alarm);
    }


    @Override
    protected void onResume() {
        super.onResume();
        camera.start();
    }

    @Override
    protected void onPause() {
        camera.stop();
        super.onPause();
    }

    @OnClick(R.id.startMonitoring)
    void startMonitor() {
        clicked = false;
        openCamera();
    }


    @OnClick(R.id.stopMonitoring)
    void stopMonitor(){
        clicked = true;
    }

    private void openCamera(){
        MainActivity.this.runOnUiThread(new Runnable() {
           public void run() {
                try {
                    while (clicked == false) {
                        if(hasAPICallFinished) {
                            hasAPICallFinished = false;
                            startImageLoop();
                        }
                        Thread.sleep(1000);
                        btn.performClick();

                        txtResult.setText(currentResult);
                        startMonitor();
                    }
                } catch (Exception e) {

                }
            }
        });
    }




    private void updateCamera(boolean updateWidth, boolean updateHeight) {
        ViewGroup.LayoutParams cameraLayoutParams = camera.getLayoutParams();
        int width = cameraLayoutParams.width;
        int height = cameraLayoutParams.height;

        cameraLayoutParams.width = width;
        cameraLayoutParams.height = height;

        camera.addOnLayoutChangeListener(this);
        camera.setLayoutParams(cameraLayoutParams);

        Toast.makeText(this, (updateWidth && updateHeight ? "Width and height" : updateWidth ? "Width" : "Height") + " updated!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        mCameraWidth = right - left;
        mCameraHeight = bottom - top;
        camera.removeOnLayoutChangeListener(this);
    }

    public void startImageLoop(){
        final long startTime = System.currentTimeMillis();
        camera.setCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(byte[] jpeg) {
                try {
                    super.onPictureTaken(jpeg);
                    long callbackTime = System.currentTimeMillis();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
                    int byteSize = bitmap.getRowBytes() * bitmap.getHeight();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(byteSize);
                    bitmap.copyPixelsToBuffer(byteBuffer);

                    // Get the byteArray.
                    currentImage = byteBuffer.array();

                    // Get the ByteArrayInputStream.
                    ByteArrayInputStream bs = new ByteArrayInputStream(currentImage);
                    callCloudVision(bitmap);

                }catch(Exception e){

                }
            }
        });
        camera.captureImage();
    }

    private void callCloudVision(final Bitmap bitmap) throws IOException {
        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                /**
                                 * We override this so we can inject important identifying fields into the HTTP
                                 * headers. This enables use of a restricted cloud platform API key.
                                 */
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                                                   }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        // add the features we want
                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    //Log.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    String res = response.toString();
                    compareImage(res);
                    return res;

                } catch (GoogleJsonResponseException e) {
                    //Log.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    //Log.d(TAG, "failed to make API request because of other IOException " +
                           // e.getMessage());
                }
                return "Cloud Vision API request failed. Check logs for details.";
            }

            protected void onPostExecute(String result) {

            }
        }.execute();
    }
    public void compareImage(String result)
    {
        StringBuilder processing = new StringBuilder();
        // Parse JSON response to get the element with highest probability
        if(result != null) {
            try {
                JSONObject jsonObj = new JSONObject(result);
                JSONArray responses = jsonObj.getJSONArray("responses");
                for (int count = 0; count < responses.length(); count++) {
                    JSONObject labelAnnotations = responses.getJSONObject(count);
                    JSONArray lAnnotations = labelAnnotations.getJSONArray("labelAnnotations");
                    for(int lCount = 0; lCount < lAnnotations.length(); lCount++)
                    {
                        JSONObject description = lAnnotations.getJSONObject(lCount);
                        Double score = Double.parseDouble(description.getString("score"));
                        if(score > 0.90) // Setting the minimum probability to 90% in order to consider a label
                            processing.append(description.getString("description"));
                    }
                }

                //Compare intial result with current result to check if the object is missing
                if (initialResult.equals("")) {
                    initialResult = processing.toString();
                    currentResult = processing.toString();
                } else {
                    currentResult = processing.toString();
                }
                //Call method to set textview to display current result
                setText(appendText.concat(currentResult));
                if (!currentResult.contains("hand") && !currentResult.contains("finger")) {
                    if (!initialResult.equals(currentResult)) {
                        objMediaPlayer.start();
                        isMediaPlayerRunning = true;
                    } else {
                        if (isMediaPlayerRunning)
                            objMediaPlayer.pause();
                    }
                }
                hasAPICallFinished = true;
            } catch (Exception e) {
                System .out.println(e.getMessage());
            }
        }
    }

    // Method to set textview to display current result
    private void setText(final String value){
        runOnUiThread(new Runnable() { //Running UI Thread as UI elements are accessible only through UI Thread
            @Override
            public void run() {
                txtResult.setText(value);
            }
        });
    }
}
