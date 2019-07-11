package com.sammie.drawsignature.View;

import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;

public class DrawSignatureView extends View {
    public static final float TOUCH_TOLERANCE = 10;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private Paint paintScreen;
    private Paint paintLine;
    private HashMap<Integer, Path> pathHashMap;
    private HashMap<Integer, Point> previousPointMap;
    private OutputStream outputStream;

    public DrawSignatureView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void init() {
        paintScreen = new Paint();
        paintLine = new Paint();
        paintLine.setAntiAlias(true);
        paintLine.setColor(Color.BLACK);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setStrokeWidth(5);
        paintLine.setStrokeCap(Paint.Cap.ROUND);

        pathHashMap = new HashMap<>();
        previousPointMap = new HashMap<>();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmap.eraseColor(Color.WHITE);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.drawBitmap(bitmap, 0, 0, paintScreen);

        for (Integer key : pathHashMap.keySet()) {
            canvas.drawPath(pathHashMap.get(key), paintLine);

        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked(); // event type (up or down)
        int actionIndex = event.getActionIndex();//pointer type (finger, mouse )
        Log.d("ScreenTouched", String.valueOf(event.getActionMasked()));

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_UP) {
            touchStarted(event.getX(actionIndex),
                    event.getY(actionIndex),
                    event.getPointerId(actionIndex));


        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            touchEnded(event.getPointerId(actionIndex));

        } else {
            touchMoved(event);
        }

        invalidate(); // redraw the screen

        return true;
    }

    private void touchMoved(MotionEvent event) {
        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerId = event.getPointerId(i);
            int pointerIndex = event.findPointerIndex(pointerId);

            if (pathHashMap.containsKey(pointerId)) {
                float newX = event.getX(pointerIndex);
                float newY = event.getY(pointerIndex);

                Path path = pathHashMap.get(pointerId);
                Point point = previousPointMap.get(pointerId);

                //calculate how far the user moved from the last update
                float deltaX = Math.abs(newX - point.x);
                float deltaY = Math.abs(newY - point.y);

                //if the distance is significant enough to b considered a movement then...
                if (deltaX >= TOUCH_TOLERANCE || deltaY >= TOUCH_TOLERANCE) {
                    //move the path to new location
                    path.quadTo(point.x, point.y,
                            (newX + point.x) / 2,
                            (newY + point.y) / 2);

                    //store the new coordinates
                    point.x = (int) newX;
                    point.y = (int) newY;

                }

            }
        }

    }

    public void setDrawingColor(int color) {

        paintLine.setColor(color);

    }

    public int getDrawigColor() {
        return paintLine.getColor();
    }

    public int getLineWidth() {
        return (int) paintLine.getStrokeWidth();

    }

    public void setLineWidth(int width) {
        paintLine.setStrokeWidth(width);
    }

    public void clear() {
        pathHashMap.clear(); //removes all points
        previousPointMap.clear();
        bitmap.eraseColor(Color.WHITE);
        invalidate(); //refresh screen

    }

    private void touchEnded(int pointerId) {
        Path path = pathHashMap.get(pointerId);// get corresponding path
        bitmapCanvas.drawPath(path, paintLine); //draw to bitmap canvas
        path.reset();
    }

    private void touchStarted(float x, float y, int pointerId) {
        Path path; //store the path for given touch
        Point point; // store the last point in path

        if (pathHashMap.containsKey(pointerId)) {
            path = pathHashMap.get(pointerId);
            point = previousPointMap.get(pointerId);

        } else {
            path = new Path();
            pathHashMap.put(pointerId, path);
            point = new Point();
            previousPointMap.put(pointerId, point);

        }
        //move to the coordinate of the touch
        path.moveTo(x, y);
        point.x = (int) x;
        point.y = (int) y;

    }

    public void saveImage() {
        String fileName = "fromDrawingSignatureApp" + System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "images/jgp");

        //get uri for the location to save the file
        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);

        try {
            OutputStream outputStream =
                    getContext().getContentResolver().openOutputStream(uri);
            //copy bitmap to the  output stream
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream); //this is our image

            try {
                outputStream.flush();
                outputStream.close();
                Toast message = Toast.makeText(getContext(), "your image is Saved", Toast.LENGTH_SHORT);
                message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
//                message.setDuration(Toast.LENGTH_LONG);
                message.show();


            } catch (IOException e) {
                Toast message = Toast.makeText(getContext(), "your data image is Saved", Toast.LENGTH_SHORT);
                message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
//                message.setDuration(Toast.LENGTH_LONG);
                message.show();
            }
        } catch (FileNotFoundException e) {

            Toast message = Toast.makeText(getContext(), "Image not Saved", Toast.LENGTH_SHORT);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2, message.getYOffset() / 2);
//                message.setDuration(Toast.LENGTH_LONG);
            message.show();
//            e.printStackTrace();
        }
    }

    public void loadImageFromStorage(String path) {

        try {
            File f = new File(path, "profile.jpg");
            Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
            // ImageView imageView = findViewById(R.id.savedImageView);
            // imageView.setImageBitmap(b);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void saveImageToExternalStorage() {

        //Source: https://stackoverflow.com/questions/17674634/saving-and-reading-bitmaps-images-from-internal-memory-in-android
        String filename = "Pikasso" + System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");


        // get a URI for the location to save the file
        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, values);


        try {
            outputStream =
                    getContext().getContentResolver().openOutputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + "test.jpeg");

        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);


        try {
            outputStream.flush();
            outputStream.close();

            Toast message = Toast.makeText(getContext(), "Image Saved", Toast.LENGTH_LONG);
            message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                    message.getYOffset() / 2);
            message.show();
        } catch (IOException e) {
            e.printStackTrace();
        }


        try {
            MediaStore.Images.Media.insertImage(getContext().getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void saveToInternalStorage() {
        ContextWrapper cw = new ContextWrapper(getContext());

        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, "doodleApp.png");

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.flush();
                fos.close();
                Log.d("Image:", directory.getAbsolutePath());
                Toast message = Toast.makeText(getContext(), "Image Saved +" + directory.getAbsolutePath(), Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                        message.getYOffset() / 2);
                message.show();
                message.setDuration(Toast.LENGTH_LONG);
            } catch (IOException e) {
                Log.d("Image:","Not saved");
                Toast message = Toast.makeText(getContext(), "Image not  Saved ", Toast.LENGTH_LONG);
                message.setGravity(Gravity.CENTER, message.getXOffset() / 2,
                        message.getYOffset() / 2);
                message.show();
            }
        }
        // return directory.getAbsolutePath();
    }


}
