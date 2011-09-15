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
import info.staticfree.android.units.Units;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Button with click-animation effect.
 */
class ColorButton extends Button implements OnClickListener {
    private int CLICK_FEEDBACK_COLOR;
    private static final int CLICK_FEEDBACK_INTERVAL = 10;
    private static final int CLICK_FEEDBACK_DURATION = 350;

    private float mTextX;
    private float mTextY;
    private long mAnimStart;
    private OnClickListener mListener;
    private Paint mFeedbackPaint;

    private Drawable mEllipsis;
    private final String mLongpressText;
    private Paint mLongpressTextPaint;

    public ColorButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
        if (attrs.getAttributeBooleanValue(Units.XMLNS, "longpressEllipsis", false)){
        	final Resources res = getResources();
        	mEllipsis = res.getDrawable(R.drawable.button_ellipsis);
        }

        mLongpressText = attrs.getAttributeValue(Units.XMLNS, "longpressText");
    }

    private void init() {
        final Resources res = getResources();

        CLICK_FEEDBACK_COLOR = res.getColor(R.color.magic_flame);
        mFeedbackPaint = new Paint();
        mFeedbackPaint.setStyle(Style.FILL_AND_STROKE);
        mFeedbackPaint.setStrokeWidth(2);
        final Paint textPaint = getPaint();
        textPaint.setColor(res.getColor(R.color.button_text));
        mLongpressTextPaint = new Paint(textPaint);
        mLongpressTextPaint.setAlpha(127);

        mAnimStart = -1;

        //

    }

    /**
     * Resizes the text so that it is as big as possible, but still fits comfortably on the button.
     * This allows arbitrary text to be placed on the buttons.
     */
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

        if (mLongpressText != null){
        	mLongpressTextPaint.measureText(mLongpressText);
        	final float textSize = (getHeight() - paint.getTextSize()) / 2 - 4;
        	mLongpressTextPaint.setTextAlign(Align.RIGHT);
            mLongpressTextPaint.setTextSize(textSize);
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        measureText();
    }

    private static final int MAX_ALPHA = 64;
    private void drawMagicFlame(int duration, Canvas canvas) {
        final int alpha = MAX_ALPHA - MAX_ALPHA * duration / CLICK_FEEDBACK_DURATION;
        final int color = CLICK_FEEDBACK_COLOR | (alpha << 24);

        mFeedbackPaint.setColor(color);
        canvas.drawRect(1, 1, getWidth() - 1, getHeight() - 1, mFeedbackPaint);
    }

    private static final int PADDING = 10;
    @Override
    public void onDraw(Canvas canvas) {

    	if (mEllipsis != null){
	        mEllipsis.setBounds(canvas.getClipBounds());
	        mEllipsis.draw(canvas);
    	}

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

        if (mLongpressText != null){

        	canvas.drawText(mLongpressText, getWidth() - PADDING, -mLongpressTextPaint.ascent() + PADDING, mLongpressTextPaint);
        }
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
            	setPressed(true); // not sure why this is needed here

            case MotionEvent.ACTION_CANCEL:

                invalidate();
                break;
        }

        return result;
    }
}