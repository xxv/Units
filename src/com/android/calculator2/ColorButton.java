/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import info.staticfree.android.units.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Button with click-animation effect.
 */
class ColorButton extends Button implements OnClickListener {
    int CLICK_FEEDBACK_COLOR;
    static final int CLICK_FEEDBACK_INTERVAL = 10;
    static final int CLICK_FEEDBACK_DURATION = 350;

    float mTextX;
    float mTextY;
    long mAnimStart;
    OnClickListener mListener;
    Paint mFeedbackPaint;
    private final Context mContext;

    public ColorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        //final Units units = (Units)context;
        init();

        //mListener = units.buttonListener;
        //setOnClickListener(this);
    }

    private void init() {
        final Resources res = getResources();

        CLICK_FEEDBACK_COLOR = res.getColor(R.color.magic_flame);
        mFeedbackPaint = new Paint();
        mFeedbackPaint.setStyle(Style.STROKE);
        mFeedbackPaint.setStrokeWidth(2);
        getPaint().setColor(res.getColor(R.color.button_text));

        mAnimStart = -1;


        //adjustFontSize();
    }

    private static final int HVGA_WIDTH_PIXELS  = 320;

    /**
     * The font sizes in the layout files are specified for a HVGA display.
     * Adjust the font sizes accordingly if we are running on a different
     * display.
     */
    public void adjustFontSize() {
        final float fontPixelSize = getTextSize();
        final Display display = ((WindowManager)(mContext.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay();
        final int h = Math.min(display.getWidth(), display.getHeight());
        final float ratio = (float)h/HVGA_WIDTH_PIXELS;
        setTextSize(TypedValue.COMPLEX_UNIT_PX, fontPixelSize*ratio);
    }

    // XXX doesn't work
    public void adjustFontSizeToFit() {
        final Paint newPaint = new Paint(getPaint());
        float newX = mTextX;
        for (int i = 0; newX < 10 && i < 10; i++){

        	newPaint.setTextSize(newPaint.getTextSize() * 0.9f);
        	newX = (getWidth() - newPaint.measureText(getText().toString())) / 2;
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, newPaint.getTextSize());

    }


    public void onClick(View view) {
        mListener.onClick(this);
    }


    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
    	measureText();
        adjustFontSizeToFit();
        measureText();
    }

    private void measureText() {
        final Paint paint = getPaint();
        mTextX = (getWidth() - paint.measureText(getText().toString())) / 2;
        mTextY = (getHeight() - paint.ascent() - paint.descent()) / 2;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        measureText();
    }

    private void drawMagicFlame(int duration, Canvas canvas) {
        final int alpha = 255 - 255 * duration / CLICK_FEEDBACK_DURATION;
        final int color = CLICK_FEEDBACK_COLOR | (alpha << 24);

        mFeedbackPaint.setColor(color);
        canvas.drawRect(1, 1, getWidth() - 1, getHeight() - 1, mFeedbackPaint);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mAnimStart != -1) {
            final int animDuration = (int) (System.currentTimeMillis() - mAnimStart);

            if (animDuration >= CLICK_FEEDBACK_DURATION) {
                mAnimStart = -1;
            } else {
                drawMagicFlame(animDuration, canvas);
                postInvalidateDelayed(CLICK_FEEDBACK_INTERVAL);
            }
        } else if (isPressed()) {
            drawMagicFlame(0, canvas);
        }

        final CharSequence text = getText();
        canvas.drawText(text, 0, text.length(), mTextX, mTextY, getPaint());
    }

    public void animateClickFeedback() {
        mAnimStart = System.currentTimeMillis();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean result = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                animateClickFeedback();
                break;
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_CANCEL:
                invalidate();
                break;
        }

        return result;
    }
}
