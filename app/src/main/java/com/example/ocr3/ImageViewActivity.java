package com.example.ocr3;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.PictureResult;

public class ImageViewActivity extends AppCompatActivity {

    PictureResult mPictureResult;
    ImageView mImageView;
    Button recogniseBtn;
    Bitmap mBitmap;
    private String TAG = "ImageViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        mPictureResult=MainActivity.mPictureResult;
        mImageView=findViewById(R.id.imageView);
        recogniseBtn = findViewById(R.id.recogniseBtn);
        recogniseBtn.setVisibility(View.INVISIBLE);
        displayPicture(mPictureResult, mImageView);
    }
    private void displayPicture(PictureResult pictureResult, final ImageView imageView){
        pictureResult.toBitmap(pictureResult.getSize().getWidth(), pictureResult.getSize().getHeight(), new BitmapCallback() {
            @Override
            public void onBitmapReady(@Nullable Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
                mBitmap=bitmap;
                recogniseBtn.setVisibility(View.VISIBLE);
            }
        });
    }
    public void onClickRecogniseButton(View view){
        recogniseText(mBitmap);
    }
    protected void recogniseText(Bitmap bitmap){
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer();
        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                    @Override
                    public void onSuccess(FirebaseVisionDocumentText result) {
                        String resultText = result.getText();
                        for (FirebaseVisionDocumentText.Block block : result.getBlocks()) {
                            String blockText = block.getText();
                            Log.d(TAG, blockText);
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
}
