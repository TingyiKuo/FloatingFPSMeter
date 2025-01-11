package org.amito4.floatfpsmeter;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.view.Choreographer;
import android.graphics.drawable.ColorDrawable;
import android.view.GestureDetector;
import android.app.Dialog;
import android.view.Window;

public class FPSService extends Service {
    private static final String TAG = "TingyiFPSService";
    private Handler handler;
    private long lastFrameTime;
    private int frameCount;
    private float currentFps;
    private Choreographer.FrameCallback frameCallback;
    
    private WindowManager windowManager;
    private View floatingView;
    private TextView fpsTextView;
    private WindowManager.LayoutParams params;
    
    private float initialX;
    private float initialY;
    private float initialTouchX;
    private float initialTouchY;
    private GestureDetector gestureDetector;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        lastFrameTime = SystemClock.elapsedRealtime();
        
        createFloatingWindow();

        frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                calculateFPS();
                Choreographer.getInstance().postFrameCallback(this);
            }
        };
        
        startMeasuring();
    }

    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_fps, null);
        fpsTextView = floatingView.findViewById(R.id.fpsTextView);

        // Initialize GestureDetector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.d(TAG, "onDoubleTap++");
                showCloseDialog();
                //stopSelf();
                return true;
            }
        });

        params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // Update touch listener to handle both drag and double tap
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = (int) (initialX + (event.getRawX() - initialTouchX));
                        params.y = (int) (initialY + (event.getRawY() - initialTouchY));
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingView, params);
    }

    private void startMeasuring() {
        Choreographer.getInstance().postFrameCallback(frameCallback);
    }

    private void calculateFPS() {
        long currentTime = SystemClock.elapsedRealtime();
        frameCount++;

        if (currentTime - lastFrameTime >= 1000) {
            currentFps = frameCount * 1000f / (currentTime - lastFrameTime);
            frameCount = 0;
            lastFrameTime = currentTime;
            
            Log.d(TAG, String.format("Calculated FPS: %.1f", currentFps));
            
            handler.post(() -> {
                if (fpsTextView != null) {
                    fpsTextView.setText(String.format("%.1f FPS", currentFps));
                }
            });
        }
    }

    private void showCloseDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_close_fps, null);

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        
        // Set dialog window properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, 
                           WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        // Set button click listeners
        dialogView.findViewById(R.id.btn_yes).setOnClickListener(v -> {
            dialog.dismiss();
            stopSelf();
        });
        
        dialogView.findViewById(R.id.btn_no).setOnClickListener(v -> {
            dialog.dismiss();
        });
        
        dialog.show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (frameCallback != null) {
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }
} 