package jkfj.brumhack.justeatpetsapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;

import java.io.ByteArrayOutputStream;


/**
 * A simple Activity that performs recognition using the Clarifai API.
 */
public class RecognitionActivity extends Activity {
    private static final String TAG = "RECOGNITIONACTIVITY";
    public static final String FILTER = "filter";

    // IMPORTANT NOTE: you should replace these keys with your own App ID and secret.
    // These can be obtained at https://developer.clarifai.com/applications
    private static final String APP_ID = credentials.APP_ID;
    private static final String APP_SECRET = credentials.APP_SECRET;
    //shouldn't really commit these. Oops

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private final ClarifaiClient client = new ClarifaiClient(APP_ID, APP_SECRET);
    private ImageView imageView;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);
        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);
        dispatchTakePictureIntent();
    }

    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap bitmap = (Bitmap) extras.get("data");
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                textView.setText("Recognizing...");

                // Run recognition on a background thread since it makes a network call.
                new AsyncTask<Bitmap, Void, RecognitionResult>() {
                    @Override
                    protected RecognitionResult doInBackground(Bitmap... bitmaps) {
                        return recognizeBitmap(bitmaps[0]);
                    }

                    @Override
                    protected void onPostExecute(RecognitionResult result) {
                        filterTagsToGetPet(result);
                    }
                }.execute(bitmap);
            } else {
                textView.setText("Unable to load selected image.");
            }
        }
    }

    /**
     * Sends the given bitmap to Clarifai for recognition and returns the result.
     */
    private RecognitionResult recognizeBitmap(Bitmap bitmap) {
        try {
            // Scale down the image. This step is optional. However, sending large images over the
            // network is slow and  does not significantly improve recognition performance.
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 480,
                    480 * bitmap.getHeight() / bitmap.getWidth(), true);

            // Compress the image as a JPEG.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 90, out);
            byte[] jpeg = out.toByteArray();

            // Send the JPEG to Clarifai and return the result.
            return client.recognize(new RecognitionRequest(jpeg)).get(0);
        } catch (ClarifaiException e) {
            Log.e(TAG, "Clarifai error", e);
            return null;
        }
    }


    /**
     * Gets the first animal tag from Clarifai, and starts the food list activity with that filter.
     * @param result
     */
    private void filterTagsToGetPet(RecognitionResult result) {
        String animalFilter = "dog"; //everyone likes dogs, not sure of behaviour if I make this blank

        //if given result is ok, filter tags to get animal
        if (result != null && result.getStatusCode() == RecognitionResult.StatusCode.OK) {
            for (Tag tag : result.getTags()) {

                //gets first tag that matches
                String tagName = tag.getName();
                if (tagName.equals("cat")) {
                    animalFilter = "cat";
                    break;
                } else if (tagName.equals("dog")) {
                    animalFilter = "dog";
                    break;
                } else if (tagName.equals("fish")) {
                    animalFilter = "fish";
                    break;
                }
            }

            Log.i(TAG, "Animal filter is : " + animalFilter);

            toastAnimalFilter(animalFilter);

            //send intent to the food api business
            Intent i = new Intent(RecognitionActivity.this, ProductsActivity.class);
            i.putExtra(FILTER, animalFilter);
            startActivity(i);
        }
    }

    /**
     * Tells the user what animal was selected, via a toast
     * @param animalFilter the animal chosen
     */
    private void toastAnimalFilter(String animalFilter) {
        Context context = getApplicationContext();
        CharSequence text = animalFilter;
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }
}