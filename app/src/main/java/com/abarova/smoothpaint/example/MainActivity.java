package com.abarova.smoothpaint.example;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.os.Bundle;
import android.widget.FrameLayout;

import com.abarova.smoothpaint.example.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding mBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        bindSmoothPaintView();
    }

    private void bindSmoothPaintView() {
        FrameLayout frameLayout = mBinding.zonePaint;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);

        CustomPaintView customPaintView = new CustomPaintView(MainActivity.this);
        customPaintView.setLayoutParams(params);
        frameLayout.addView(customPaintView);
    }
}