package com.example.client_system;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StarryBackgroundView extends View {
    private Paint starPaint;
    private List<Star> stars;
    private Random random;
    private ValueAnimator animator;

    public StarryBackgroundView(Context context) {
        super(context);
        init();
    }

    public StarryBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        starPaint = new Paint();
        starPaint.setColor(Color.WHITE);
        starPaint.setAntiAlias(true);

        stars = new ArrayList<>();
        random = new Random();

        // 별 100개 생성
        for (int i = 0; i < 100; i++) {
            stars.add(new Star());
        }

        // 애니메이션 시작
        startAnimation();
    }

    private void startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(3000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            // 각 별의 투명도 업데이트
            for (Star star : stars) {
                star.update();
            }
            invalidate(); // 다시 그리기
        });
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 화면 크기가 정해지면 별 위치 재설정
        for (Star star : stars) {
            star.x = random.nextFloat() * w;
            star.y = random.nextFloat() * h;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 모든 별 그리기
        for (Star star : stars) {
            starPaint.setAlpha((int) (star.alpha * 255));
            canvas.drawCircle(star.x, star.y, star.size, starPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }

    // 별 클래스
    private class Star {
        float x, y;
        float size;
        float alpha;
        float alphaSpeed;
        boolean increasing;

        Star() {
            // 랜덤 크기 (1~3 픽셀)
            size = 1f + random.nextFloat() * 2f;

            // 초기 투명도
            alpha = 0.3f + random.nextFloat() * 0.7f;

            // 반짝이는 속도
            alphaSpeed = 0.01f + random.nextFloat() * 0.02f;

            // 밝아지는 중인지 어두워지는 중인지
            increasing = random.nextBoolean();
        }

        void update() {
            // 투명도 변경 (반짝임 효과)
            if (increasing) {
                alpha += alphaSpeed;
                if (alpha >= 1f) {
                    alpha = 1f;
                    increasing = false;
                }
            } else {
                alpha -= alphaSpeed;
                if (alpha <= 0.3f) {
                    alpha = 0.3f;
                    increasing = true;
                }
            }
        }
    }
}