package chenqiren.com.magiclines.drawing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class DrawingView1 extends View {

    private Bitmap mBitmap;
    private Canvas mCanvas;

    private Paint mBitmapPaint;
    private Paint mPaint;
    private Paint mClearPaint;

    private Set<DrawPath> mDrawPaths = new HashSet<>();
    private DrawPath mCurrentDrawPath;
    private Handler mHandler;

    public DrawingView1(Context c) {
        super(c);

        init();
    }

    public DrawingView1(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    public DrawingView1(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    private void init() {
        HandlerThread handlerThread = new HandlerThread("");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFF0000);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);

        mClearPaint = new Paint();
        mClearPaint.setColor(Color.TRANSPARENT);
        mClearPaint.setStrokeWidth(13);
        mClearPaint.setAntiAlias(true);
        mClearPaint.setStyle(Paint.Style.STROKE);
        mClearPaint.setStrokeJoin(Paint.Join.ROUND);
        mClearPaint.setStrokeCap(Paint.Cap.ROUND);
        Xfermode xFermode = new PorterDuffXfermode(Mode.CLEAR);
        mClearPaint.setXfermode(xFermode);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFAAAAAA);

        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    }

    private void touch_start(float x, float y) {
        mCurrentDrawPath = new DrawPath();
        mCurrentDrawPath.moveTo(x, y);
        mCurrentDrawPath.lineTo(x+0.1f, y+0.1f);

        mDrawPaths.add(mCurrentDrawPath);
    }
    private void touch_move(float x, float y) {
        mCurrentDrawPath.lineTo(x, y);
    }
    private void touch_up(float x, float y) {
        mCurrentDrawPath.lineTo(x, y);

        mHandler.post(new ClearPathTask(mCurrentDrawPath));
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

    private class ClearPathTask implements Runnable {

        private DrawPath mDrawPath;
        private Path mClearPath;

        public ClearPathTask(DrawPath drawPath) {
            mDrawPath = drawPath;
            mClearPath = new Path();
        }

        @Override
        public void run() {
            Queue<DrawPoint> pathPoints = new LinkedList<>(mDrawPath.drawPoints);
            if (pathPoints.isEmpty()) {
                return;
            }

            long startTimestamp = System.currentTimeMillis();
            DrawPoint firstPoint = pathPoints.poll();
            long firstTimestamp = firstPoint.timestamp;
            long diff = startTimestamp - firstTimestamp;
            mClearPath.moveTo(firstPoint.point.x, firstPoint.point.y);

            while (!pathPoints.isEmpty()) {
                if (System.currentTimeMillis() - pathPoints.peek().timestamp > diff) {
                    DrawPoint drawPoint = pathPoints.poll();

                    mClearPath.lineTo(drawPoint.point.x, drawPoint.point.y);
                    mCanvas.drawPath(mClearPath, mClearPaint);

                    DrawingView1.this.postInvalidate();
                }
            }
        }
    }

    private class DrawPath {
        public Path path;
        public Queue<DrawPoint> drawPoints = new LinkedList<>();

        public DrawPath() {
            this.path = new Path();
        }

        public void lineTo(float x, float y) {
            path.lineTo(x, y);
            drawPoints.add(new DrawPoint(x, y));

            mCanvas.drawPath(path, mPaint);
            invalidate();
        }

        public void moveTo(float x, float y) {
            path.moveTo(x, y);
            drawPoints.add(new DrawPoint(x, y));

            mCanvas.drawPath(path, mPaint);
            invalidate();
        }
    }

    private static class DrawPoint {
        public PointF point;
        public long timestamp;

        public DrawPoint(float x, float y) {
            point = new PointF(x, y);
            timestamp = System.currentTimeMillis();
        }
    }
}


