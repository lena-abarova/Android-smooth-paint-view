package com.abarova.smoothpaint.example;

import android.util.Log;

public class MyLogs {

    public static void LOG(String theClass, String theMethod, String theComment) {
        Log.d("!!! MyLogs !!!", theClass + "  :: " + theMethod + " : " + theComment);
    }
}