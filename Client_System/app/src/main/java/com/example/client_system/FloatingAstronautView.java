package com.example.client_system;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class FloatingAstronautView extends View {
    private Paint textPaint;
    private String astronaut = "🧑‍🚀";
    private float offsetY = 0f;
    private ValueAnimator animator;

    public FloatingAstronautView(Context context) {
        super(context);
        init();
    }

    public FloatingAstronautView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint = new Paint();
        textPaint.setTextSize(200f);  // 70dp 정도
        textPaint.setAntiAlias(true);

        // 둥둥 떠다니는 애니메이션
        animator = ValueAnimator.ofFloat(0f, -30f, 0f);
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            offsetY = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // 우주비행사 이모지 그리기 (중앙)
        float x = width / 2f - textPaint.getTextSize() / 2f;
        float y = height / 2f + textPaint.getTextSize() / 3f + offsetY;

        canvas.drawText(astronaut, x, y, textPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }
}