package com.example.ocr3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;

public class MainActivity extends AppCompatActivity {

    CameraView camera;
    ImageView imageView;
    Button clickBtn;

    Activity mainActivity;

    private String TAG = "MainActivity";

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;
        camera = findViewById(R.id.camera);
        imageView = findViewById(R.id.imageView);
        clickBtn = findViewById(R.id.button);

        camera.setLifecycleOwner(this);
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                // A Picture was taken!
                result.toBitmap(result.getSize().getWidth(), result.getSize().getHeight(), new BitmapCallback() {
                    @Override
                    public void onBitmapReady(@Nullable Bitmap bitmap) {
                        imageView.setImageBitmap(bitmap);
                        clickBtn.setVisibility(View.INVISIBLE);

                        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                        FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
                                .getCloudDocumentTextRecognizer();
                        detector.processImage(image)
                                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                                    @Override
                                    public void onSuccess(FirebaseVisionDocumentText result) {
                                        // Task completed successfully
                                        // ...
                                        String resultText = result.getText();
                                        for (FirebaseVisionDocumentText.Block block: result.getBlocks()) {
                                            String blockText = block.getText();
                                            Log.d(TAG, blockText);
                                            for (FirebaseVisionDocumentText.Paragraph paragraph: block.getParagraphs()) {
                                                String paragraphText = paragraph.getText();
                                                Log.d(TAG, paragraphText);
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });

                    }
                });
            }
        });
    }
    public void takePicture(View view){
        camera.takePictureSnapshot();
    }

}
