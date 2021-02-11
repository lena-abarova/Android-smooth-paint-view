package com.abarova.smoothpaint.example;

import android.content.Context;

import com.abarova.smoothpaint.CustomPath;
import com.abarova.smoothpaint.SmoothPaintView;

public class CustomPaintView extends SmoothPaintView {

    public CustomPaintView(Context context) {
        super(context);
    }

    @Override
    protected void onPathDrawn(CustomPath path) {
        MyLogs.LOG("CustomPaintView", "onPathDrawn", "...");
    }

    @Override
    protected void onChangeDrawingWarnState(boolean isOn) {
        MyLogs.LOG("CustomPaintView", "onChangeDrawingWarnState", "isOn: " + isOn);
    }
}
