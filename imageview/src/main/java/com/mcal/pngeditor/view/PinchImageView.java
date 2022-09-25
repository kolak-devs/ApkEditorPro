package com.mcal.pngeditor.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PinchImageView extends AppCompatImageView {
    public static final int SCALE_ANIMATOR_DURATION = 200;
    public static final float FLING_DAMPING_FACTOR = 0.9f;
    public static final int PINCH_MODE_FREE = 0;
    public static final int PINCH_MODE_SCROLL = 1;
    public static final int PINCH_MODE_SCALE = 2;
    private static final float MAX_SCALE = 4f;
    private final Matrix mOuterMatrix = new Matrix();
    private final PointF mLastMovePoint = new PointF();
    private final PointF mScaleCenter = new PointF();
    private OnClickListener mOnClickListener;
    private OnLongClickListener mOnLongClickListener;
    private RectF mMask;
    private int mPinchMode = PINCH_MODE_FREE;
    private List<OuterMatrixChangedListener> mOuterMatrixChangedListeners;
    private List<OuterMatrixChangedListener> mOuterMatrixChangedListenersCopy;
    private int mDispatchOuterMatrixChangedLock;
    private MaskAnimator mMaskAnimator;
    private float mScaleBase = 0;
    private ScaleAnimator mScaleAnimator;
    private FlingAnimator mFlingAnimator;
    private final GestureDetector mGestureDetector = new GestureDetector(PinchImageView.this.getContext(), new GestureDetector.SimpleOnGestureListener() {

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (mPinchMode == PINCH_MODE_FREE && !(mScaleAnimator != null && mScaleAnimator.isRunning())) {
                fling(velocityX, velocityY);
            }
            return true;
        }

        public void onLongPress(MotionEvent e) {
            if (mOnLongClickListener != null) {
                mOnLongClickListener.onLongClick(PinchImageView.this);
            }
        }

        public boolean onDoubleTap(MotionEvent e) {
            if (mPinchMode == PINCH_MODE_SCROLL && !(mScaleAnimator != null && mScaleAnimator.isRunning())) {
                doubleTap(e.getX(), e.getY());
            }
            return true;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(PinchImageView.this);
            }
            return true;
        }
    });

    public PinchImageView(Context context) {
        super(context);
        initView();
    }

    public PinchImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PinchImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mOnClickListener = l;
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mOnLongClickListener = l;
    }

    public Matrix getOuterMatrix(Matrix matrix) {
        if (matrix == null) {
            matrix = new Matrix(mOuterMatrix);
        } else {
            matrix.set(mOuterMatrix);
        }
        return matrix;
    }

    public Matrix getInnerMatrix(Matrix matrix) {
        if (matrix == null) {
            matrix = new Matrix();
        } else {
            matrix.reset();
        }
        if (isReady()) {
            RectF tempSrc = MathUtils.rectFTake(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            RectF tempDst = MathUtils.rectFTake(0, 0, getWidth(), getHeight());
            matrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);
            MathUtils.rectFGiven(tempDst);
            MathUtils.rectFGiven(tempSrc);
        }
        return matrix;
    }

    public Matrix getCurrentImageMatrix(Matrix matrix) {
        matrix = getInnerMatrix(matrix);
        matrix.postConcat(mOuterMatrix);
        return matrix;
    }

    public RectF getImageBound(RectF rectF) {
        if (rectF == null) {
            rectF = new RectF();
        } else {
            rectF.setEmpty();
        }
        if (isReady()) {
            Matrix matrix = MathUtils.matrixTake();
            getCurrentImageMatrix(matrix);
            rectF.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            matrix.mapRect(rectF);
            MathUtils.matrixGiven(matrix);
        }
        return rectF;
    }

    public RectF getMask() {
        if (mMask != null) {
            return new RectF(mMask);
        } else {
            return null;
        }
    }

    public int getPinchMode() {
        return mPinchMode;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (mPinchMode == PinchImageView.PINCH_MODE_SCALE) {
            return true;
        }
        RectF bound = getImageBound(null);
        if (bound == null) {
            return false;
        }
        if (bound.isEmpty()) {
            return false;
        }
        if (direction > 0) {
            return bound.right > getWidth();
        } else {
            return bound.left < 0;
        }
    }

    @Override
    public boolean canScrollVertically(int direction) {
        if (mPinchMode == PinchImageView.PINCH_MODE_SCALE) {
            return true;
        }
        RectF bound = getImageBound(null);
        if (bound == null) {
            return false;
        }
        if (bound.isEmpty()) {
            return false;
        }
        if (direction > 0) {
            return bound.bottom > getHeight();
        } else {
            return bound.top < 0;
        }
    }

    public void outerMatrixTo(Matrix endMatrix, long duration) {
        if (endMatrix == null) {
            return;
        }
        mPinchMode = PINCH_MODE_FREE;
        cancelAllAnimator();
        if (duration <= 0) {
            mOuterMatrix.set(endMatrix);
            dispatchOuterMatrixChanged();
            invalidate();
        } else {
            mScaleAnimator = new ScaleAnimator(mOuterMatrix, endMatrix, duration);
            mScaleAnimator.start();
        }
    }

    public void zoomMaskTo(RectF mask, long duration) {
        if (mask == null) {
            return;
        }
        if (mMaskAnimator != null) {
            mMaskAnimator.cancel();
            mMaskAnimator = null;
        }
        if (duration <= 0 || mMask == null) {
            if (mMask == null) {
                mMask = new RectF();
            }
            mMask.set(mask);
            invalidate();
        } else {
            mMaskAnimator = new MaskAnimator(mMask, mask, duration);
            mMaskAnimator.start();
        }
    }

    public void reset() {
        mOuterMatrix.reset();
        dispatchOuterMatrixChanged();
        mMask = null;
        mPinchMode = PINCH_MODE_FREE;
        mLastMovePoint.set(0, 0);
        mScaleCenter.set(0, 0);
        mScaleBase = 0;
        if (mMaskAnimator != null) {
            mMaskAnimator.cancel();
            mMaskAnimator = null;
        }
        cancelAllAnimator();
        invalidate();
    }

    public void addOuterMatrixChangedListener(OuterMatrixChangedListener listener) {
        if (listener == null) {
            return;
        }
        if (mDispatchOuterMatrixChangedLock == 0) {
            if (mOuterMatrixChangedListeners == null) {
                mOuterMatrixChangedListeners = new ArrayList<>();
            }
            mOuterMatrixChangedListeners.add(listener);
        } else {
            if (mOuterMatrixChangedListenersCopy == null) {
                if (mOuterMatrixChangedListeners != null) {
                    mOuterMatrixChangedListenersCopy = new ArrayList<>(mOuterMatrixChangedListeners);
                } else {
                    mOuterMatrixChangedListenersCopy = new ArrayList<>();
                }
            }
            mOuterMatrixChangedListenersCopy.add(listener);
        }
    }

    public void removeOuterMatrixChangedListener(OuterMatrixChangedListener listener) {
        if (listener == null) {
            return;
        }
        if (mDispatchOuterMatrixChangedLock == 0) {
            if (mOuterMatrixChangedListeners != null) {
                mOuterMatrixChangedListeners.remove(listener);
            }
        } else {
            if (mOuterMatrixChangedListenersCopy == null) {
                if (mOuterMatrixChangedListeners != null) {
                    mOuterMatrixChangedListenersCopy = new ArrayList<>(mOuterMatrixChangedListeners);
                }
            }
            if (mOuterMatrixChangedListenersCopy != null) {
                mOuterMatrixChangedListenersCopy.remove(listener);
            }
        }
    }

    private void dispatchOuterMatrixChanged() {
        if (mOuterMatrixChangedListeners == null) {
            return;
        }
        mDispatchOuterMatrixChangedLock++;
        for (OuterMatrixChangedListener listener : mOuterMatrixChangedListeners) {
            listener.onOuterMatrixChanged(this);
        }
        mDispatchOuterMatrixChangedLock--;
        if (mDispatchOuterMatrixChangedLock == 0) {
            if (mOuterMatrixChangedListenersCopy != null) {
                mOuterMatrixChangedListeners = mOuterMatrixChangedListenersCopy;
                mOuterMatrixChangedListenersCopy = null;
            }
        }
    }

    protected float getMaxScale() {
        return MAX_SCALE;
    }

    protected float calculateNextScale(float innerScale, float outerScale) {
        float currentScale = innerScale * outerScale;
        if (currentScale < MAX_SCALE) {
            return MAX_SCALE;
        } else {
            return innerScale;
        }
    }

    private void initView() {
        super.setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isReady()) {
            Matrix matrix = MathUtils.matrixTake();
            setImageMatrix(getCurrentImageMatrix(matrix));
            MathUtils.matrixGiven(matrix);
        }
        if (mMask != null) {
            canvas.save();
            canvas.clipRect(mMask);
            super.onDraw(canvas);
            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }

    private boolean isReady() {
        return getDrawable() != null && getDrawable().getIntrinsicWidth() > 0 && getDrawable().getIntrinsicHeight() > 0
                && getWidth() > 0 && getHeight() > 0;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (mPinchMode == PINCH_MODE_SCALE) {
                scaleEnd();
            }
            mPinchMode = PINCH_MODE_FREE;
        } else if (action == MotionEvent.ACTION_POINTER_UP) {
            if (mPinchMode == PINCH_MODE_SCALE) {
                if (event.getPointerCount() > 2) {
                    if (event.getAction() >> 8 == 0) {
                        saveScaleContext(event.getX(1), event.getY(1), event.getX(2), event.getY(2));
                    } else if (event.getAction() >> 8 == 1) {
                        saveScaleContext(event.getX(0), event.getY(0), event.getX(2), event.getY(2));
                    }
                }
            }
        } else if (action == MotionEvent.ACTION_DOWN) {
            if (!(mScaleAnimator != null && mScaleAnimator.isRunning())) {
                cancelAllAnimator();
                mPinchMode = PINCH_MODE_SCROLL;
                mLastMovePoint.set(event.getX(), event.getY());
            }
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            cancelAllAnimator();
            mPinchMode = PINCH_MODE_SCALE;
            saveScaleContext(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (!(mScaleAnimator != null && mScaleAnimator.isRunning())) {
                if (mPinchMode == PINCH_MODE_SCROLL) {
                    scrollBy(event.getX() - mLastMovePoint.x, event.getY() - mLastMovePoint.y);
                    mLastMovePoint.set(event.getX(), event.getY());
                } else if (mPinchMode == PINCH_MODE_SCALE && event.getPointerCount() > 1) {
                    float distance = MathUtils.getDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                    float[] lineCenter = MathUtils.getCenterPoint(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                    mLastMovePoint.set(lineCenter[0], lineCenter[1]);
                    scale(mScaleCenter, mScaleBase, distance, mLastMovePoint);
                }
            }
        }
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    private boolean scrollBy(float xDiff, float yDiff) {
        if (!isReady()) {
            return false;
        }
        RectF bound = MathUtils.rectFTake();
        getImageBound(bound);
        float displayWidth = getWidth();
        float displayHeight = getHeight();
        if (bound.right - bound.left < displayWidth) {
            xDiff = 0;
        } else if (bound.left + xDiff > 0) {
            if (bound.left < 0) {
                xDiff = -bound.left;
            } else {
                xDiff = 0;
            }
        } else if (bound.right + xDiff < displayWidth) {
            if (bound.right > displayWidth) {
                xDiff = displayWidth - bound.right;
            } else {
                xDiff = 0;
            }
        }
        if (bound.bottom - bound.top < displayHeight) {
            yDiff = 0;
        } else if (bound.top + yDiff > 0) {
            if (bound.top < 0) {
                yDiff = -bound.top;
            } else {
                yDiff = 0;
            }
        } else if (bound.bottom + yDiff < displayHeight) {
            if (bound.bottom > displayHeight) {
                yDiff = displayHeight - bound.bottom;
            } else {
                yDiff = 0;
            }
        }
        MathUtils.rectFGiven(bound);
        mOuterMatrix.postTranslate(xDiff, yDiff);
        dispatchOuterMatrixChanged();
        invalidate();
        return xDiff != 0 || yDiff != 0;
    }

    private void saveScaleContext(float x1, float y1, float x2, float y2) {
        mScaleBase = MathUtils.getMatrixScale(mOuterMatrix)[0] / MathUtils.getDistance(x1, y1, x2, y2);
        float[] center = MathUtils.inverseMatrixPoint(MathUtils.getCenterPoint(x1, y1, x2, y2), mOuterMatrix);
        mScaleCenter.set(center[0], center[1]);
    }

    private void scale(PointF scaleCenter, float scaleBase, float distance, PointF lineCenter) {
        if (!isReady()) {
            return;
        }
        float scale = scaleBase * distance;
        Matrix matrix = MathUtils.matrixTake();
        matrix.postScale(scale, scale, scaleCenter.x, scaleCenter.y);
        matrix.postTranslate(lineCenter.x - scaleCenter.x, lineCenter.y - scaleCenter.y);
        mOuterMatrix.set(matrix);
        MathUtils.matrixGiven(matrix);
        dispatchOuterMatrixChanged();
        invalidate();
    }

    private void doubleTap(float x, float y) {
        if (!isReady()) {
            return;
        }
        Matrix innerMatrix = MathUtils.matrixTake();
        getInnerMatrix(innerMatrix);
        float innerScale = MathUtils.getMatrixScale(innerMatrix)[0];
        float outerScale = MathUtils.getMatrixScale(mOuterMatrix)[0];
        float currentScale = innerScale * outerScale;
        float displayWidth = getWidth();
        float displayHeight = getHeight();
        float maxScale = getMaxScale();
        float nextScale = calculateNextScale(innerScale, outerScale);
        if (nextScale > maxScale) {
            nextScale = maxScale;
        }
        if (nextScale < innerScale) {
            nextScale = innerScale;
        }
        Matrix animEnd = MathUtils.matrixTake(mOuterMatrix);
        animEnd.postScale(nextScale / currentScale, nextScale / currentScale, x, y);
        animEnd.postTranslate(displayWidth / 2f - x, displayHeight / 2f - y);
        Matrix testMatrix = MathUtils.matrixTake(innerMatrix);
        testMatrix.postConcat(animEnd);
        RectF testBound = MathUtils.rectFTake(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        testMatrix.mapRect(testBound);
        float postX = 0;
        float postY = 0;
        if (testBound.right - testBound.left < displayWidth) {
            postX = displayWidth / 2f - (testBound.right + testBound.left) / 2f;
        } else if (testBound.left > 0) {
            postX = -testBound.left;
        } else if (testBound.right < displayWidth) {
            postX = displayWidth - testBound.right;
        }
        if (testBound.bottom - testBound.top < displayHeight) {
            postY = displayHeight / 2f - (testBound.bottom + testBound.top) / 2f;
        } else if (testBound.top > 0) {
            postY = -testBound.top;
        } else if (testBound.bottom < displayHeight) {
            postY = displayHeight - testBound.bottom;
        }
        animEnd.postTranslate(postX, postY);
        cancelAllAnimator();
        mScaleAnimator = new ScaleAnimator(mOuterMatrix, animEnd);
        mScaleAnimator.start();
        MathUtils.rectFGiven(testBound);
        MathUtils.matrixGiven(testMatrix);
        MathUtils.matrixGiven(animEnd);
        MathUtils.matrixGiven(innerMatrix);
    }

    private void scaleEnd() {
        if (!isReady()) {
            return;
        }
        boolean change = false;
        Matrix currentMatrix = MathUtils.matrixTake();
        getCurrentImageMatrix(currentMatrix);
        float currentScale = MathUtils.getMatrixScale(currentMatrix)[0];
        float outerScale = MathUtils.getMatrixScale(mOuterMatrix)[0];
        float displayWidth = getWidth();
        float displayHeight = getHeight();
        float maxScale = getMaxScale();
        float scalePost = 1f;
        float postX = 0;
        float postY = 0;
        if (currentScale > maxScale) {
            scalePost = maxScale / currentScale;
        }
        if (outerScale * scalePost < 1f) {
            scalePost = 1f / outerScale;
        }
        if (scalePost != 1f) {
            change = true;
        }
        Matrix testMatrix = MathUtils.matrixTake(currentMatrix);
        testMatrix.postScale(scalePost, scalePost, mLastMovePoint.x, mLastMovePoint.y);
        RectF testBound = MathUtils.rectFTake(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
        testMatrix.mapRect(testBound);
        if (testBound.right - testBound.left < displayWidth) {
            postX = displayWidth / 2f - (testBound.right + testBound.left) / 2f;
        } else if (testBound.left > 0) {
            postX = -testBound.left;
        } else if (testBound.right < displayWidth) {
            postX = displayWidth - testBound.right;
        }
        if (testBound.bottom - testBound.top < displayHeight) {
            postY = displayHeight / 2f - (testBound.bottom + testBound.top) / 2f;
        } else if (testBound.top > 0) {
            postY = -testBound.top;
        } else if (testBound.bottom < displayHeight) {
            postY = displayHeight - testBound.bottom;
        }
        if (postX != 0 || postY != 0) {
            change = true;
        }
        if (change) {
            Matrix animEnd = MathUtils.matrixTake(mOuterMatrix);
            animEnd.postScale(scalePost, scalePost, mLastMovePoint.x, mLastMovePoint.y);
            animEnd.postTranslate(postX, postY);
            cancelAllAnimator();
            mScaleAnimator = new ScaleAnimator(mOuterMatrix, animEnd);
            mScaleAnimator.start();
            MathUtils.matrixGiven(animEnd);
        }
        MathUtils.rectFGiven(testBound);
        MathUtils.matrixGiven(testMatrix);
        MathUtils.matrixGiven(currentMatrix);
    }

    private void fling(float vx, float vy) {
        if (!isReady()) {
            return;
        }
        cancelAllAnimator();
        mFlingAnimator = new FlingAnimator(vx / 60f, vy / 60f);
        mFlingAnimator.start();
    }

    private void cancelAllAnimator() {
        if (mScaleAnimator != null) {
            mScaleAnimator.cancel();
            mScaleAnimator = null;
        }
        if (mFlingAnimator != null) {
            mFlingAnimator.cancel();
            mFlingAnimator = null;
        }
    }

    public interface OuterMatrixChangedListener {
        void onOuterMatrixChanged(PinchImageView pinchImageView);
    }

    private static abstract class ObjectsPool<T> {

        private final int mSize;

        private final Queue<T> mQueue;

        public ObjectsPool(int size) {
            mSize = size;
            mQueue = new LinkedList<>();
        }

        public T take() {
            if (mQueue.size() == 0) {
                return newInstance();
            } else {
                return resetInstance(mQueue.poll());
            }
        }

        public void given(T obj) {
            if (obj != null && mQueue.size() < mSize) {
                mQueue.offer(obj);
            }
        }

        abstract protected T newInstance();

        abstract protected T resetInstance(T obj);
    }

    private static class MatrixPool extends ObjectsPool<Matrix> {

        public MatrixPool(int size) {
            super(size);
        }

        @NonNull
        @Contract(" -> new")
        @Override
        protected Matrix newInstance() {
            return new Matrix();
        }

        @NonNull
        @Contract("_ -> param1")
        @Override
        protected Matrix resetInstance(@NonNull Matrix obj) {
            obj.reset();
            return obj;
        }
    }

    private static class RectFPool extends ObjectsPool<RectF> {

        public RectFPool(int size) {
            super(size);
        }

        @NonNull
        @Contract(" -> new")
        @Override
        protected RectF newInstance() {
            return new RectF();
        }

        @NonNull
        @Contract("_ -> param1")
        @Override
        protected RectF resetInstance(@NonNull RectF obj) {
            obj.setEmpty();
            return obj;
        }
    }

    public static class MathUtils {

        private static final MatrixPool mMatrixPool = new MatrixPool(16);
        private static final RectFPool mRectFPool = new RectFPool(16);

        public static Matrix matrixTake() {
            return mMatrixPool.take();
        }

        public static Matrix matrixTake(Matrix matrix) {
            Matrix result = mMatrixPool.take();
            if (matrix != null) {
                result.set(matrix);
            }
            return result;
        }

        public static void matrixGiven(Matrix matrix) {
            mMatrixPool.given(matrix);
        }

        public static RectF rectFTake() {
            return mRectFPool.take();
        }

        @NonNull
        public static RectF rectFTake(float left, float top, float right, float bottom) {
            RectF result = mRectFPool.take();
            result.set(left, top, right, bottom);
            return result;
        }

        public static RectF rectFTake(RectF rectF) {
            RectF result = mRectFPool.take();
            if (rectF != null) {
                result.set(rectF);
            }
            return result;
        }

        public static void rectFGiven(RectF rectF) {
            mRectFPool.given(rectF);
        }

        public static float getDistance(float x1, float y1, float x2, float y2) {
            float x = x1 - x2;
            float y = y1 - y2;
            return (float) Math.sqrt(x * x + y * y);
        }

        @NonNull
        @Contract(value = "_, _, _, _ -> new", pure = true)
        public static float[] getCenterPoint(float x1, float y1, float x2, float y2) {
            return new float[]{(x1 + x2) / 2f, (y1 + y2) / 2f};
        }

        @NonNull
        @Contract("null -> new")
        public static float[] getMatrixScale(Matrix matrix) {
            if (matrix != null) {
                float[] value = new float[9];
                matrix.getValues(value);
                return new float[]{value[0], value[4]};
            } else {
                return new float[2];
            }
        }

        @NonNull
        @Contract("null, _ -> new; !null, null -> new")
        public static float[] inverseMatrixPoint(float[] point, Matrix matrix) {
            if (point != null && matrix != null) {
                float[] dst = new float[2];
                Matrix inverse = matrixTake();
                matrix.invert(inverse);
                inverse.mapPoints(dst, point);
                matrixGiven(inverse);
                return dst;
            } else {
                return new float[2];
            }
        }

        public static void calculateRectTranslateMatrix(RectF from, RectF to, Matrix result) {
            if (from == null || to == null || result == null) {
                return;
            }
            if (from.width() == 0 || from.height() == 0) {
                return;
            }
            result.reset();
            result.postTranslate(-from.left, -from.top);
            result.postScale(to.width() / from.width(), to.height() / from.height());
            result.postTranslate(to.left, to.top);
        }

        public static void calculateScaledRectInContainer(RectF container, float srcWidth, float srcHeight, ScaleType scaleType, RectF result) {
            if (container == null || result == null) {
                return;
            }
            if (srcWidth == 0 || srcHeight == 0) {
                return;
            }
            if (scaleType == null) {
                scaleType = ScaleType.FIT_CENTER;
            }
            result.setEmpty();
            if (ScaleType.FIT_XY.equals(scaleType)) {
                result.set(container);
            } else if (ScaleType.CENTER.equals(scaleType)) {
                Matrix matrix = matrixTake();
                RectF rect = rectFTake(0, 0, srcWidth, srcHeight);
                matrix.setTranslate((container.width() - srcWidth) * 0.5f, (container.height() - srcHeight) * 0.5f);
                matrix.mapRect(result, rect);
                rectFGiven(rect);
                matrixGiven(matrix);
                result.left += container.left;
                result.right += container.left;
                result.top += container.top;
                result.bottom += container.top;
            } else if (ScaleType.CENTER_CROP.equals(scaleType)) {
                Matrix matrix = matrixTake();
                RectF rect = rectFTake(0, 0, srcWidth, srcHeight);
                float scale;
                float dx = 0;
                float dy = 0;
                if (srcWidth * container.height() > container.width() * srcHeight) {
                    scale = container.height() / srcHeight;
                    dx = (container.width() - srcWidth * scale) * 0.5f;
                } else {
                    scale = container.width() / srcWidth;
                    dy = (container.height() - srcHeight * scale) * 0.5f;
                }
                matrix.setScale(scale, scale);
                matrix.postTranslate(dx, dy);
                matrix.mapRect(result, rect);
                rectFGiven(rect);
                matrixGiven(matrix);
                result.left += container.left;
                result.right += container.left;
                result.top += container.top;
                result.bottom += container.top;
            } else if (ScaleType.CENTER_INSIDE.equals(scaleType)) {
                Matrix matrix = matrixTake();
                RectF rect = rectFTake(0, 0, srcWidth, srcHeight);
                float scale;
                float dx;
                float dy;
                if (srcWidth <= container.width() && srcHeight <= container.height()) {
                    scale = 1f;
                } else {
                    scale = Math.min(container.width() / srcWidth, container.height() / srcHeight);
                }
                dx = (container.width() - srcWidth * scale) * 0.5f;
                dy = (container.height() - srcHeight * scale) * 0.5f;
                matrix.setScale(scale, scale);
                matrix.postTranslate(dx, dy);
                matrix.mapRect(result, rect);
                rectFGiven(rect);
                matrixGiven(matrix);
                result.left += container.left;
                result.right += container.left;
                result.top += container.top;
                result.bottom += container.top;
            } else if (ScaleType.FIT_CENTER.equals(scaleType)) {
                Matrix matrix = matrixTake();
                RectF rect = rectFTake(0, 0, srcWidth, srcHeight);
                RectF tempSrc = rectFTake(0, 0, srcWidth, srcHeight);
                RectF tempDst = rectFTake(0, 0, container.width(), container.height());
                matrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.CENTER);
                matrix.mapRect(result, rect);
                rectFGiven(tempDst);
                rectFGiven(tempSrc);
                rectFGiven(rect);
                matrixGiven(matrix);
                result.left += container.left;
                result.right += container.left;
                result.top += container.top;
                result.bottom += container.top;
            } else if (ScaleType.FIT_START.equals(scaleType)) {
                Matrix matrix = matrixTake();
                RectF rect = rectFTake(0, 0, srcWidth, srcHeight);
                RectF tempSrc = rectFTake(0, 0, srcWidth, srcHeight);
                RectF tempDst = rectFTake(0, 0, container.width(), container.height());
                matrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.START);
                matrix.mapRect(result, rect);
                rectFGiven(tempDst);
                rectFGiven(tempSrc);
                rectFGiven(rect);
                matrixGiven(matrix);
                result.left += container.left;
                result.right += container.left;
                result.top += container.top;
                result.bottom += container.top;
            } else if (ScaleType.FIT_END.equals(scaleType)) {
                Matrix matrix = matrixTake();
                RectF rect = rectFTake(0, 0, srcWidth, srcHeight);
                RectF tempSrc = rectFTake(0, 0, srcWidth, srcHeight);
                RectF tempDst = rectFTake(0, 0, container.width(), container.height());
                matrix.setRectToRect(tempSrc, tempDst, Matrix.ScaleToFit.END);
                matrix.mapRect(result, rect);
                rectFGiven(tempDst);
                rectFGiven(tempSrc);
                rectFGiven(rect);
                matrixGiven(matrix);
                result.left += container.left;
                result.right += container.left;
                result.top += container.top;
                result.bottom += container.top;
            } else {
                result.set(container);
            }
        }
    }

    private class MaskAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener {

        private final float[] mStart = new float[4];
        private final float[] mEnd = new float[4];
        private final float[] mResult = new float[4];

        public MaskAnimator(@NonNull RectF start, @NonNull RectF end, long duration) {
            super();
            setFloatValues(0, 1f);
            setDuration(duration);
            addUpdateListener(this);
            mStart[0] = start.left;
            mStart[1] = start.top;
            mStart[2] = start.right;
            mStart[3] = start.bottom;
            mEnd[0] = end.left;
            mEnd[1] = end.top;
            mEnd[2] = end.right;
            mEnd[3] = end.bottom;
        }

        @Override
        public void onAnimationUpdate(@NonNull ValueAnimator animation) {
            float value = (Float) animation.getAnimatedValue();
            for (int i = 0; i < 4; i++) {
                mResult[i] = mStart[i] + (mEnd[i] - mStart[i]) * value;
            }
            if (mMask == null) {
                mMask = new RectF();
            }
            mMask.set(mResult[0], mResult[1], mResult[2], mResult[3]);
            invalidate();
        }
    }

    private class FlingAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener {

        private final float[] mVector;

        public FlingAnimator(float vectorX, float vectorY) {
            super();
            setFloatValues(0, 1f);
            setDuration(1000000);
            addUpdateListener(this);
            mVector = new float[]{vectorX, vectorY};
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            boolean result = scrollBy(mVector[0], mVector[1]);
            mVector[0] *= FLING_DAMPING_FACTOR;
            mVector[1] *= FLING_DAMPING_FACTOR;
            if (!result || MathUtils.getDistance(0, 0, mVector[0], mVector[1]) < 1f) {
                animation.cancel();
            }
        }
    }

    private class ScaleAnimator extends ValueAnimator implements ValueAnimator.AnimatorUpdateListener {

        private final float[] mStart = new float[9];
        private final float[] mEnd = new float[9];
        private final float[] mResult = new float[9];

        public ScaleAnimator(Matrix start, Matrix end) {
            this(start, end, SCALE_ANIMATOR_DURATION);
        }

        public ScaleAnimator(@NonNull Matrix start, @NonNull Matrix end, long duration) {
            super();
            setFloatValues(0, 1f);
            setDuration(duration);
            addUpdateListener(this);
            start.getValues(mStart);
            end.getValues(mEnd);
        }

        @Override
        public void onAnimationUpdate(@NonNull ValueAnimator animation) {
            float value = (Float) animation.getAnimatedValue();
            for (int i = 0; i < 9; i++) {
                mResult[i] = mStart[i] + (mEnd[i] - mStart[i]) * value;
            }
            mOuterMatrix.setValues(mResult);
            dispatchOuterMatrixChanged();
            invalidate();
        }
    }
}