package com.example.vibebank.ui.register;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class IDCardOverlayView extends View {
    private Paint eraserPaint;
    private Paint borderPaint;
    private RectF frameRect;
    private float radius = 30f;

    public IDCardOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        eraserPaint = new Paint();
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        eraserPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        borderPaint.setAntiAlias(true);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Vẽ màn đen mờ phủ toàn bộ
        canvas.drawColor(Color.parseColor("#80000000")); // Màu đen độ trong suốt 50%

        // 2. Tính toán vị trí khung chữ nhật (tỉ lệ CCCD chuẩn ~ 85.6mm x 54mm => Tỉ lệ 1.58)
        float width = getWidth();
        float height = getHeight();

        float frameWidth = width * 0.9f; // Chiếm 90% chiều rộng màn hình
        float frameHeight = frameWidth / 1.58f; // Chiều cao theo tỉ lệ CCCD

        float left = (width - frameWidth) / 2;
        float top = (height - frameHeight) / 2;
        float right = left + frameWidth;
        float bottom = top + frameHeight;

        frameRect = new RectF(left, top, right, bottom);

        // 3. Khoét lỗ trong suốt
        canvas.drawRoundRect(frameRect, radius, radius, eraserPaint);

        // 4. Vẽ viền trắng bo theo
        canvas.drawRoundRect(frameRect, radius, radius, borderPaint);
    }
}