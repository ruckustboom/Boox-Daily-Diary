package com.onyx.dailydiary.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;

import com.onyx.android.sdk.rx.RxRequest;
import com.onyx.android.sdk.utils.RectUtils;

public class PartialRefreshRequest extends RxRequest {
    private final RectF refreshRect;
    private final BitmapView surfaceView;
    private Bitmap bitmap;
    private static final String TAG = PartialRefreshRequest.class.getSimpleName();

    public PartialRefreshRequest(Context context, BitmapView surfaceView, RectF refreshRect) {
        setContext(context);

        this.surfaceView = surfaceView;
        this.refreshRect = refreshRect;
    }

    public PartialRefreshRequest setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        return this;
    }

    @Override
    public void execute() {
        renderToScreen(surfaceView, bitmap);
    }

    public void renderToScreen(BitmapView surfaceView, Bitmap bitmap) {
        if (surfaceView == null || !surfaceView.getHolder().getSurface().isValid()) {
            return;
        }


        Rect renderRect = RectUtils.toRect(refreshRect);
//        EpdController.setViewDefaultUpdateMode(surfaceView, UpdateMode.REGAL);
        Canvas canvas = surfaceView.getHolder().lockCanvas(renderRect);
        if (canvas == null) {
            return;
        }
        try {
            canvas.clipRect(renderRect);
            drawRendererContent(bitmap, canvas);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
//            EpdController.resetViewUpdateMode(surfaceView);
        }
    }

    private void drawRendererContent(Bitmap bitmap, Canvas canvas) {
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        canvas.drawBitmap(bitmap, rect, rect, null);
    }
}