package com.thalmic.android.sample.multiplemyos;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.thalmic.android.sample.multiplemyos.R;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Vector3;

public class FingerActivity extends Activity
{
    public LinearLayout mContent;
    public signature mSignature;

    public Button mClear;
    public static String tempDir;
    public int count = 1;
    public String current = null;
    public static Bitmap mBitmap;
    public View mView;
    public File mypath;

    private String uniqueId;
    private EditText yourName;

    private ArrayList<Myo> mKnownMyos = new ArrayList<Myo>();

    private Pose mCurrPose;
    private int mCycleCount = 0;

    public static final float STROKE_WIDTH = 5f;
    public static final float HALF_STROKE_WIDTH = STROKE_WIDTH / 2;

    private DeviceListener mListener = new AbstractDeviceListener() {
        // Every time the SDK successfully pairs with a Myo armband, this function will be called.
        //
        // You can rely on the following rules:
        //  - onPair() will only be called once for each Myo device
        //  - no other events will occur involving a given Myo device before onPair() is called with it
        //
        // If you need to do some kind of per-Myo preparation before handling events, you can safely do it in onPair().
        @Override
        public void onDisconnect(Myo myo, long timestamp) {
        }
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            mCurrPose = pose;
        }

        @Override
        public void onGyroscopeData (Myo myo, long timestamp, Vector3 gyro) {
        //public void onAccelerometerData (Myo myo, long timestamp, Vector3 accel) {
            //mAdapter.setMessage(myo, "Myo " + identifyMyo(myo) + " moved at " + accel.x() + ".");
            Log.i("FingerActivity", gyro.toString());

            if (mCycleCount < 18) {
                mCycleCount++;
                return;
            }
            else if (mCycleCount == 18) {
                mCycleCount = 0;
            }

            float eventX = (float) gyro.x() * 8 + 550; //event.getX();
            float eventY = (float) gyro.y() * 8 + 250; //event.getY();

            if (mKnownMyos.size() < 2 && !mKnownMyos.contains(myo)) {
                mKnownMyos.add(myo);
            }

            int index = identifyMyo(myo) - 1;
            Log.i("FingerActivity", Integer.toString(index));
            if (index == 0) {
                mSignature.paint.setColor(Color.BLUE);
            }
            else {
                mSignature.paint.setColor(Color.RED);
            }

            if (mCurrPose == Pose.WAVE_IN) {
                mSignature.clear();
            }
            else if (mCurrPose == Pose.FINGERS_SPREAD) {
                mSignature.path.moveTo(eventX, eventY);
                mSignature.lastTouchX = eventX;
                mSignature.lastTouchY = eventY;
            }
            else if (mCurrPose == Pose.WAVE_OUT) {
                // exit app
                System.exit(0);
            }
            else {
                resetDirtyRect(eventX, eventY);
                // Figure out which myo the signal came from

                mSignature.path.lineTo(eventX, eventY);
            }

            // Invalidate
            mSignature.invalidate((int) (mSignature.dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (mSignature.dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (mSignature.dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (mSignature.dirtyRect.bottom + HALF_STROKE_WIDTH));

            mSignature.lastTouchX = eventX;
            mSignature.lastTouchY = eventY;

            return;
        }

        private void expandDirtyRect(float historicalX, float historicalY)
        {
            if (historicalX < mSignature.dirtyRect.left)
            {
                mSignature.dirtyRect.left = historicalX;
            }
            else if (historicalX > mSignature.dirtyRect.right)
            {
                mSignature.dirtyRect.right = historicalX;
            }

            if (historicalY < mSignature.dirtyRect.top)
            {
                mSignature.dirtyRect.top = historicalY;
            }
            else if (historicalY > mSignature.dirtyRect.bottom)
            {
                mSignature.dirtyRect.bottom = historicalY;
            }
        }

        private void resetDirtyRect(float eventX, float eventY)
        {
            mSignature.dirtyRect.left = Math.min(mSignature.lastTouchX, eventX);
            mSignature.dirtyRect.right = Math.max(mSignature.lastTouchX, eventX);
            mSignature.dirtyRect.top = Math.min(mSignature.lastTouchY, eventY);
            mSignature.dirtyRect.bottom = Math.max(mSignature.lastTouchY, eventY);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_finger);
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        mContent = (LinearLayout) findViewById(R.id.linearLayout);
        mSignature = new signature(this, null);
        mSignature.setBackgroundColor(Color.WHITE);
        mContent.addView(mSignature, LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        //mClear = (Button)findViewById(R.id.clear);
        //mGetSign = (Button)findViewById(R.id.getsign);
        //mGetSign.setEnabled(false);
        //mCancel = (Button)findViewById(R.id.cancel);
        mView = mContent;

        //   yourName = (EditText) findViewById(R.id.yourName);

        mClear.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                Log.v("log_tag", "Panel Cleared");
                mSignature.clear();
            }
        });

        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final int pairingCount = 2;
        hub.pairWithAdjacentMyos(pairingCount);
        hub.addListener(mListener);
    }

    @Override
    protected void onDestroy()
    {
        Log.w("GetSignature", "onDestory");
        super.onDestroy();

        Hub.getInstance().removeListener(mListener);
        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will unpair all paired Myo devices,
            // and disconnect any that are connected.
            Hub.getInstance().shutdown();
        }
    }

    private String getTodaysDate()
    {

        final Calendar c = Calendar.getInstance();
        int todaysDate =     (c.get(Calendar.YEAR) * 10000) +
                ((c.get(Calendar.MONTH) + 1) * 100) +
                (c.get(Calendar.DAY_OF_MONTH));
        Log.w("DATE:",String.valueOf(todaysDate));
        return(String.valueOf(todaysDate));

    }

    private String getCurrentTime()
    {

        final Calendar c = Calendar.getInstance();
        int currentTime =     (c.get(Calendar.HOUR_OF_DAY) * 10000) +
                (c.get(Calendar.MINUTE) * 100) +
                (c.get(Calendar.SECOND));
        Log.w("TIME:",String.valueOf(currentTime));
        return(String.valueOf(currentTime));

    }


    private boolean prepareDirectory()
    {
        try
        {
            if (makedirs())
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this, "Could not initiate File System.. Is Sdcard mounted properly?", 1000).show();
            return false;
        }
    }

    private boolean makedirs()
    {
        File tempdir = new File(tempDir);
        if (!tempdir.exists())
            tempdir.mkdirs();

        if (tempdir.isDirectory())
        {
            File[] files = tempdir.listFiles();
            for (File file : files)
            {
                if (!file.delete())
                {
                    System.out.println("Failed to delete " + file);
                }
            }
        }
        return (tempdir.isDirectory());
    }

    public class signature extends View
    {
        public Paint paint = new Paint();
        public Path path = new Path();

        public float lastTouchX;
        public float lastTouchY;
        public final RectF dirtyRect = new RectF();

        public signature(Context context, AttributeSet attrs)
        {
            super(context, attrs);
            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeWidth(STROKE_WIDTH);
        }

        public void save(View v)
        {
            Log.v("log_tag", "Width: " + v.getWidth());
            Log.v("log_tag", "Height: " + v.getHeight());
            if(mBitmap == null)
            {
                mBitmap =  Bitmap.createBitmap (mContent.getWidth(), mContent.getHeight(), Bitmap.Config.RGB_565);
            }
            Canvas canvas = new Canvas(mBitmap);
            try
            {

                FileOutputStream mFileOutStream = new FileOutputStream(mypath);

                v.draw(canvas);
                mBitmap.compress(Bitmap.CompressFormat.PNG, 90, mFileOutStream);
                mFileOutStream.flush();
                mFileOutStream.close();
                String url = Images.Media.insertImage(getContentResolver(), mBitmap, "title", null);
                //Log.v("log_tag","url: " + url);

//                //In case you want to delete the file
//                boolean deleted = mypath.delete();
//                Log.v("log_tag","deleted: " + mypath.toString() + deleted);
//                //If you want to convert the image to string use base64 converter
            }
            catch(Exception e)
            {
                Log.v("log_tag", e.toString());
            }
        }

        public void clear()
        {
            path.reset();
            invalidate();
        }

        public void refresh(int a, int b, int c, int d)
        {
            invalidate(a, b, c, d);
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event)
        {
            float eventX = event.getX();
            float eventY = event.getY();

            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(eventX, eventY);
                    lastTouchX = eventX;
                    lastTouchY = eventY;
                    return true;

                case MotionEvent.ACTION_MOVE:

                case MotionEvent.ACTION_UP:

                    resetDirtyRect(eventX, eventY);
                    int historySize = event.getHistorySize();
                    for (int i = 0; i < historySize; i++)
                    {
                        float historicalX = event.getHistoricalX(i);
                        float historicalY = event.getHistoricalY(i);
                        expandDirtyRect(historicalX, historicalY);
                        path.lineTo(historicalX, historicalY);
                    }
                    path.lineTo(eventX, eventY);
                    break;

                default:
                    debug("Ignored touch event: " + event.toString());
                    return false;
            }

            invalidate((int) (dirtyRect.left - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.top - HALF_STROKE_WIDTH),
                    (int) (dirtyRect.right + HALF_STROKE_WIDTH),
                    (int) (dirtyRect.bottom + HALF_STROKE_WIDTH));

            lastTouchX = eventX;
            lastTouchY = eventY;

            return true;
        }

        private void debug(String string)
        {
        }

        private void expandDirtyRect(float historicalX, float historicalY)
        {
            if (historicalX < dirtyRect.left)
            {
                dirtyRect.left = historicalX;
            }
            else if (historicalX > dirtyRect.right)
            {
                dirtyRect.right = historicalX;
            }

            if (historicalY < dirtyRect.top)
            {
                dirtyRect.top = historicalY;
            }
            else if (historicalY > dirtyRect.bottom)
            {
                dirtyRect.bottom = historicalY;
            }
        }

        private void resetDirtyRect(float eventX, float eventY)
        {
            dirtyRect.left = Math.min(lastTouchX, eventX);
            dirtyRect.right = Math.max(lastTouchX, eventX);
            dirtyRect.top = Math.min(lastTouchY, eventY);
            dirtyRect.bottom = Math.max(lastTouchY, eventY);
        }
    }

    // This is a utility function implemented for this sample that maps a Myo to a unique ID starting at 1.
    // It does so by looking for the Myo object in mKnownMyos, which onPair() adds each Myo into as it is paired.
    private int identifyMyo(Myo myo) {
        return mKnownMyos.indexOf(myo) + 1;
    }
}