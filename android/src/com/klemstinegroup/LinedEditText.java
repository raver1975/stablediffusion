package com.klemstinegroup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.util.AttributeSet;
import android.widget.EditText;

public class LinedEditText extends EditText {
    private Paint mPaint;

    // we need this constructor for LayoutInflater
    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setColor(Color.RED); //SET YOUR OWN COLOR HERE
    }

    public LinedEditText(Context context) {
        super(context);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(3);
        mPaint.setColor(Color.RED); //SET YOUR OWN COLOR HERE
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //int count = getLineCount();

        int height = getHeight();
        int line_height = getLineHeight();

        int count = height / line_height;

        if (getLineCount() > count)
            count = getLineCount();//for long text with scrolling

        Paint paint = mPaint;

        Layout layout = getLayout();
        if (layout == null) { // Layout may be null right after change to the view
            // Do nothing
        }

        String s = getText().toString();
        for (int offset = 0; offset < s.length(); offset++) {
            if (s.charAt(offset) != '\n') {
                continue;
            }
            int lineOfText = layout.getLineForOffset(offset);
            int xCoordinate = (int) layout.getPrimaryHorizontal(offset);
            int yCoordinate = (int) ((lineOfText+1.6f)* getLineHeight());
            canvas.drawLine(0, yCoordinate, getWidth(), yCoordinate, paint);
        }

        super.onDraw(canvas);
    }
}