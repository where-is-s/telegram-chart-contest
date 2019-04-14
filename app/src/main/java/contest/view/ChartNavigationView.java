package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

import contest.datasource.ChartDataSource;
import contest.datasource.ChartType;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.utils.ChartUtils;
import contest.utils.GeneralUtils;
import contest.utils.SimpleAnimator;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartNavigationView extends View implements RangeListener {

    private static final int DRAG_NONE = 0;
    private static final int DRAG_LEFT = 1;
    private static final int DRAG_RIGHT = 2;
    private static final int DRAG_WINDOW = 3;

    private static final int ANIMATE_CHART_BITMAP_TOP = 1;
    private static final int ANIMATE_CHART_BITMAP_BOTTOM = 2;
    private static final int ANIMATE_ALPHA = 3;
    private static final int ANIMATE_OLD_CHART_BITMAP_TOP = 4;
    private static final int ANIMATE_OLD_CHART_BITMAP_BOTTOM = 5;
    private static final int ANIMATE_WINDOW_LEFT = 6;
    private static final int ANIMATE_WINDOW_RIGHT = 7;

    private ChartDataSource chartDataSource;

    private long snapToXDistance = -1;
    private boolean allowResizeWindow = true;

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

    private float rightYAxisMultiplier;

    private float gridWidth;
    private float gridHeight;
    private float gridStepX;
    private float gridStepY;

    private Bitmap currentChartBitmap;
    private Bitmap cornerCoverBitmap;
    private Bitmap leftSelectorEdgeBitmap;
    private Bitmap rightSelectorEdgeBitmap;

    // animation helpers
    private Paint chartBitmapPaint;
    private Rect chartBitmapSrcRect = new Rect();
    private Rect chartBitmapDstRect = new Rect();
    private Bitmap oldChartBitmap;
    private Rect oldChartBitmapSrcRect = new Rect();
    private Rect oldChartBitmapDstRect = new Rect();
    private Paint oldChartBitmapPaint;

    private float windowLeftRow;
    private float windowRightRow;

    private Paint backgroundPaint;
    private Paint windowPaint;

    private int dragMode = DRAG_NONE;
    private float dragOffset;
    private float touchBeginX;
    private float touchBeginY;
    private boolean mightCancelDrag;

    private SimpleAnimator chartsAnimator;
    private SimpleAnimator windowAnimator;

    private RangeListener listener;
    private ChartDataSource.Listener chartDataSourceListener = new ChartDataSource.Listener() {

        @Override
        public void onSetColumnVisibility(int column, boolean visible) {
            if (getVisibility() == GONE) {
                return;
            }
            if (chartsAnimator != null) {
                chartsAnimator.cancel();
            }
            chartsAnimator = new SimpleAnimator();
            chartsAnimator.setDuration(300);
            float oldTopBound = topBound;
            float oldBottomBound = bottomBound;
            oldChartBitmap = currentChartBitmap;
            update();

            // we need to calculate new lines coordinates in the old top/bottom system
            float newTopBound = topBound;
            float newBottomBound = bottomBound;
            topBound = oldTopBound;
            bottomBound = oldBottomBound;
            calculateGridBounds();
            chartsAnimator.addValue(ANIMATE_CHART_BITMAP_TOP, gridToScreenY(false, newTopBound), topGridOffset);
            chartsAnimator.addValue(ANIMATE_CHART_BITMAP_BOTTOM, gridToScreenY(false, newBottomBound), topGridOffset + gridHeight);
            topBound = newTopBound;
            bottomBound = newBottomBound;
            calculateGridBounds();

            chartsAnimator.addValue(ANIMATE_ALPHA, 0, 255);
            chartsAnimator.addValue(ANIMATE_OLD_CHART_BITMAP_TOP, topGridOffset, gridToScreenY(false, oldTopBound));
            chartsAnimator.addValue(ANIMATE_OLD_CHART_BITMAP_BOTTOM, topGridOffset + gridHeight, gridToScreenY(false, oldBottomBound));
            chartsAnimator.setInterpolator(new DecelerateInterpolator());
            chartsAnimator.setListener(new SimpleAnimator.Listener() {

                @Override
                public void onEnd() {
                    chartsAnimator = null;
                    if (oldChartBitmap != null) {
                        oldChartBitmap.recycle();
                        oldChartBitmap = null;
                    }
                    invalidate();
                }

                @Override
                public void onCancel() {
                    onEnd();
                }

                @Override
                public void onUpdate() {
                    float oldChartLinesTopOffset = chartsAnimator.getFloatValue(ANIMATE_OLD_CHART_BITMAP_TOP);
                    float oldChartLinesBottomOffset = chartsAnimator.getFloatValue(ANIMATE_OLD_CHART_BITMAP_BOTTOM);
                    float chartLinesTopOffset = chartsAnimator.getFloatValue(ANIMATE_CHART_BITMAP_TOP);
                    float chartLinesBottomOffset = chartsAnimator.getFloatValue(ANIMATE_CHART_BITMAP_BOTTOM);

                    oldChartBitmapSrcRect.left = 0;
                    oldChartBitmapSrcRect.top = 0;
                    oldChartBitmapSrcRect.right = oldChartBitmap.getWidth();
                    oldChartBitmapSrcRect.bottom = oldChartBitmap.getHeight();
                    oldChartBitmapDstRect.left = (int) leftGridOffset;
                    oldChartBitmapDstRect.top = (int) oldChartLinesTopOffset;
                    oldChartBitmapDstRect.right = (int) (leftGridOffset + gridWidth);
                    oldChartBitmapDstRect.bottom = (int) oldChartLinesBottomOffset;
                    chartBitmapSrcRect.left = 0;
                    chartBitmapSrcRect.top = 0;
                    chartBitmapSrcRect.right = currentChartBitmap.getWidth();
                    chartBitmapSrcRect.bottom = currentChartBitmap.getHeight();
                    chartBitmapDstRect.left = (int) leftGridOffset;
                    chartBitmapDstRect.top = (int) chartLinesTopOffset;
                    chartBitmapDstRect.right = (int) (leftGridOffset + gridWidth);
                    chartBitmapDstRect.bottom = (int) chartLinesBottomOffset;

                    int alpha = chartsAnimator.getIntValue(ANIMATE_ALPHA);
                    chartBitmapPaint.setAlpha(alpha);
                    if (isChartType(ChartType.PERCENTAGE) || isChartType(ChartType.PIE)) {
                        oldChartBitmapPaint.setAlpha(255);
                    } else {
                        oldChartBitmapPaint.setAlpha(255 - alpha);
                    }
                    invalidate();
                }
            });
            chartsAnimator.start();
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
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        windowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        windowPaint.setStyle(Paint.Style.FILL);
        chartBitmapPaint = new Paint();
        chartBitmapPaint.setAntiAlias(true);
        chartBitmapPaint.setAlpha(255);
        oldChartBitmapPaint = new Paint();
        oldChartBitmapPaint.setAntiAlias(true);
        oldChartBitmapPaint.setAlpha(0);
        setChartLineWidth(GeneralUtils.dp2px(getContext(), 2));
        setWindowVerticalFrameSize(GeneralUtils.dp2px(getContext(), 10));
        setWindowHorizontalFrameSize(GeneralUtils.dp2px(getContext(), 1.5f));
        setFingerSize(GeneralUtils.dp2px(getContext(), 32));
        setBackgroundColor(0x99E2EEF9);
        setWindowColor(0x8086A9C4);
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
        this.topBoundFixed = !Float.isNaN(bound);
        if (this.topBoundFixed) {
            this.topBound = bound;
        }
    }

    public void setBottomBound(float bound) {
        this.bottomBoundFixed = !Float.isNaN(bound);
        if (this.bottomBoundFixed) {
            this.bottomBound = bound;
        }
    }

    private void calculateVerticalBounds() {
        if (isChartType(ChartType.PERCENTAGE) || isChartType(ChartType.PIE)) {
            bottomBound = 0f;
            topBound = 100f;
            return;
        }
        ChartUtils.VertBounds vertBounds = ChartUtils.calculateVertBounds(
                chartDataSource, 0, chartDataSource.getRowsCount() - 1, isChartType(ChartType.BAR_STACK));
        if (!topBoundFixed) {
            topBound = vertBounds.calculatedTopBound;
        }
        if (!bottomBoundFixed) {
            bottomBound = vertBounds.calculatedBottomBound;
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

    private float snapToNearestRow(float row) {
        ColumnDataSource xColumn = chartDataSource.getColumn(chartDataSource.getXAxisValueSourceColumn());
        if (row > xColumn.getRowsCount() - 1) {
            return xColumn.getRowsCount() - 1;
        }
        if (row < 0) {
            return 0;
        }
        long value = xColumn.getValue((int) row);
        long leftValue = (value / snapToXDistance) * snapToXDistance;
        long rightValue = (value / snapToXDistance + 1) * snapToXDistance;
        if (value < (leftValue + rightValue) / 2) {
            for (int r = (int) row; r >= 0; --r) {
                if (xColumn.getValue(r) < leftValue) {
                    return r + 1;
                }
            }
            return 0;
        }
        for (int r = (int) row; r < xColumn.getRowsCount(); ++r) {
            if (xColumn.getValue(r) > rightValue) {
                return r - 1;
            }
        }
        return xColumn.getRowsCount();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float windowLeft = getWindowLeft();
        float windowRight = getWindowRight();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchBeginX = event.getX();
                touchBeginY = event.getY();
                mightCancelDrag = true;
                if (event.getX() > windowLeft + windowVerticalFrameSize && event.getX() < windowRight - windowVerticalFrameSize
                        || (!allowResizeWindow && event.getX() > windowLeft - fingerSize / 2 && event.getX() < windowRight + fingerSize / 2)) {
                    setDragMode(DRAG_WINDOW);
                    dragOffset = event.getX() - gridToScreenX(windowLeftRow);
                } else if (allowResizeWindow && event.getX() > windowLeft + windowVerticalFrameSize - fingerSize && event.getX() < windowLeft + windowVerticalFrameSize) {
                    setDragMode(DRAG_LEFT);
                    dragOffset = event.getX() - gridToScreenX(windowLeftRow);
                } else if (allowResizeWindow && event.getX() > windowRight - windowVerticalFrameSize && event.getX() < windowRight - windowVerticalFrameSize + fingerSize) {
                    setDragMode(DRAG_RIGHT);
                    dragOffset = event.getX() - gridToScreenX(windowRightRow);
                } else {
                    setDragMode(DRAG_NONE);
                    return false;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                switch (dragMode) {
                    case DRAG_LEFT: {
                        float newWindowLeftRow = screenToGridX(event.getX() - dragOffset);
                        if (snapToXDistance < 0) {
                            windowLeftRow = Math.max(0, Math.min(windowRightRow - windowSizeMinRows, newWindowLeftRow));
                        } else {
                            float snapLeftRow = snapToNearestRow(newWindowLeftRow);
                            if (windowAnimator == null &&
                                    (snapLeftRow < windowRightRow || (windowRightRow == snapLeftRow && isChartType(ChartType.PIE)))) {
                                animateTo(snapLeftRow, windowRightRow);
                            }
                        }
                        break;
                    }
                    case DRAG_RIGHT: {
                        float newWindowRightRow = screenToGridX(event.getX() - dragOffset);
                        if (snapToXDistance < 0) {
                            windowRightRow = Math.min(chartDataSource.getRowsCount() - 1, Math.max(windowLeftRow + windowSizeMinRows, newWindowRightRow));
                        } else {
                            float snapRightRow = snapToNearestRow(newWindowRightRow);
                            if (windowAnimator == null &&
                                    (snapRightRow > windowLeftRow || (snapRightRow == windowLeftRow && isChartType(ChartType.PIE)))) {
                                animateTo(windowLeftRow, snapRightRow);
                            }
                        }
                        break;
                    }
                    case DRAG_WINDOW: {
                        float windowSizeRows = windowRightRow - windowLeftRow;
                        float newWindowLeftRow = screenToGridX(event.getX() - dragOffset);
                        if (snapToXDistance < 0) {
                            windowLeftRow = Math.max(0, Math.min(chartDataSource.getRowsCount() - 1 - windowSizeRows, newWindowLeftRow));
                            windowRightRow = Math.min(chartDataSource.getRowsCount() - 1, Math.max(0 + windowSizeRows, windowLeftRow + windowSizeRows));
                        } else {
                            float snapLeftRow = snapToNearestRow(newWindowLeftRow);
                            if (snapLeftRow != windowLeftRow && snapLeftRow < chartDataSource.getRowsCount() && windowAnimator == null) {
                                float snapRightRow = snapToNearestRow(snapLeftRow + windowSizeRows);
                                if (!isChartType(ChartType.PIE)) {
                                    snapRightRow--;
                                }
                                float newWindowSize = snapRightRow - snapLeftRow;
                                if (newWindowSize == windowSizeRows
                                        && (snapRightRow > snapLeftRow || (snapRightRow == snapLeftRow && isChartType(ChartType.PIE)))) {
                                    animateTo(snapLeftRow, snapRightRow);
                                }
                            }
                        }
                        break;
                    }
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
                if (listener != null && snapToXDistance < 0) { // when snapping, range will be animated
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

    private void animateTo(float snapLeftRow, float snapRightRow) {
        if (windowAnimator != null) {
            windowAnimator.cancel();
        }
        windowAnimator = new SimpleAnimator();
        windowAnimator.addValue(ANIMATE_WINDOW_LEFT, windowLeftRow, snapLeftRow);
        windowAnimator.addValue(ANIMATE_WINDOW_RIGHT, windowRightRow, snapRightRow);
        windowAnimator.setDuration(150);
        windowAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onUpdate() {
                windowLeftRow = windowAnimator.getFloatValue(ANIMATE_WINDOW_LEFT);
                windowRightRow = windowAnimator.getFloatValue(ANIMATE_WINDOW_RIGHT);
                if (listener != null) {
                    listener.onStartDragging(); // make sure we're still in dragging state
                    listener.onRangeSelected(windowLeftRow, windowRightRow);
                }
                invalidate();
            }

            @Override
            public void onEnd() {
                windowAnimator = null;
            }

            @Override
            public void onCancel() {
                windowAnimator = null;
            }
        });
        windowAnimator.start();
    }

    public void setChartDataSource(ChartDataSource chartDataSource) {
        this.chartDataSource = chartDataSource;
        windowRightRow = chartDataSource.getRowsCount() - 1;
        windowLeftRow = Math.max(0, windowRightRow - 5 * windowSizeMinRows);
        rightYAxisMultiplier = chartDataSource.getRightYAxisMultiplier();
        chartDataSource.addListener(chartDataSourceListener);
        if (listener != null) {
            listener.onRangeSelected(windowLeftRow, windowRightRow);
        }
        update();
    }

    private float gridToScreenX(float gridX) {
        return leftGridOffset + gridX * gridStepX;
    }

    private float gridToScreenY(boolean right, float gridY) {
        if (right) {
            gridY *= rightYAxisMultiplier;
        }
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
        if (chartDataSource != null) {
            gridStepX = (chartDataSource.getRowsCount()) <= 1 ? 0 : gridWidth / (chartDataSource.getRowsCount() - 1);
            gridStepY = (topBound - bottomBound) <= 1 ? 0 : gridHeight / (topBound - bottomBound);
        }
    }

    public void update() {
        float viewWidth = getMeasuredWidth();
        float viewHeight = getMeasuredHeight();

        if (viewWidth == 0 || viewHeight == 0 || chartDataSource == null) {
            return;
        }

        calculateVerticalBounds();
        calculateGridBounds();

        currentChartBitmap = Bitmap.createBitmap((int) gridWidth, (int) gridHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(currentChartBitmap);

        List<ColumnDataSource> visibleColumns = new ArrayList<>();
        for (int column = 0; column < chartDataSource.getColumnsCount(); ++column) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            if (columnDataSource.getType().equals(ColumnType.X) || !chartDataSource.isColumnVisible(column)) {
                continue;
            }
            visibleColumns.add(columnDataSource);
        }

        if (isChartType(ChartType.LINE)) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(chartLineWidth);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);

            float lines[] = new float[4 * (chartDataSource.getRowsCount() - 1)];
            for (ColumnDataSource columnDataSource: visibleColumns) {
                paint.setColor(columnDataSource.getColor());
                long valuesFast[] = columnDataSource.getValues();
                boolean right = columnDataSource.getYAxis().equals(ChartDataSource.YAxis.RIGHT);
                for (int row = 0; row < chartDataSource.getRowsCount() - 1; ++row) {
                    lines[4 * row] = gridToScreenX(row) - leftGridOffset;
                    lines[4 * row + 1] = gridToScreenY(right, valuesFast[row]) - topGridOffset;
                    lines[4 * row + 2] = gridToScreenX(row + 1) - leftGridOffset;
                    lines[4 * row + 3] = gridToScreenY(right, valuesFast[row + 1]) - topGridOffset;
                }
                canvas.drawLines(lines, paint);
            }
        } else if (isChartType(ChartType.BAR_STACK)) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);

            float lines[] = new float[4 * chartDataSource.getRowsCount()];
            float gridBottom = gridHeight;
            float firstX = gridToScreenX(0) + gridStepX / 2 - leftGridOffset;
            boolean first = true;
            for (ColumnDataSource columnDataSource: visibleColumns) {
                paint.setColor(columnDataSource.getColor());
                long valuesFast[] = columnDataSource.getValues();
                ChartDataSource.YAxis yAxis = columnDataSource.getYAxis();
                float multiplier = gridStepY * (yAxis.equals(ChartDataSource.YAxis.RIGHT) ? rightYAxisMultiplier : 1f);
                float fixedGridStepX = gridStepX - gridStepX * 1 / columnDataSource.getRowsCount();
                paint.setStrokeWidth(gridStepX + 1f);
                if (first) {
                    first = false;
                    for (int row = 0; row < chartDataSource.getRowsCount(); ++row) {
                        int offset = 4 * row;
                        lines[offset] = row == 0 ? firstX : (lines[offset + 2 - 4] + fixedGridStepX);
                        lines[offset + 1] = gridHeight + bottomBound * gridStepY - multiplier * valuesFast[row];
                        lines[offset + 2] = lines[offset];
                        lines[offset + 3] = gridBottom;
                    }
                } else {
                    for (int row = 0; row < chartDataSource.getRowsCount(); ++row) {
                        int offset = 4 * row;
                        lines[offset] = lines[offset];
                        lines[offset + 2] = lines[offset];
                        lines[offset + 3] = lines[offset + 1];
                        lines[offset + 1] -= multiplier * valuesFast[row];
                    }
                }
                canvas.drawLines(lines, paint);
            }
        } else if (isChartType(ChartType.PERCENTAGE) || isChartType(ChartType.PIE)) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.FILL);

            float gridBottom = gridHeight;
            float gridToScreenYFast = gridHeight + bottomBound * gridStepY; // - gridY * gridStepY

            Path chartPaths[] = new Path[visibleColumns.size()];
            long[][] fastValues = new long[visibleColumns.size()][];
            int i = 0;
            for (ColumnDataSource column: visibleColumns) {
                fastValues[i++] = column.getValues();
            }

            float firstX = gridToScreenX(0) - leftGridOffset;
            for (int chartIdx = 0; chartIdx < fastValues.length; ++chartIdx) {
                Path currentPath = new Path();
                chartPaths[chartIdx] = currentPath;
                chartPaths[chartIdx].moveTo(firstX, gridBottom);
            }

            for (int row = 0; row < chartDataSource.getRowsCount(); ++row) {
                float curValue = gridToScreenYFast;
                float total = 0f;
                for (int chartIdx = 0; chartIdx < fastValues.length; ++chartIdx) {
                    total += fastValues[chartIdx][row];
                }
                for (int chartIdx = 0; chartIdx < fastValues.length; ++chartIdx) {
                    curValue -= fastValues[chartIdx][row] / total * gridHeight;
                    chartPaths[chartIdx].lineTo(firstX + row * gridStepX, curValue);
                }
            }

            for (int chartIdx = fastValues.length - 1; chartIdx >= 0; --chartIdx) {
                chartPaths[chartIdx].lineTo(gridWidth, gridBottom);
                chartPaths[chartIdx].close();
                paint.setColor(visibleColumns.get(chartIdx).getColor());
                canvas.drawPath(chartPaths[chartIdx], paint);
            }
        }

        invalidate();
    }

    public void setChartLineWidth(float chartLineWidth) {
        this.chartLineWidth = chartLineWidth;
        update();
    }

    public void setListener(RangeListener listener) {
        this.listener = listener;
        if (listener != null) {
            listener.onRangeSelected(windowLeftRow, windowRightRow);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));

        calculateGridBounds();
        update();
        updateBitmaps();
    }

    private void updateBitmaps() {
        float cornerRadius = GeneralUtils.dp2px(getContext(), 6);
        cornerCoverBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cornerCoverBitmap);
        canvas.drawColor(Color.WHITE); // TODO
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        RectF rect = new RectF(leftGridOffset, topGridOffset, leftGridOffset + gridWidth, topGridOffset + gridHeight);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        leftSelectorEdgeBitmap = Bitmap.createBitmap((int) windowVerticalFrameSize, (int) (gridHeight + 2 * windowHorizontalFrameSize), Bitmap.Config.ARGB_8888);
        rightSelectorEdgeBitmap = Bitmap.createBitmap((int) windowVerticalFrameSize, (int) (gridHeight + 2 * windowHorizontalFrameSize), Bitmap.Config.ARGB_8888);
        Canvas leftCanvas = new Canvas(leftSelectorEdgeBitmap);
        Canvas rightCanvas = new Canvas(rightSelectorEdgeBitmap);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(windowPaint.getColor());
        rect = new RectF(0, 0, windowVerticalFrameSize * 2, leftSelectorEdgeBitmap.getHeight());
        leftCanvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        rect = new RectF(-windowVerticalFrameSize, 0, windowVerticalFrameSize, rightSelectorEdgeBitmap.getHeight());
        rightCanvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(GeneralUtils.dp2px(getContext(), 2));
        float lineHalfLength = GeneralUtils.dp2px(getContext(), 6);
        leftCanvas.drawLine(leftSelectorEdgeBitmap.getWidth() * 0.5f, leftSelectorEdgeBitmap.getHeight() * 0.5f - lineHalfLength,
                leftSelectorEdgeBitmap.getWidth() * 0.5f, leftSelectorEdgeBitmap.getHeight() * 0.5f + lineHalfLength,
                paint);
        rightCanvas.drawLine(leftSelectorEdgeBitmap.getWidth() * 0.5f, leftSelectorEdgeBitmap.getHeight() * 0.5f - lineHalfLength,
                leftSelectorEdgeBitmap.getWidth() * 0.5f, leftSelectorEdgeBitmap.getHeight() * 0.5f + lineHalfLength,
                paint);
    }

    private float getWindowLeft() {
        float windowLeft = gridToScreenX(windowLeftRow);
        if (isChartType(ChartType.PIE)) {
            windowLeft = windowLeft - gridStepX * windowLeftRow / (chartDataSource.getRowsCount() - 1);
        }
        return windowLeft;
    }

    private float getWindowRight() {
        float windowRight = gridToScreenX(windowRightRow);
        if (isChartType(ChartType.PIE)) {
            windowRight = windowRight + gridStepX - gridStepX * windowRightRow / (chartDataSource.getRowsCount() - 1);
        }
        return windowRight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (chartDataSource == null) {
            return;
        }

        if (oldChartBitmap != null) {
            canvas.save();
            canvas.clipRect(leftGridOffset, topGridOffset, leftGridOffset + gridWidth, topGridOffset + gridHeight);
            canvas.drawBitmap(oldChartBitmap, oldChartBitmapSrcRect, oldChartBitmapDstRect, oldChartBitmapPaint);
            canvas.drawBitmap(currentChartBitmap, chartBitmapSrcRect, chartBitmapDstRect, chartBitmapPaint);
            canvas.restore();
        } else {
            chartBitmapSrcRect.left = 0;
            chartBitmapSrcRect.top = 0;
            chartBitmapSrcRect.right = currentChartBitmap.getWidth();
            chartBitmapSrcRect.bottom = currentChartBitmap.getHeight();
            chartBitmapDstRect.left = (int) leftGridOffset;
            chartBitmapDstRect.top = (int) topGridOffset;
            chartBitmapDstRect.right = chartBitmapDstRect.left + currentChartBitmap.getWidth();
            chartBitmapDstRect.bottom = chartBitmapDstRect.top + currentChartBitmap.getHeight();
            canvas.drawBitmap(currentChartBitmap, chartBitmapSrcRect, chartBitmapDstRect, chartBitmapPaint);
        }

        float windowLeft = getWindowLeft();
        float windowRight = getWindowRight();
        float top = topGridOffset; // - windowHorizontalFrameSize;
        float bottom = topGridOffset + gridHeight; // + windowHorizontalFrameSize;

        canvas.drawRect(leftGridOffset,
                top,
                windowLeft + windowVerticalFrameSize,
                bottom, backgroundPaint);

        canvas.drawRect(windowRight - windowVerticalFrameSize,
                top,
                leftGridOffset + gridWidth,
                bottom, backgroundPaint);

//        canvas.drawRect(windowLeft, top, windowLeft + windowVerticalFrameSize, bottom, windowPaint);
//        canvas.drawRect(windowRight - windowVerticalFrameSize, top, windowRight, bottom, windowPaint);

        canvas.drawBitmap(cornerCoverBitmap, 0, 0, null);

        top -= windowHorizontalFrameSize;
        bottom += windowHorizontalFrameSize;
        canvas.drawBitmap(leftSelectorEdgeBitmap, windowLeft, top, null);
        canvas.drawBitmap(rightSelectorEdgeBitmap, windowRight - windowVerticalFrameSize, top, null);
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

    private boolean isChartType(ChartType chartType) {
        return chartDataSource != null && chartDataSource.getChartType().equals(chartType);
    }

    public void setSnapToXDistance(long snapToXDistance) {
        this.snapToXDistance = snapToXDistance;
    }

    public void setAllowResizeWindow(boolean allowResizeWindow) {
        this.allowResizeWindow = allowResizeWindow;
    }

}
