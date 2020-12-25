package com.pedro.rtpstreamer.displayexample;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.accessibility.AccessibilityEvent;

public class Accessibility extends AccessibilityService {
    private static String TAG = "MyAccessibilitySvc";
    private Handler mHandler;
    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread handlerThread = new HandlerThread("auto-handler");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    public final void click(float x, float y) {
        if (Build.VERSION.SDK_INT >= 24) {
            Path path = new Path();
            path.moveTo((float) x, (float) y);
            GestureDescription.Builder builder = new GestureDescription.Builder();
            GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 1)).build();
            dispatchGesture(gestureDescription, null, null);
        }
    }
}
