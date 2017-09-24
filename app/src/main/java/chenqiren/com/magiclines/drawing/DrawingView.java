package chenqiren.com.magiclines.drawing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.common.collect.ImmutableList;

public class DrawingView extends View {

    private static final int TIME_TO_SHOW = 2000; //2s
    private static final int DRAW_PAINT_DEFAULT_WIDTH = 20;
    private static final int DRAW_PAINT_DEFAULT_COLOR = 0xFFFF0000;
    private static final int BACKGROUND_COLOR = 0xFFAAAAAA;

    private Paint mPaint;
    private DrawPath mCurrentDrawPath;
    private LinkedHashSet<DrawPath> mDrawPaths;
    private Handler mHandler;

    public DrawingView(Context context) {
        super(context);

        init();
    }

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public DrawingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        HandlerThread handlerThread = new HandlerThread("");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mDrawPaths = new LinkedHashSet<>();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DRAW_PAINT_DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(DRAW_PAINT_DEFAULT_WIDTH);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(BACKGROUND_COLOR);

        drawPath(canvas);
    }

    private void drawPath(Canvas canvas) {
        ImmutableList<DrawPath> drawPaths = ImmutableList.copyOf(mDrawPaths);
        for (DrawPath drawPath : drawPaths) {
            for (DrawSegment segment : drawPath.drawSegment) {
                canvas.drawPath(segment.path, mPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up(x, y);
                invalidate();
                break;
        }

        return true;
    }

    public void setColor(int color) {
        mPaint.setColor(color);
    }

    public void setStrokeWidth(int width) {
        mPaint.setStrokeWidth(width);
    }

    private void touch_start(float x, float y) {
        mCurrentDrawPath = new DrawPath(x, y);
        mDrawPaths.add(mCurrentDrawPath);
    }
    private void touch_move(float x, float y) {
        mCurrentDrawPath.lineTo(x, y);
    }

    private void touch_up(float x, float y) {
        mCurrentDrawPath.lineTo(x, y);

        mHandler.postDelayed(new ClearPathTask(mCurrentDrawPath), TIME_TO_SHOW);
    }

    private class ClearPathTask implements Runnable {

        private DrawPath mDrawPath;

        public ClearPathTask(DrawPath drawPath) {
            mDrawPath = drawPath;
        }

        @Override
        public void run() {
            Queue<DrawSegment> segments = mDrawPath.drawSegment;
            if (segments.isEmpty()) {
                return;
            }

            long startTimestamp = System.currentTimeMillis();
            DrawSegment firstSegment = segments.poll();
            long firstTimestamp = firstSegment.timestamp;
            long diff = startTimestamp - firstTimestamp;

            while (!segments.isEmpty()) {
                if (System.currentTimeMillis() - segments.peek().timestamp > diff) {
                    segments.poll();

                    DrawingView.this.postInvalidate();
                }
            }

            mDrawPaths.remove(mDrawPath);
        }
    }

    private class DrawPath {
        public Queue<DrawSegment> drawSegment = new ConcurrentLinkedQueue<>();

        public float oldx;
        public float oldy;

        public DrawPath(float x, float y) {
            oldx = x;
            oldy = y;
        }

        public void lineTo(float x, float y) {
            DrawSegment segment = new DrawSegment(oldx, oldy, x, y);

            oldx = x;
            oldy = y;

            drawSegment.add(segment);

            invalidate();
        }
    }

    private static class DrawSegment {
        private static final float SPLINE_TOLERANCE = 8;

        public Path path;
        public long timestamp;

        public DrawSegment(float x, float y, float oldx, float oldy) {
            path = new Path();
            path.moveTo(oldx, oldy);

            float dx = Math.abs(x - oldx);
            float dy = Math.abs(y - oldy);
            if (dx >= SPLINE_TOLERANCE || dy >= SPLINE_TOLERANCE) {
                path.quadTo((x + oldx) / 2, (y + oldy) / 2, x, y);
            } else {
                path.lineTo(x, y);
            }

            timestamp = System.currentTimeMillis();
        }
    }
}

