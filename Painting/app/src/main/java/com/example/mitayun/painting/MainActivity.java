package com.example.mitayun.painting;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import java.util.Arrays;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImage3x3ConvolutionFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageAddBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageAlphaBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageBilateralFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageBoxBlurFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageBrightnessFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageBulgeDistortionFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageCGAColorspaceFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageChromaKeyBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageColorBalanceFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageColorBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageColorBurnBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageColorDodgeBlendFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageCrosshatchFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageEmbossFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageGaussianBlurFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageHueFilter;
import jp.co.cyberagent.android.gpuimage.GPUImagePosterizeFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSobelEdgeDetection;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_S = 100 * 60;
    private static final float PER_PIXEL_S = 0.5f;
    private static final int RADIUS_FILL = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView imageView = (ImageView) findViewById(R.id.image);
        ImageView imageView1 = (ImageView) findViewById(R.id.image1);
        ImageView imageView2 = (ImageView) findViewById(R.id.image2);
        ImageView imageView3 = (ImageView) findViewById(R.id.image3);

        filterAndDraw(imageView, R.drawable.bears, false);
        filterAndDraw(imageView1, R.drawable.mita, true);
        filterAndDraw(imageView2, R.drawable.jitu, true);
        filterAndDraw(imageView3, R.drawable.chai, true);
    }

    private void filterAndDraw(ImageView imageView, int resId, boolean human) {

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);


        /*
        float totalPixel = MAX_S / (PER_PIXEL_S * 1.0f);
        double scale = Math.sqrt(bitmap.getWidth() * bitmap.getHeight() / totalPixel);
        android.util.Log.d("MITA", "scale: " + scale);
        Bitmap smallGreyBitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth()/scale), (int)(bitmap.getHeight()/scale), false);
        bitmap = smallGreyBitmap;
        */

        GPUImage bearGpuImage = new GPUImage(this);
        bearGpuImage.setImage(bitmap);

        Bitmap map;
        if(!human) {
            bearGpuImage.setFilter(new GPUImageSobelEdgeDetection());
            map = getFillBitmap(bearGpuImage.getBitmapWithFilterApplied(), false, false, human);
        }else{
            GPUImageBulgeDistortionFilter filter = new GPUImageBulgeDistortionFilter();
            filter.setScale(0.75f);
            bearGpuImage.setFilter(filter);
            //bearGpuImage.setFilter(new GPUImagePosterizeFilter());
            map = getFillBitmap(bearGpuImage.getBitmapWithFilterApplied(), true, true, human);
            //map = getFillBitmap(bitmap, true, true, human);
        }
        imageView.setImageBitmap(map);
    }

    // if revert, white is fill
    // if not revert, black is fill
    private Bitmap getFillBitmap(Bitmap original, boolean runGreyScale, boolean revert, boolean human) {
        int width = original.getWidth();
        int height = original.getHeight();

        int threshold = human ? 0x25 : 0x40;

        Bitmap greyBitmap;

        // create greyscale bitmap
        if(runGreyScale) {
            greyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(greyBitmap);
            Paint paint = new Paint();
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            //imageView.setColorFilter(filter);
            c.drawBitmap(original, 0, 0, paint);
        } else{
            greyBitmap = original;
        }

        // scale down

        // calculate scale
        float totalPixel = MAX_S / (PER_PIXEL_S * 1.0f);
        double scale = Math.sqrt(width * height / totalPixel);
        android.util.Log.d("MITA", "scale: " + scale);
        Bitmap smallGreyBitmap = Bitmap.createScaledBitmap(greyBitmap, (int)(width/scale), (int)(height/scale), false);

        //imageView.setImageBitmap(smallGreyBitmap);

        int smallX = smallGreyBitmap.getWidth();
        int smallY = smallGreyBitmap.getHeight();

        android.util.Log.d("MITA", "smallX: " + smallX + " smallY: " + smallY);

        // size of each filling pixel
        int mapWidth = RADIUS_FILL * 2 * smallX;
        int mapHeight = RADIUS_FILL * 2 * smallY;
        Bitmap map = Bitmap.createBitmap(mapWidth, mapHeight, Bitmap.Config.ARGB_8888);
        Canvas mapCanvas = new Canvas(map);
        Paint mapPaint = new Paint();
        mapPaint.setAntiAlias(true);
        mapPaint.setStyle(Paint.Style.FILL);
        mapPaint.setColor(Color.BLACK);

        int[][] result = new int[smallY][smallX];

        int humanCrop = (int)((smallX-smallY)/2.0f) + 5;

        for(int y = 0; y < smallY; y++) {
            StringBuilder sb = new StringBuilder();
            StringBuilder sbShifted = new StringBuilder();
            for(int x = 0; x < smallX; x++) {
                int pixel = smallGreyBitmap.getPixel(x, y);
                //sb.append(Integer.toHexString(pixel)+" ");

                int shiftedPixel = pixel & 0x000000FF;

                int drawX = x * RADIUS_FILL * 2 + RADIUS_FILL;
                int drawY = y * RADIUS_FILL * 2 + RADIUS_FILL;

                if(shiftedPixel < threshold) {
                    if(!revert) {
                        // SPACE
                        sbShifted.append(" ");
                        result[y][x] = 0;
                    }else{
                        if(human && (x < humanCrop || x > smallX-humanCrop)){
                            continue;
                        }
                        // FILL
                        sbShifted.append("0");
                        mapCanvas.drawCircle(drawX, drawY, RADIUS_FILL, mapPaint);

                        result[y][x] = 1;
                    }
                } else{
                    if(!revert) {
                        if(human && (x < humanCrop || x > smallX-humanCrop)){
                            continue;
                        }

                        // FILL
                        sbShifted.append("0");
                        mapCanvas.drawCircle(drawX, drawY, RADIUS_FILL, mapPaint);

                        result[y][x] = 1;
                    }else{

                        // SPACE
                        sbShifted.append(" ");

                        result[y][x] = 0;
                    }
                }
            }
            sb.append("\n");
            //android.util.Log.d("MITA", sb.toString());
            sbShifted.append("\n");
            //android.util.Log.d("MITA", sbShifted.toString());

        }
/*
        for(int i = 0; i < result.length; i++) {
            android.util.Log.d("MITA", Arrays.toString(result[i]));
        }
        */

        return map;
    }
}
