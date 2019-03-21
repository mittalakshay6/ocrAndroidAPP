package com.example.ocr3;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.PictureResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ImageViewActivity extends AppCompatActivity {

    PictureResult mPictureResult;
    ImageView mImageView;
    Button recogniseBtn;
    Bitmap mBitmap;
    Bitmap mMutableBitmap;

    private Canvas mCanvas;
    private Paint mPaint;
    private String TAG = "ImageViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);
        verifyStoragePermissions(ImageViewActivity.this);

        mPaint=new Paint();
        mPaint.setColor(Color.BLUE);
        mPaint.setStrokeWidth(1);
        mPaint.setStyle(Paint.Style.STROKE);

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
        Log.d(TAG, "Button Pressed");
        recogniseText(mBitmap);
    }
    protected void recogniseText(Bitmap bitmap){
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        Log.d(TAG, "Firebase vision image object created");
        FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudDocumentTextRecognizer();
        Log.d(TAG, "Detector");
        detector.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                    @Override
                    public void onSuccess(FirebaseVisionDocumentText result) {
                        Log.d(TAG, "Success");
                        mMutableBitmap = convertToMutable(mBitmap);
                        Log.d(TAG, "Converted to mutable");
                        String resultText = result.getText();
                        for (FirebaseVisionDocumentText.Block block : result.getBlocks()) {
                            String blockText = block.getText();
                            Log.d(TAG, blockText);
                            Rect rect = block.getBoundingBox();
                            Integer dim = rect.width() + rect.height();
                            Log.d(TAG, dim.toString());
                            mCanvas = new Canvas(mMutableBitmap);
                            mCanvas.drawRect(rect, mPaint);
                            mImageView.setImageBitmap(mMutableBitmap);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        e.printStackTrace();
                        // ...
                    }
                });
    }
    /**
     * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
     * more memory that there is already allocated.
     *
     * @param imgIn - Source image. It will be released, and should not be used more
     * @return a copy of imgIn, but muttable.
     */
    public static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
