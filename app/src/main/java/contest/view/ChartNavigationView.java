package contest.view;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.utils.EmptyAnimatorListener;
import contest.utils.GeneralUtils;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartNavigationView extends View implements RangeListener {

    private static final int DRAG_NONE = 0;
    private static final int DRAG_LEFT = 1;
    private static final int DRAG_RIGHT = 2;
    private static final int DRAG_WINDOW = 3;

    private ChartDataSource chartDataSource;

    private float chartLineWidth = 10;
    private float windowVerticalFrameSize = 12;
    private float windowHorizontalFrameSize = 4;
    private int windowSizeMinRows = 10;
    private float fingerSize = 100f;

    private float topBound;
    private float bottomBound;
    private boolean topBoundFixed;
    private boolean bottomBoundFixed;

    private float topGridOffset;
    private float bottomGridOffset;
    private float leftGridOffset;
    private float rightGridOffset;

    private float gridWidth;
    private float gridHeight;
    private float gridStepX;
    private float gridStepY;

    private Bitmap chartLines;

    // animation helpers
    private Paint chartLinesPaint;
    private Rect chartLinesSrcRect = new Rect();
    private Rect chartLinesDstRect = new Rect();
    private Bitmap oldChartLines;
    private Rect oldChartLinesSrcRect = new Rect();
    private Rect oldChartLinesDstRect = new Rect();
    private Paint oldChartLinesPaint;

    private float windowLeftRow;
    private float windowRightRow;

    private Paint backgroundPaint;
    private Paint windowPaint;

    private int dragMode = DRAG_NONE;
    private float dragOffset;
    private float touchBeginX;
    private float touchBeginY;
    private boolean mightCancelDrag;

    private ValueAnimator activeAnimator;

    private RangeListener listener;
    private ChartDataSource.Listener chartDataSourceListener = new ChartDataSource.Listener() {
        @Override
        public void onSetColumnVisibility(int column, boolean visible) {
            if (activeAnimator != null) {
                activeAnimator.cancel();
            }
            activeAnimator = new ValueAnimator();
            activeAnimator.setDuration(300);
            float oldTopBound = topBound;
            float oldBottomBound = bottomBound;
            oldChartLines = chartLines;
            update();

            // we need to calculate new lines coordinates in the old top/bottom system
            float newTopBound = topBound;
            float newBottomBound = bottomBound;
            topBound = oldTopBound;
            bottomBound = oldBottomBound;
            calculateGridBounds();
            PropertyValuesHolder chartLinesTop = PropertyValuesHolder.ofFloat("chartLinesTop", gridToScreenY(newTopBound), topGridOffset);
            PropertyValuesHolder chartLinesBottom = PropertyValuesHolder.ofFloat("chartLinesBottom", gridToScreenY(newBottomBound), topGridOffset + gridHeight);
            topBound = newTopBound;
            bottomBound = newBottomBound;
            calculateGridBounds();

            PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofInt("alpha", 0, 255);
            PropertyValuesHolder oldChartLinesTop = PropertyValuesHolder.ofFloat("oldChartLinesTop", topGridOffset, gridToScreenY(oldTopBound));
            PropertyValuesHolder oldChartLinesBottom = PropertyValuesHolder.ofFloat("oldChartLinesBottom", topGridOffset + gridHeight, gridToScreenY(oldBottomBound));
            activeAnimator.setValues(alphaProperty, chartLinesTop, chartLinesBottom, oldChartLinesTop, oldChartLinesBottom);
            activeAnimator.setInterpolator(new DecelerateInterpolator());
            activeAnimator.addListener(new EmptyAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    activeAnimator = null;
                    if (oldChartLines != null) {
                        oldChartLines.recycle();
                        oldChartLines = null;
                    }
                    invalidate();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    onAnimationEnd(animation);
                }
            });
            activeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    float oldChartLinesTopOffset = (float) animator.getAnimatedValue("oldChartLinesTop");
                    float oldChartLinesBottomOffset = (float) animator.getAnimatedValue("oldChartLinesBottom");
                    float chartLinesTopOffset = (float) animator.getAnimatedValue("chartLinesTop");
                    float chartLinesBottomOffset = (float) animator.getAnimatedValue("chartLinesBottom");

                    oldChartLinesSrcRect.left = 0;
                    oldChartLinesSrcRect.top = 0;
                    oldChartLinesSrcRect.right = oldChartLines.getWidth();
                    oldChartLinesSrcRect.bottom = oldChartLines.getHeight();
                    oldChartLinesDstRect.left = (int) leftGridOffset;
                    oldChartLinesDstRect.top = (int) oldChartLinesTopOffset;
                    oldChartLinesDstRect.right = (int) (leftGridOffset + gridWidth);
                    oldChartLinesDstRect.bottom = (int) oldChartLinesBottomOffset;
                    chartLinesSrcRect.left = 0;
                    chartLinesSrcRect.top = 0;
                    chartLinesSrcRect.right = chartLines.getWidth();
                    chartLinesSrcRect.bottom = chartLines.getHeight();
                    chartLinesDstRect.left = (int) leftGridOffset;
                    chartLinesDstRect.top = (int) chartLinesTopOffset;
                    chartLinesDstRect.right = (int) (leftGridOffset + gridWidth);
                    chartLinesDstRect.bottom = (int) chartLinesBottomOffset;

                    int alpha = (int) animator.getAnimatedValue("alpha");
                    chartLinesPaint.setAlpha(alpha);
                    oldChartLinesPaint.setAlpha(255 - alpha);
                    invalidate();
                }
            });
            activeAnimator.start();
        }
    };

    public ChartNavigationView(Context context) {
        super(context);
        init();
    }

    public ChartNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChartNavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChartNavigationView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
        backgroundPaint.setStyle(Paint.Style.FILL);
        windowPaint = new Paint();
        windowPaint.setAntiAlias(true);
        windowPaint.setStyle(Paint.Style.FILL);
        chartLinesPaint = new Paint();
        chartLinesPaint.setAntiAlias(true);
        chartLinesPaint.setAlpha(255);
        oldChartLinesPaint = new Paint();
        oldChartLinesPaint.setAntiAlias(true);
        oldChartLinesPaint.setAlpha(0);
        setChartLineWidth(GeneralUtils.dp2px(getContext(), 2));
        setWindowVerticalFrameSize(GeneralUtils.dp2px(getContext(), 4));
        setWindowHorizontalFrameSize(GeneralUtils.dp2px(getContext(), 2));
        setFingerSize(GeneralUtils.dp2px(getContext(), 32));
        setBackgroundColor(0xa0f5f6f9);
        setWindowColor(0x60a5bed1);
    }

    public void setWindowSizeMinRows(int windowSizeMinRows) {
        this.windowSizeMinRows = windowSizeMinRows;
    }

    public void setWindowHorizontalFrameSize(float windowHorizontalFrameSize) {
        this.windowHorizontalFrameSize = windowHorizontalFrameSize;
        invalidate();
    }

    public void setWindowVerticalFrameSize(float windowVerticalFrameSize) {
        this.windowVerticalFrameSize = windowVerticalFrameSize;
        invalidate();
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {
        backgroundPaint.setColor(backgroundColor);
        invalidate();
    }

    public void setWindowColor(int windowColor) {
        windowPaint.setColor(windowColor);
        invalidate();
    }

    public void setFingerSize(float fingerSize) {
        this.fingerSize = fingerSize;
    }

    public void setTopBound(float bound) {
        this.topBound = bound;
        this.topBoundFixed = !Float.isNaN(bound);
    }

    public void setBottomBound(float bound) {
        this.bottomBound = bound;
        this.bottomBoundFixed = !Float.isNaN(bound);
    }

    private void calculateVerticalBounds() {
        boolean first = true;
        for (int column = 0; column < chartDataSource.getColumnsCount(); ++column) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            if (columnDataSource.getType().equals(ColumnType.X)) {
                continue;
            }
            if (!chartDataSource.isColumnVisible(column)) {
                continue;
            }
            long valuesFast[] = columnDataSource.getValues();
            for (int row = 0; row < columnDataSource.getRowsCount(); ++row) {
                long value = valuesFast[row];
                if (!bottomBoundFixed && (first || bottomBound > value)) {
                    bottomBound = value;
                }
                if (!topBoundFixed && (first || topBound < value)) {
                    topBound = value;
                }
                first = false;
            }
        }
    }

    public void setDragMode(int dragMode) {
        if (this.dragMode == dragMode) {
            return;
        }
        this.dragMode = dragMode;
        if (listener != null) {
            if (dragMode != DRAG_NONE) {
                listener.onStartDragging();
            } else {
                listener.onStopDragging();
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float windowLeft = gridToScreenX(windowLeftRow);
        float windowRight = gridToScreenX(windowRightRow);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchBeginX = event.getX();
                touchBeginY = event.getY();
                mightCancelDrag = true;
                if (event.getX() > windowLeft - fingerSize / 2 && event.getX() < windowLeft + fingerSize / 2) {
                    setDragMode(DRAG_LEFT);
                    dragOffset = event.getX() - windowLeft;
                } else if (event.getX() > windowRight - fingerSize / 2 && event.getX() < windowRight + fingerSize / 2) {
                    setDragMode(DRAG_RIGHT);
                    dragOffset = event.getX() - windowRight;
                } else if (event.getX() > windowLeft + fingerSize / 2 && event.getX() < windowRight + fingerSize / 2) {
                    setDragMode(DRAG_WINDOW);
                    dragOffset = event.getX() - windowLeft;
                } else {
                    setDragMode(DRAG_NONE);
                    return false;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                switch (dragMode) {
                    case DRAG_LEFT:
                        windowLeftRow = Math.max(0, Math.min(windowRightRow - windowSizeMinRows, screenToGridX(event.getX() - dragOffset)));
                        break;
                    case DRAG_RIGHT:
                        windowRightRow = Math.min(chartDataSource.getRowsCount() - 1, Math.max(windowLeftRow + windowSizeMinRows, screenToGridX(event.getX() - dragOffset)));
                        break;
                    case DRAG_WINDOW:
                        float windowSizeRows = windowRightRow - windowLeftRow;
                        windowLeftRow = Math.max(0, Math.min(chartDataSource.getRowsCount() - 1 - windowSizeRows, screenToGridX(event.getX() - dragOffset)));
                        windowRightRow = Math.min(chartDataSource.getRowsCount() - 1, Math.max(0 + windowSizeRows, windowLeftRow + windowSizeRows));
                        break;
                    case DRAG_NONE:
                    default:
                        return false;
                }
                if (mightCancelDrag && Math.abs(touchBeginY - event.getY()) > fingerSize / 2) {
                    if (Math.abs(touchBeginX - event.getX()) < fingerSize / 2) {
                        dragMode = DRAG_NONE;
                        getParent().requestDisallowInterceptTouchEvent(false);
                    } else {
                        mightCancelDrag = false; // now finger is too far away, cancel is not possible anymore
                    }
                }
                invalidate();
                if (listener != null) {
                    listener.onRangeSelected(windowLeftRow, windowRightRow);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setDragMode(DRAG_NONE);
                return true;

        }
        return super.onTouchEvent(event);
    }

    public void setChartDataSource(ChartDataSource chartDataSource) {
        this.chartDataSource = chartDataSource;
        windowRightRow = chartDataSource.getRowsCount() - 1;
        windowLeftRow = Math.max(0, windowRightRow - 5 * windowSizeMinRows);
        chartDataSource.addListener(chartDataSourceListener);
        if (listener != null) {
            listener.onRangeSelected(windowLeftRow, windowRightRow);
        }
        update();
    }

    private float gridToScreenX(float gridX) {
        return leftGridOffset + gridX * gridStepX;
    }

    private float gridToScreenY(float gridY) {
        return topGridOffset + gridHeight - (gridY - bottomBound) * gridStepY;
    }

    private float screenToGridX(float screenX) {
        float x = screenX - leftGridOffset;
        float valuesCount = chartDataSource.getRowsCount();
        if (valuesCount == 0) {
            return valuesCount / 2;
        }
        return valuesCount * x / gridWidth;
    }

    private float screenToGridY(float screenY) {
        float localY = bottomGridOffset - screenY;
        float valuesCount = (topBound - bottomBound);
        if (valuesCount == 0) {
            return bottomBound + valuesCount / 2;
        }
        return bottomBound + valuesCount * localY / gridHeight;
    }

    private void calculateGridBounds() {
        leftGridOffset = getPaddingLeft() + chartLineWidth / 2;
        rightGridOffset = getPaddingRight() + chartLineWidth / 2;
        topGridOffset = getPaddingTop() + 2 * windowHorizontalFrameSize;
        bottomGridOffset = getPaddingBottom() + 2 * windowHorizontalFrameSize;
        gridWidth = getMeasuredWidth() - leftGridOffset - rightGridOffset;
        gridHeight = getMeasuredHeight() - topGridOffset - bottomGridOffset;
        gridStepX = (chartDataSource.getRowsCount()) <= 1 ? 0 : gridWidth / (chartDataSource.getRowsCount());
        gridStepY = (topBound - bottomBound) <= 1 ? 0 : gridHeight / (topBound - bottomBound);
    }

    public void update() {
        float viewWidth = getMeasuredWidth();
        float viewHeight = getMeasuredHeight();

        if (viewWidth == 0 || viewHeight == 0) {
            return;
        }

        calculateVerticalBounds();
        calculateGridBounds();

        chartLines = Bitmap.createBitmap((int) gridWidth, (int) gridHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(chartLines);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(chartLineWidth);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.STROKE);

        float lines[] = new float[4 * (chartDataSource.getRowsCount() - 1)];
        for (int column = 0; column < chartDataSource.getColumnsCount(); ++column) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            if (columnDataSource.getType().equals(ColumnType.X) || !chartDataSource.isColumnVisible(column)) {
                continue;
            }
            paint.setColor(columnDataSource.getColor());
            long valuesFast[] = columnDataSource.getValues();
            for (int row = 0; row < chartDataSource.getRowsCount() - 1; ++row) {
                lines[4 * row] = gridToScreenX(row) - leftGridOffset;
                lines[4 * row + 1] = gridToScreenY(valuesFast[row]) - topGridOffset;
                lines[4 * row + 2] = gridToScreenX(row + 1) - leftGridOffset;
                lines[4 * row + 3] = gridToScreenY(valuesFast[row + 1]) - topGridOffset;
            }
            canvas.drawLines(lines, paint);
        }

        invalidate();
    }

    public void setChartLineWidth(float chartLineWidth) {
        this.chartLineWidth = chartLineWidth;
        update();
    }

    public void setChartView(RangeListener listener) {
        this.listener = listener;
        if (listener != null) {
            listener.onRangeSelected(windowLeftRow, windowRightRow);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));

        update();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (chartDataSource == null) {
            return;
        }

        if (oldChartLines != null) {
            canvas.save();
            canvas.clipRect(leftGridOffset, topGridOffset, leftGridOffset + gridWidth, topGridOffset + gridHeight);
            canvas.drawBitmap(oldChartLines, oldChartLinesSrcRect, oldChartLinesDstRect, oldChartLinesPaint);
            canvas.drawBitmap(chartLines, chartLinesSrcRect, chartLinesDstRect, chartLinesPaint);
            canvas.restore();
        } else {
            chartLinesSrcRect.left = 0;
            chartLinesSrcRect.top = 0;
            chartLinesSrcRect.right = chartLines.getWidth();
            chartLinesSrcRect.bottom = chartLines.getHeight();
            chartLinesDstRect.left = (int) leftGridOffset;
            chartLinesDstRect.top = (int) topGridOffset;
            chartLinesDstRect.right = chartLinesDstRect.left + chartLines.getWidth();
            chartLinesDstRect.bottom = chartLinesDstRect.top + chartLines.getHeight();
            canvas.drawBitmap(chartLines, chartLinesSrcRect, chartLinesDstRect, chartLinesPaint);
        }

        float windowLeft = gridToScreenX(windowLeftRow);
        float windowRight = gridToScreenX(windowRightRow);
        float top = topGridOffset - windowHorizontalFrameSize;
        float bottom = topGridOffset + gridHeight + windowHorizontalFrameSize;

        canvas.drawRect(leftGridOffset,
                top,
                windowLeft,
                bottom, backgroundPaint);

        canvas.drawRect(windowRight,
                top,
                leftGridOffset + gridWidth,
                bottom, backgroundPaint);

        canvas.drawRect(windowLeft, top, windowLeft + windowVerticalFrameSize, bottom, windowPaint);
        canvas.drawRect(windowRight - windowVerticalFrameSize, top, windowRight, bottom, windowPaint);
        canvas.drawRect(windowLeft + windowVerticalFrameSize, top, windowRight - windowVerticalFrameSize, top + windowHorizontalFrameSize, windowPaint);
        canvas.drawRect(windowLeft + windowVerticalFrameSize, bottom - windowHorizontalFrameSize, windowRight - windowVerticalFrameSize, bottom, windowPaint);

    }

    @Override
    public void onRangeSelected(float startRow, float endRow) {
        windowLeftRow = startRow;
        windowRightRow = endRow;
        invalidate();
    }

    @Override
    public void onStartDragging() {
    }

    @Override
    public void onStopDragging() {
    }

}
