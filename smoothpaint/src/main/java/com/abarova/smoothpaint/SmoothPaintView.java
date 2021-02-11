package com.abarova.smoothpaint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;


/**
 * Abstract class to smoothly draw.
 * <p>
 * Each child view should have own behaviour using this core drawing functional.
 */
public abstract class SmoothPaintView extends AppCompatImageView {

    // drawing path
    protected CustomPath mDrawPath;
    // drawing and canvas paint
    protected Paint mDrawPaint, mCanvasPaint;
    // initial color
    protected int mPaintColor = Color.BLACK;
    // canvas
    protected Canvas mDrawCanvas;
    // canvas bitmap
    protected Bitmap mCanvasBitmap;
    // brush sizes
    protected float mBrushSize;
    // optimizes painting by invalidating the smallest possible area.
    private final RectF dirtyRect = new RectF();
    // stylus only is required
    protected boolean mStylusOnlyAllowed = false;
    // turns off path drawings
    protected boolean mWaitingForTouchToResume = false;
    // remember latest drawing state to ignore too many event calls
    private boolean mLastDrawingState = false;


    public SmoothPaintView(Context context) {
        super(context);
        setupDrawingStyle();
    }

    public SmoothPaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupDrawingStyle();
    }

    /**
     * Setup drawing, prepare for drawing and setup paint stroke properties
     */
    private void setupDrawingStyle() {
        mBrushSize = 10;
        mDrawPath = new CustomPath();
        mDrawPaint = new Paint();
        mDrawPaint.setColor(mPaintColor);
        mDrawPaint.setAntiAlias(true);
        mDrawPaint.setStrokeWidth(mBrushSize);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setStrokeJoin(Paint.Join.ROUND);
        mDrawPaint.setStrokeCap(Paint.Cap.ROUND);
        mCanvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    protected void updateBrushColor(int newColor) {
        invalidate();
        mPaintColor = newColor;
        mDrawPaint.setColor(mPaintColor);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected void updateBrushColor(float r, float g, float b) {
        Color color = Color.valueOf(r, g, b);
        updateBrushColor(color.toArgb());
    }

    /**
     * Change brush color directly - use this only for specific scenario.
     * Otherwise is recommended to use [updateBrushColor(int newColor)]
     *
     * @param color - Argb color
     */
    protected void changeBrushColor(int color) {
        mDrawPaint.setColor(color);
    }

    protected void updateBrushSize(int newBrushSize) {
        invalidate();
        mBrushSize = newBrushSize;
        mDrawPaint.setStrokeWidth(mBrushSize);
    }

    /**
     * Change brush width directly - use this only for specific scenario.
     * Otherwise is recommended to use [updateBrushSize(int newBrushSize)]
     *
     * @param width - int size
     */
    protected void setBrushSize(int width) {
        mDrawPaint.setStrokeWidth(width);
    }

    public void setAllowStylusOnly(boolean newValue) {
        mStylusOnlyAllowed = newValue;
    }


    // size assigned to view
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mDrawCanvas = new Canvas(mCanvasBitmap);
        mDrawCanvas.drawColor(Color.argb(255, 254, 253, 251));
    }

    // draw the view - will be called after touch event
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mCanvasBitmap, 0, 0, mCanvasPaint);
        canvas.drawPath(mDrawPath, mDrawPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        boolean isStylusAction = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;
        boolean isActionDenied = mStylusOnlyAllowed && !isStylusAction;

        // update UI state about not allowed drawing action
        updateDrawingWarnState(
                event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE,
                isActionDenied);

        // ignore non-stylus events if this is a requirement
        if (isActionDenied) {
            return true;
        }

        //respond to tap down/and & move events
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDrawPath.moveTo(touchX, touchY);
                // There is no end point yet, so don't waste cycles invalidating.
                break;

            case MotionEvent.ACTION_MOVE:
                // When the hardware tracks events faster than they are delivered, the
                // event will contain a history of those skipped points.
                for (int i = 0; i < event.getHistorySize(); i++) {
                    float historicalX = event.getHistoricalX(i);
                    float historicalY = event.getHistoricalY(i);
                    addPointToPath(historicalX, historicalY);
                }

                // After replaying history, connect the line to the touch point.
                addPointToPath(touchX, touchY);
                break;

            case MotionEvent.ACTION_UP:
                // When the hardware tracks events faster than they are delivered, the
                // event will contain a history of those skipped points.
                for (int i = 0; i < event.getHistorySize(); i++) {
                    float historicalX = event.getHistoricalX(i);
                    float historicalY = event.getHistoricalY(i);
                    addPointToPath(historicalX, historicalY);
                }

                // After replaying history, connect the line to the touch point.
                addPointToPath(touchX, touchY);

                if (!mWaitingForTouchToResume)
                    mDrawCanvas.drawPath(mDrawPath, mDrawPaint);
                invalidate();

                onPathDrawn(mDrawPath);

                //reset path
                mDrawPath = new CustomPath();
                break;

            default:
                return false;
        }

        // Include half the stroke width to avoid clipping.
        invalidate(
                (int) (dirtyRect.left - mBrushSize / 2),
                (int) (dirtyRect.top - mBrushSize / 2),
                (int) (dirtyRect.right + mBrushSize / 2),
                (int) (dirtyRect.bottom + mBrushSize / 2));

        return true;
    }


    //start new drawing
    public void cleanUpDrawingArea() {
        if (mDrawCanvas != null) {
            mDrawCanvas.drawColor(Color.argb(255, 254, 253, 251));
        }
        invalidate();
    }

    private void addPointToPath(float x, float y) {
        mDrawPath.lineTo(x, y);
        expandDirtyRect(x, y);
    }

    /**
     * Called when replaying history to ensure the dirty region includes all points.
     */
    private void expandDirtyRect(float historicalX, float historicalY) {
        if (historicalX < dirtyRect.left) {
            dirtyRect.left = historicalX;
        } else if (historicalX > dirtyRect.right) {
            dirtyRect.right = historicalX;
        }
        if (historicalY < dirtyRect.top) {
            dirtyRect.top = historicalY;
        } else if (historicalY > dirtyRect.bottom) {
            dirtyRect.bottom = historicalY;
        }
    }

    /**
     * Update drawing state and push event only if state is changed since latest call
     *
     * @param isDrawing - current user drawing state
     */
    private void updateDrawingWarnState(boolean isDrawing, boolean isActionDenied) {
        if (mLastDrawingState != isDrawing) {
            this.mLastDrawingState = isDrawing;
            onChangeDrawingWarnState(isDrawing && isActionDenied);
        }
    }

    /**
     * Called when a path was drawn and need to compute result logic
     *
     * @param path - just drawn path
     */
    protected abstract void onPathDrawn(CustomPath path);

    /**
     * Called to update drawing warn if stylus is on but user try to use finger
     *
     * @param isOn - true when tap or move & drawing is not allowed
     */
    protected abstract void onChangeDrawingWarnState(boolean isOn);
}
