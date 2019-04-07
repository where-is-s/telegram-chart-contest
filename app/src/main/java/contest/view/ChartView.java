package contest.view;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.datasource.ValueFormatType;
import contest.utils.EmptyAnimatorListener;
import contest.utils.GeneralUtils;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartView extends View implements RangeListener {

    public static final float NO_BOUND = Float.NaN;

    private static final int HINT_INVISIBLE = 0;
    private static final int HINT_APPEARING = 1;
    private static final int HINT_VISIBLE = 2;
    private static final int HINT_DISAPPEARING = 3;

    private ChartDataSource chartDataSource;
    private List<ColumnDataSource> visibleLineColumnSources = new ArrayList<>();
    private ColumnDataSource xColumnSource;
    private ColumnDataSource yLeftColumnSource;
    private ColumnDataSource yRightColumnSource;
    private ColumnDataSource animatingSource;

    private float chartLineWidth;
    private float vertGridLineInterval;
    private float horzGridValueInterval;
    private int selectedCircleRadius;
    private float gridLineWidth;
    private float hintVertPadding;
    private float hintHorzPadding;
    private float hintHorzMargin;
    private boolean clipToPadding;
    private float fingerSize;
    private int animationSpeed;
    private int hintShadowColor;
    private float hintShadowRadius;
    private int hintBorderRadius;
    private boolean gesturesEnabled;
    private int windowSizeMinRows = 10;

    private float leftBound;
    private float rightBound;
    private float topBound; // calculated only for left Y axis
    private float bottomBound; // calculated only for left Y axis
    private boolean leftBoundFixed = false;
    private boolean rightBoundFixed = false;
    private boolean topBoundFixed = false;
    private boolean bottomBoundFixed = false;

    private float topGridOffset;
    private float bottomGridOffset;
    private float leftGridOffset;
    private float rightGridOffset;
    private float gridWidth;
    private float gridHeight;
    private float gridStepX;
    private float gridStepY;

    private float chartLines[][] = new float[][] {};
    private int chartLineLengths[] = new int[] {};

    private VertGridPainter vertGridPainter = new VertGridPainter();
    private VertGridPainter oldVertGridPainter = new VertGridPainter();
    private boolean drawOldVertGrid = false;

    private int horzGridRows[] = new int[] {};
    private String horzGridValues[] = new String[] {};
    private int oldHorzGridRows[] = new int[] {};
    private String oldHorzGridValues[] = new String[] {};
    private boolean drawOldHorzGrid;

    private Bitmap hintBitmap;
    private float calculatedHintWidth;
    private float calculatedHintHeight;
    private Rect hintBitmapSrcRect = new Rect();
    private Rect hintBitmapDstRect = new Rect();
    private int hintState = HINT_INVISIBLE;

    private float calculatedTopBound; // target bound for animations, calculated only for left Y axis
    private float calculatedBottomBound; // target bound for animations, calculated only for left Y axis
    private float rightYAxisMultiplier = 0.5f;

    private int selectedRow = -1;

    private Paint chartPaints[] = new Paint[] {};
    private Paint selectedLinePaint;
    private Paint horzGridTextPaint;
    private Paint oldHorzGridTextPaint;
    private Paint selectedCircleFillPaint;
    private Paint hintTitlePaint;
    private Paint hintBodyPaint;
    private Paint hintValuePaint;
    private Paint hintNamePaint;
    private Paint hintCopyPaint;

    private class VertGridPainter {
        float lines[] = new float[] {};
        float values[] = new float[] {};

        Paint linePaint;
        Paint textPaint;
        int alpha = 255;
        boolean drawLeft;
        boolean drawRight;

        VertGridPainter() {
            linePaint = new Paint();
            linePaint.setStyle(Paint.Style.STROKE);
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
        }

        void setAlpha(int alpha) {
            this.alpha = alpha;
        }

        void ensureSize(int lines) {
            if (this.lines.length != lines * 4) {
                this.lines = new float[lines * 4];
            }
            if (values.length != lines * 2) {
                values = new float[lines * 2];
            }
        }

        int getLinesCount() {
            return values.length / 2;
        }

        boolean isDifferentFrom(VertGridPainter oldVertGridPainter) {
            return getLinesCount() < 2 || oldVertGridPainter.getLinesCount() != getLinesCount()
                    || values[2] - values[0] != oldVertGridPainter.values[2] - oldVertGridPainter.values[0];
        }

        void setColor(int gridLineColor) {
            linePaint.setColor(gridLineColor);
            textPaint.setColor(gridLineColor);
        }

        void setTextSize(float gridTextSize) {
            textPaint.setTextSize(gridTextSize);
        }

        void setStrokeWidth(float gridLineWidth) {
            linePaint.setStrokeWidth(gridLineWidth);
        }

        void copyTo(VertGridPainter painter) {
            if (painter.lines.length != lines.length) {
                painter.lines = new float[lines.length];
            }
            if (painter.values.length != values.length) {
                painter.values = new float[values.length];
            }
            System.arraycopy(lines, 0, painter.lines, 0, lines.length);
            System.arraycopy(values, 0, painter.values, 0, values.length);
            painter.drawLeft = drawLeft;
            painter.drawRight = drawRight;
        }

        void init() {
            drawLeft = chartDataSource.isColumnVisible(yLeftColumnSource);
            drawRight = chartDataSource.isColumnVisible(yRightColumnSource);
        }

        void draw(Canvas canvas) {
            canvas.drawLines(lines, linePaint);
            float fontHeight = getFontHeight(textPaint);
            for (int lineIdx = 0; lineIdx < lines.length / 4; ++lineIdx) {
                if (drawLeft) {
                    textPaint.setTextAlign(Paint.Align.LEFT);
                    textPaint.setColor(yLeftColumnSource.getColor());
                    textPaint.setAlpha(alpha);
                    canvas.drawText(formatGridValue(true, values[2 * lineIdx]),
                            lines[4 * lineIdx] + gridLineWidth,
                            lines[4 * lineIdx + 1] - fontHeight / 3,
                            textPaint);
                }
                if (drawRight) {
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    textPaint.setColor(yRightColumnSource.getColor());
                    textPaint.setAlpha(alpha);
                    canvas.drawText(formatGridValue(false, values[2 * lineIdx + 1]),
                            lines[4 * lineIdx + 2] - gridLineWidth,
                            lines[4 * lineIdx + 1] - fontHeight / 3,
                            textPaint);
                } else {
                    int a = 10;
                }
            }
        }
    }

    private float touchBeginX;
    private ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
        private float oldFocusX;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!gesturesEnabled) {
                return false;
            }
            float x = detector.getFocusX() / getMeasuredWidth();
            float center = leftBound + (rightBound - leftBound) * x;
            float width = (rightBound - leftBound) / (detector.getScaleFactor() * detector.getScaleFactor());
            width = Math.max(width, windowSizeMinRows);
            float offset = (detector.getFocusX() - oldFocusX) / getMeasuredWidth() * width;
            oldFocusX = detector.getFocusX();
            setBounds(center - width * x - offset, center + width * (1 - x) - offset, true);
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (!gesturesEnabled) {
                return false;
            }
            oldFocusX = detector.getFocusX();
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    });
    private GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {
        private ValueAnimator flingAnimator;

        @Override
        public boolean onDown(MotionEvent e) {
            if (!gesturesEnabled) {
                return true;
            }
            if (flingAnimator != null) {
                flingAnimator.cancel();
                flingAnimator = null;
            }
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            playSoundEffect(SoundEffectConstants.CLICK);
            if (hintState == HINT_VISIBLE
                    && event.getX() > hintBitmapDstRect.left
                    && event.getX() < hintBitmapDstRect.right
                    && event.getY() > hintBitmapDstRect.top
                    && event.getY() < hintBitmapDstRect.bottom) {
                setSelectedRow(-1);
                hintState = HINT_DISAPPEARING;
                startHintAnimation(
                        hintBitmapDstRect.left,
                        hintBitmapDstRect.top,
                        hintBitmapDstRect.right,
                        hintBitmapDstRect.bottom
                );
            } else {
                selectNearest(event.getX(), event.getY(), fingerSize);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            getParent().requestDisallowInterceptTouchEvent(true);
            if (!gesturesEnabled) {
                selectNearest(e2.getX(), e2.getY(), fingerSize / 2);
            } else {
                float newLeftBound = Math.max(0, leftBound + screenToGridX(distanceX) - screenToGridX(0));
                float newRightBound = newLeftBound + rightBound - leftBound;
                if (newRightBound > chartDataSource.getRowsCount() - 1) {
                    newRightBound = chartDataSource.getRowsCount() - 1;
                    newLeftBound = newRightBound - (rightBound - leftBound);
                }
                setBounds(newLeftBound, newRightBound, true);
            }
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (!gesturesEnabled) {
                return false;
            }
            float normalVelocity = (float) (Math.pow(Math.abs(velocityX), 0.8f) * Math.signum(velocityX));
            final float rows = -(rightBound - leftBound) * normalVelocity / getMeasuredWidth();
            flingAnimator = ValueAnimator.ofFloat(0, rows);
            flingAnimator.setDuration(500);
            flingAnimator.setInterpolator(new DecelerateInterpolator());
            final float initialLeftBound = leftBound;
            final float initialRightBound = rightBound;
            flingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float value = (float) animation.getAnimatedValue();
                    float newLeftBound = Math.max(0, initialLeftBound + value);
                    float newRightBound = newLeftBound + initialRightBound - initialLeftBound;
                    if (newRightBound > chartDataSource.getRowsCount() - 1) {
                        newRightBound = chartDataSource.getRowsCount() - 1;
                        newLeftBound = newRightBound - (initialRightBound - initialLeftBound);
                    }
                    setBounds(newLeftBound, newRightBound, true);
                }
            });
            flingAnimator.start();
            return true;
        }
    });

    private RangeListener listener;

    private ChartDataSource.Listener chartDataSourceListener = new ChartDataSource.Listener() {
        @Override
        public void onSetColumnVisibility(int column, boolean visible) {
            final ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            animatingSource = columnDataSource;
            float savedTopBound = topBound;
            float savedBottomBound = bottomBound;
            if (chartAnimator != null) {
                chartAnimator.cancel();
                animatingSource = columnDataSource; // restore animating source because it is nulled on animation cancel
                updateColumns();
            }
            chartAnimator = new ValueAnimator();
            chartAnimator.setDuration(animationSpeed);
            PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofInt("alpha", visible ? 0 : 255, visible ? 255 : 0);
            PropertyValuesHolder oldGridAlphaProperty = PropertyValuesHolder.ofInt("oldGridAlpha", 255, 0);
            PropertyValuesHolder newGridAlphaProperty = PropertyValuesHolder.ofInt("newGridAlpha", 0, 255);
            calculateVertBounds();
            PropertyValuesHolder topBoundProperty = PropertyValuesHolder.ofFloat("topBound", savedTopBound, calculatedTopBound);
            PropertyValuesHolder bottomBoundProperty = PropertyValuesHolder.ofFloat("bottomBound", savedBottomBound, calculatedBottomBound);
            chartAnimator.setValues(alphaProperty, oldGridAlphaProperty, newGridAlphaProperty, topBoundProperty, bottomBoundProperty);
            chartAnimator.setInterpolator(new DecelerateInterpolator());
            chartAnimator.addListener(new EmptyAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    chartAnimator = null;
                    animatingSource = null;
                    drawOldVertGrid = false;
                    updateColumns(); // invisible column will finally be removed from visibleLineColumnSources
                    updateHint();
                    updateChart(true);
                }

//                @Override
//                public void onAnimationCancel(Animator animation) {
//                    onAnimationEnd(animation);
//                }
            });
            if (visible) {
                updateColumns();
            }
            final Paint chartPaint = chartPaints[visibleLineColumnSources.indexOf(columnDataSource)];
            vertGridPainter.copyTo(oldVertGridPainter);
            vertGridPainter.init();
            drawOldVertGrid = updateVertGrid(true);
            chartAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    chartPaint.setAlpha((Integer) animator.getAnimatedValue("alpha"));
                    updateHint();
                    if (oldVertGridPainter != null) {
                        oldVertGridPainter.setAlpha((Integer) animator.getAnimatedValue("oldGridAlpha"));
                        vertGridPainter.setAlpha((Integer) animator.getAnimatedValue("newGridAlpha"));
                    } else {
                        oldVertGridPainter.setAlpha(255);
                        vertGridPainter.setAlpha(255);
                    }
                    if (!topBoundFixed) {
                        topBound = (float) animator.getAnimatedValue("topBound");
                    }
                    if (!bottomBoundFixed) {
                        bottomBound = (float) animator.getAnimatedValue("bottomBound");
                    }
                    updateGridOffsets();
                    updateChart(false);
                }
            });
            chartAnimator.start();
        }
    };

    private ValueAnimator chartAnimator;
    private ValueAnimator hintAnimator;
    private boolean isDragging;

    public ChartView(Context context) {
        super(context);
        init();
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChartView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        selectedLinePaint = new Paint();
        selectedLinePaint.setStyle(Paint.Style.STROKE);
        horzGridTextPaint = new Paint();
        horzGridTextPaint.setAntiAlias(true);
        horzGridTextPaint.setTextAlign(Paint.Align.CENTER);
        oldHorzGridTextPaint = new Paint();
        oldHorzGridTextPaint.setAntiAlias(true);
        oldHorzGridTextPaint.setTextAlign(Paint.Align.CENTER);
        selectedCircleFillPaint = new Paint();
        selectedCircleFillPaint.setStyle(Paint.Style.FILL);
        hintTitlePaint = new Paint();
        hintTitlePaint.setAntiAlias(true);
        hintTitlePaint.setTypeface(GeneralUtils.getMediumTypeface());
        hintTitlePaint.setStyle(Paint.Style.FILL);
        hintBodyPaint = new Paint();
        hintBodyPaint.setStyle(Paint.Style.FILL);
        hintValuePaint = new Paint();
        hintValuePaint.setAntiAlias(true);
        hintValuePaint.setStyle(Paint.Style.FILL);
        hintValuePaint.setTypeface(GeneralUtils.getMediumTypeface());
        hintNamePaint = new Paint();
        hintNamePaint.setAntiAlias(true);
        hintNamePaint.setStyle(Paint.Style.FILL);
        hintCopyPaint = new Paint();
        hintCopyPaint.setAntiAlias(true);
        setChartLineWidth(GeneralUtils.dp2px(getContext(), 2));
        setVertGridLineInterval(GeneralUtils.dp2px(getContext(), 40));
        setHorzGridValueInterval(GeneralUtils.dp2px(getContext(), 20));
        setGridLineColor(0xfff3f3f3);
        setGridTextColor(0xffa0abb2);
        setGridTextSize(GeneralUtils.sp2px(getContext(), 12));
        setSelectedCircleRadius(GeneralUtils.dp2px(getContext(), 4));
        setSelectedCircleFillColor(Color.WHITE);
        setGridLineWidth(GeneralUtils.dp2px(getContext(), 1));
        setHintBackgroundColor(Color.WHITE);
        setHintTitleTextColor(0xff222222);
        setHintTitleTextSize(GeneralUtils.sp2px(getContext(), 12));
        setHintVertPadding(GeneralUtils.dp2px(getContext(), 6));
        setHintHorzPadding(GeneralUtils.dp2px(getContext(), 14));
        setHintHorzMargin(GeneralUtils.dp2px(getContext(), 20));
        setHintChartValueTextSize(GeneralUtils.sp2px(getContext(), 15));
        setHintChartNameTextSize(GeneralUtils.sp2px(getContext(), 12));
        setClipToPadding(false);
        setFingerSize(GeneralUtils.dp2px(getContext(), 24));
        setAnimationSpeed(300);
        setHintShadowColor(0x20000000);
        setHintShadowRadius(GeneralUtils.dp2px(getContext(), 2));
        setHintBorderRadius(GeneralUtils.dp2px(getContext(), 4));
        setGesturesEnabled(true);
        updateGridOffsets();
    }

    public void setChartNavigationView(RangeListener listener) {
        this.listener = listener;
    }

    public void setChartLineWidth(float chartLineWidth) {
        this.chartLineWidth = chartLineWidth;
        updateChart(true);
    }

    public void setVertGridLineInterval(float vertGridLineInterval) {
        this.vertGridLineInterval = vertGridLineInterval;
        updateChart(true);
    }

    public void setHorzGridValueInterval(float horzGridValueInterval) {
        this.horzGridValueInterval = horzGridValueInterval;
        updateChart(true);
    }

    public void setGridLineColor(int gridLineColor) {
        vertGridPainter.setColor(gridLineColor);
        oldVertGridPainter.setColor(gridLineColor);
        selectedLinePaint.setColor(gridLineColor);
        invalidate();
    }

    public void setGridTextColor(int gridTextColor) {
        horzGridTextPaint.setColor(gridTextColor);
        oldHorzGridTextPaint.setColor(gridTextColor);
        updateChart(true);
    }

    public void setGridTextSize(float gridTextSize) {
        vertGridPainter.setTextSize(gridTextSize);
        oldVertGridPainter.setTextSize(gridTextSize);
        horzGridTextPaint.setTextSize(gridTextSize);
        oldHorzGridTextPaint.setTextSize(gridTextSize);
        updateChart(true);
    }

    public void setSelectedCircleRadius(int selectedCircleRadius) {
        this.selectedCircleRadius = selectedCircleRadius;
        updateChart(true);
    }

    public void setSelectedCircleFillColor(int color) {
        selectedCircleFillPaint.setColor(color);
        invalidate();
    }

    public void setGridLineWidth(float gridLineWidth) {
        this.gridLineWidth = gridLineWidth;
        vertGridPainter.setStrokeWidth(gridLineWidth);
        oldVertGridPainter.setStrokeWidth(gridLineWidth);
        selectedLinePaint.setStrokeWidth(gridLineWidth);
        invalidate();
    }

    public void setHintBackgroundColor(int color) {
        hintBodyPaint.setColor(color);
        drawHintBitmap();
    }

    public void setHintTitleTextColor(int hintTitleTextColor) {
        hintTitlePaint.setColor(hintTitleTextColor);
        updateHint();
    }

    public void setHintTitleTextSize(int hintTitleTextSize) {
        hintTitlePaint.setTextSize(hintTitleTextSize);
        updateHint();
    }

    public void setHintVertPadding(float hintVertPadding) {
        this.hintVertPadding = hintVertPadding;
        updateHint();
    }

    public void setHintHorzPadding(float hintHorzPadding) {
        this.hintHorzPadding = hintHorzPadding;
        updateHint();
    }

    public void setHintHorzMargin(float hintHorzMargin) {
        this.hintHorzMargin = hintHorzMargin;
        updateHint();
    }

    public void setHintChartValueTextSize(float hintChartValueTextSize) {
        hintValuePaint.setTextSize(hintChartValueTextSize);
        updateHint();
    }

    public void setHintChartNameTextSize(float hintChartNameTextSize) {
        hintNamePaint.setTextSize(hintChartNameTextSize);
        updateHint();
    }

    public void setClipToPadding(boolean clipToPadding) {
        this.clipToPadding = clipToPadding;
        updateChart(true);
        updateHint();
    }

    public void setFingerSize(float fingerSize) {
        this.fingerSize = fingerSize;
        updateChart(true);
    }

    public void setAnimationSpeed(int animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    public void setHintShadowRadius(float hintShadowRadius) {
        this.hintShadowRadius = hintShadowRadius;
        hintBodyPaint.setShadowLayer(hintShadowRadius, 0f, hintShadowRadius / 2, hintShadowColor);
        updateHint();
    }

    public void setHintShadowColor(int hintShadowColor) {
        this.hintShadowColor = hintShadowColor;
        hintBodyPaint.setShadowLayer(hintShadowRadius, 0f, hintShadowRadius / 2, hintShadowColor);
        updateHint();
    }

    public void setHintBorderRadius(int hintBorderRadius) {
        this.hintBorderRadius = hintBorderRadius;
        updateHint();
    }

    public void setGesturesEnabled(boolean gesturesEnabled) {
        this.gesturesEnabled = gesturesEnabled;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(false);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchBeginX = event.getX();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() > 1 || (Math.abs(touchBeginX - event.getX()) > fingerSize / 2)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
        }
        return super.onTouchEvent(event);
    }

    private float sqr(float val) {
        return val * val;
    }

    private void selectNearest(float screenX, float screenY, float searchSize) {
        float minGridX = screenToGridX(screenX - searchSize / 2);
        float maxGridX = screenToGridX(screenX + searchSize / 2);
        int minGridRoundX = (int) Math.min(Math.max(Math.round(minGridX), Math.ceil(leftBound)), Math.floor(rightBound));
        int maxGridRoundX = (int) Math.min(Math.max(Math.round(maxGridX), Math.ceil(leftBound)), Math.floor(rightBound));
        float bestScreenDistance = Float.MAX_VALUE;
        int bestRow = Math.round((minGridRoundX + maxGridRoundX) * 0.5f); // middle is default
        for (int row = minGridRoundX; row <= maxGridRoundX; ++row) {
            for (int column = 0; column < chartDataSource.getColumnsCount(); ++column) {
                ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
                if (!columnDataSource.getType().equals(ColumnType.LINE)) {
                    continue;
                }
                long value = columnDataSource.getValue(row);
                float screenDistance = sqr(gridToScreenY(columnDataSource.getYAxis(), value) - screenY) + sqr(gridToScreenX(row) - screenX);
                if (screenDistance < bestScreenDistance) {
                    bestScreenDistance = screenDistance;
                    bestRow = row;
                }
            }
        }
        setSelectedRow(bestRow);
    }

    public void setLeftBound(float bound) {
        this.leftBound = bound;
        this.leftBoundFixed = !Float.isNaN(bound);
        updateChart(true);
    }

    public void setRightBound(float bound) {
        this.rightBound = bound;
        this.rightBoundFixed = !Float.isNaN(bound);
        updateChart(true);
    }

    public void setTopBound(float bound) {
        this.topBound = bound;
        this.topBoundFixed = !Float.isNaN(bound);
        updateChart(true);
    }

    public void setBottomBound(float bound) {
        this.bottomBound = bound;
        this.bottomBoundFixed = !Float.isNaN(bound);
        updateChart(true);
    }

    public void setChartDataSource(final ChartDataSource chartDataSource) {
        if (this.chartDataSource != null) {
            this.chartDataSource.removeListener(chartDataSourceListener);
        }
        this.chartDataSource = chartDataSource;
        rightYAxisMultiplier = chartDataSource.getRightYAxisMultiplier();
        updateColumns();
        vertGridPainter.init();
        drawOldVertGrid = false;
        chartDataSource.addListener(chartDataSourceListener);
        setBounds(0, chartDataSource.getRowsCount() - 1, false);
        updateChart(true);
    }

    private void updateColumns() {
        visibleLineColumnSources.clear();
        for (int column = 0; column < chartDataSource.getColumnsCount(); ++column) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            if (!chartDataSource.isColumnVisible(column) && animatingSource != columnDataSource) {
                continue;
            }
            if (columnDataSource.getType().equals(ColumnType.LINE)) {
                visibleLineColumnSources.add(columnDataSource);
            }
        }
        xColumnSource = chartDataSource.getColumn(chartDataSource.getXAxisValueSourceColumn());
        yLeftColumnSource = chartDataSource.getColumn(chartDataSource.getYAxisValueSourceColumnLeft());
        yRightColumnSource = chartDataSource.getColumn(chartDataSource.getYAxisValueSourceColumnRight());
        if (chartPaints.length != visibleLineColumnSources.size()) {
            chartPaints = new Paint[visibleLineColumnSources.size()];
        }
        for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(chartLineWidth);
            paint.setColor(visibleLineColumnSources.get(c).getColor());
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStyle(Paint.Style.STROKE);
            chartPaints[c] = paint;
        }
        updateHint();
        invalidate();
    }

    private float gridToScreenX(float gridX) {
        return leftGridOffset + (gridX - leftBound) * gridStepX;
    }

    private float gridToScreenY(ChartDataSource.YAxis yAxis, float gridY) {
        if (yAxis.equals(ChartDataSource.YAxis.LEFT)) {
            return topGridOffset + gridHeight - (gridY - bottomBound) * gridStepY;
        } else {
            return topGridOffset + gridHeight - (gridY / rightYAxisMultiplier - bottomBound) * gridStepY;
        }
    }

    private float screenToGridX(float screenX) {
        float x = screenX - leftGridOffset;
        float valuesCount = (rightBound - leftBound);
        if (valuesCount == 0) {
            return leftBound + valuesCount / 2;
        }
        return leftBound + valuesCount * x / gridWidth;
    }

    private float screenToGridY(float screenY) {
        float localY = bottomGridOffset - screenY;
        float valuesCount = (topBound - bottomBound);
        if (valuesCount == 0) {
            return bottomBound + valuesCount / 2;
        }
        return bottomBound + valuesCount * localY / gridHeight;
    }

    private int getLefterBound() {
        return (int) Math.max(0, screenToGridX(clipToPadding ? leftGridOffset : 0));
    }

    private int getRighterBound() {
        return (int) Math.min(chartDataSource.getRowsCount() - 1, screenToGridX(getMeasuredWidth() - (clipToPadding ? rightGridOffset : 0)) + 1);
    }

    private void calculateVertBounds() {
        int lefterBound = getLefterBound();
        int righterBound = getRighterBound();
        calculatedBottomBound = Float.MAX_VALUE;
        calculatedTopBound = Float.MIN_VALUE;
        for (int column = 0; column < chartDataSource.getColumnsCount(); ++column) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            if (columnDataSource.getType().equals(ColumnType.X)
                    || !chartDataSource.isColumnVisible(column)) {
                continue;
            }
            long[] valuesFast = columnDataSource.getValues();
            ChartDataSource.YAxis yAxis = columnDataSource.getYAxis();
            float multiplier = yAxis.equals(ChartDataSource.YAxis.RIGHT) ? (1f / rightYAxisMultiplier) : 1f;
            for (int row = lefterBound; row <= righterBound; ++row) {
                long value = valuesFast[row];
                value *= multiplier;
                if (calculatedBottomBound > value) {
                    calculatedBottomBound = value;
                }
                if (calculatedTopBound < value) {
                    calculatedTopBound = value;
                }
            }
        }
        if (bottomBoundFixed) {
            calculatedBottomBound = bottomBound;
        } else {
            float vertGridLinesCount = Math.max(2, (int) Math.floor(gridHeight / vertGridLineInterval));
            float valueSpacing = (float) Math.floor((calculatedTopBound - calculatedBottomBound) / (vertGridLinesCount - 1));
            valueSpacing = gridRound(valueSpacing);
            calculatedBottomBound = (float) (Math.floor(calculatedBottomBound / valueSpacing) * valueSpacing);
        }
        if (topBoundFixed) {
            calculatedTopBound = topBound;
        }
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        updateGridOffsets();
    }

    private static float getFontHeight(Paint textPaint) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        return fm.descent - fm.ascent;
    }

    private void updateGridOffsets() {
        topGridOffset = getPaddingTop() + getFontHeight(vertGridPainter.textPaint);
        leftGridOffset = getPaddingLeft() + gridLineWidth / 2;
        rightGridOffset = getPaddingRight() + gridLineWidth / 2;
        bottomGridOffset = getPaddingBottom() + getFontHeight(horzGridTextPaint);
        gridWidth = getMeasuredWidth() - leftGridOffset - rightGridOffset;
        gridHeight = getMeasuredHeight() - topGridOffset - bottomGridOffset;
        float valuesCount = (rightBound - leftBound);
        gridStepX = valuesCount <= 1 ? 0 : gridWidth / valuesCount;
        valuesCount = (topBound - bottomBound);
        gridStepY = valuesCount <= 1 ? 0 : gridHeight / valuesCount;
    }

    private void setBounds(float left, float right, boolean animation) {
        if ((leftBound == left || leftBoundFixed) && (rightBound == right || rightBoundFixed)) {
            updateChart(false);
            return;
        }
        if (!leftBoundFixed) {
            leftBound = Math.max(0, left);
        }
        if (!rightBoundFixed) {
            rightBound = Math.min(chartDataSource.getRowsCount() - 1, right);
        }
        if (listener != null) {
            listener.onRangeSelected(leftBound, rightBound);
        }
        updateGridOffsets();
        if (animation) {
            startRangeAnimation();
            placeHint();
        }
    }

    private void updateChart(boolean reset) {
        if (getMeasuredWidth() == 0 || getMeasuredHeight() == 0) {
            return;
        }

        int lefterBound = getLefterBound();
        int righterBound = getRighterBound();

        int chartLineLengthInArray = 4 * (righterBound - lefterBound);
        if (chartLineLengthInArray < 0) {
            return;
        }
        if (chartLines.length < visibleLineColumnSources.size()) {
            chartLines = new float[visibleLineColumnSources.size()][];
            chartLineLengths = new int[visibleLineColumnSources.size()];
        }
        for (int i = 0; i < chartLines.length; ++i) {
            if (chartLines[i] == null || chartLines[i].length < chartLineLengthInArray) {
                chartLines[i] = new float[chartLineLengthInArray];
            }
        }
        if (chartPaints.length != visibleLineColumnSources.size()) {
            chartPaints = new Paint[visibleLineColumnSources.size()];
        }
        if (reset) {
            calculateVertBounds();
            if (!topBoundFixed) {
                topBound = calculatedTopBound;
            }
            if (!bottomBoundFixed) {
                bottomBound = calculatedBottomBound;
            }
            updateGridOffsets();
        }

        int lineIdx = 0;
        float gridToScreenYFast = topGridOffset + gridHeight + bottomBound * gridStepY; // - gridY * gridStepY
        for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
            long valuesFast[] = columnDataSource.getValues();
            ChartDataSource.YAxis yAxis = columnDataSource.getYAxis();
            float multiplier = yAxis.equals(ChartDataSource.YAxis.RIGHT) ? (1 / rightYAxisMultiplier) : 1f;
            float firstX = gridToScreenX(lefterBound);
            float currentLines[] = chartLines[lineIdx];
            for (int row = 0; row < righterBound - lefterBound; ++row) {
                int offset = 4 * row;

                currentLines[offset] = row == 0 ? firstX : currentLines[offset + 2 - 4];
                currentLines[offset + 1] = row == 0 ? (gridToScreenYFast - gridStepY * (multiplier * valuesFast[row + lefterBound])) : currentLines[offset + 3 - 4];
                currentLines[offset + 2] = firstX + (row + 1) * gridStepX;
                currentLines[offset + 3] = gridToScreenYFast - gridStepY * (multiplier * valuesFast[row + lefterBound + 1]);
            }
            chartLineLengths[lineIdx] = 4 * (righterBound - lefterBound - 1);
            lineIdx++;
        }

        updateVertGrid(reset);
        if (reset) {
            updateHorzGrid();
        }
        invalidate();
    }

    public void update() {
        updateColumns();
        updateHint();
        updateChart(true);
    }

    private void saveOldHorzGrid() {
        if (oldHorzGridRows.length != horzGridRows.length) {
            oldHorzGridRows = new int[horzGridRows.length];
        }
        if (oldHorzGridValues.length != horzGridValues.length) {
            oldHorzGridValues = new String[horzGridValues.length];
        }
        System.arraycopy(horzGridRows, 0, oldHorzGridRows, 0, oldHorzGridRows.length);
        System.arraycopy(horzGridValues, 0, oldHorzGridValues, 0, oldHorzGridValues.length);
    }

    private float gridRound(float value) {
        double degree10 = Math.floor(Math.log10(value));
        value *= Math.pow(10, -degree10);
        // round grid to values like 1/2.5/5/10/25/50/100/etc. so it could scale nicely
        if (value < (1 + 2.5) / 2) {
            value = 1;
        } else if (value < (2.5 + 5) / 2) {
            value = 2.5f;
        } else {
            value = 5f;
        }
        value *= Math.pow(10, degree10);
        return value;
    }

    /**
     * @param reset
     * @return true if grid is different from old grid and animation is required, false otherwise
     */
    private boolean updateVertGrid(boolean reset) {
        float viewWidth = getMeasuredWidth();

        int gridLinesCount;
        float valueSpacing = 0;
        if (reset) {
            gridLinesCount = Math.max(2, (int) Math.floor(gridHeight / vertGridLineInterval));

            valueSpacing = (float) Math.floor((calculatedTopBound - calculatedBottomBound) / (gridLinesCount - 1));
            valueSpacing = gridRound(valueSpacing);

            gridLinesCount = (int) Math.floor((calculatedTopBound - calculatedBottomBound) / valueSpacing) + 1;

            vertGridPainter.ensureSize(gridLinesCount);
        } else {
            gridLinesCount = vertGridPainter.getLinesCount();
        }

        for (int gridLine = 0; gridLine < gridLinesCount; ++gridLine) {
            if (reset) {
                vertGridPainter.values[2 * gridLine] = calculatedBottomBound + valueSpacing * gridLine;
                vertGridPainter.values[2 * gridLine + 1] = valueSpacing * gridLine * rightYAxisMultiplier; // + rightAxisOffset;
            }
            vertGridPainter.lines[4 * gridLine] = leftGridOffset;
            vertGridPainter.lines[4 * gridLine + 1] = gridToScreenY(ChartDataSource.YAxis.LEFT, vertGridPainter.values[2 * gridLine]);
            vertGridPainter.lines[4 * gridLine + 2] = viewWidth - rightGridOffset;
            vertGridPainter.lines[4 * gridLine + 3] = vertGridPainter.lines[4 * gridLine + 1];
        }

        if (drawOldVertGrid) {
            for (int gridLine = 0; gridLine < oldVertGridPainter.lines.length / 4; ++gridLine) {
                oldVertGridPainter.lines[4 * gridLine] = leftGridOffset;
                oldVertGridPainter.lines[4 * gridLine + 1] = gridToScreenY(ChartDataSource.YAxis.LEFT, oldVertGridPainter.values[2 * gridLine]);
                oldVertGridPainter.lines[4 * gridLine + 2] = viewWidth - rightGridOffset;
                oldVertGridPainter.lines[4 * gridLine + 3] = oldVertGridPainter.lines[4 * gridLine + 1];
            }
        }

        invalidate();

        return vertGridPainter.isDifferentFrom(oldVertGridPainter);
    }

    /**
     * @return true if grid is different from old grid and animation is required, false otherwise
     */
    private boolean updateHorzGrid() {
        if (getMeasuredWidth() == 0 || getMeasuredHeight() == 0) {
            return false;
        }

        float markerSample1 = horzGridTextPaint.measureText(xColumnSource.formatValue(xColumnSource.getValue(0), ValueFormatType.HORZ_GRID));
        float markerSample2 = horzGridTextPaint.measureText(xColumnSource.formatValue(xColumnSource.getValue(chartDataSource.getRowsCount() - 1), ValueFormatType.HORZ_GRID));
        float fullXValueWidth = Math.max(markerSample1, markerSample2) + horzGridValueInterval;
        float xValuesPerScreen = (float) Math.floor(gridWidth / fullXValueWidth);
        float gridDistancePerMarker = (rightBound - leftBound) / xValuesPerScreen;
        double degree2 = Math.ceil(Math.log10(gridDistancePerMarker) / Math.log10(2));
        gridDistancePerMarker = (float) Math.pow(2, degree2);
        int rowsPerHorzMarker = (int) gridDistancePerMarker;

        if (rowsPerHorzMarker == 0) {
            return false;
        }

        float textFitLeftBound = screenToGridX(0);
        float textFitRightBound = screenToGridX(getMeasuredWidth());
        int leftRow = (int) (Math.max(0, Math.floor(textFitLeftBound / rowsPerHorzMarker) * rowsPerHorzMarker));
        int rightRow = (int) (Math.min(chartDataSource.getRowsCount() - 1, Math.ceil(textFitRightBound / rowsPerHorzMarker) * rowsPerHorzMarker));
        int horzGridValuesCount = (rightRow - leftRow) / rowsPerHorzMarker + 1;

        if (horzGridRows.length != horzGridValuesCount) {
            horzGridRows = new int[horzGridValuesCount];
            horzGridValues = new String[horzGridValuesCount];
        }
        for (int c = 0; c < horzGridValuesCount; ++c) {
            int row = leftRow + c * rowsPerHorzMarker;
            if (horzGridRows[c] != row || horzGridValues[c] == null) {
                horzGridRows[c] = row;
                horzGridValues[c] = xColumnSource.formatValue(xColumnSource.getValue(row), ValueFormatType.HORZ_GRID);
            }
        }

        invalidate();

        if (horzGridRows.length < 2 || oldHorzGridRows.length < 2) {
            return true;
        }
        int oldDelta = oldHorzGridRows[1] - oldHorzGridRows[0];
        int newDelta = horzGridRows[1] - horzGridRows[0];
        if (newDelta > 2 * oldDelta) {
            return false;
        }
        return oldDelta != newDelta;
    }

    public void setSelectedRow(int selectedRow) {
        this.selectedRow = selectedRow;
        updateHint();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));

        updateGridOffsets();
        updateChart(true);
    }

    private void updateHint() {
        if (selectedRow < 0 || xColumnSource == null) {
            calculatedHintWidth = 0;
            calculatedHintHeight = 0;
            return;
        }
        float titleTextWidth = hintTitlePaint.measureText(xColumnSource.formatValue(xColumnSource.getValue(selectedRow), ValueFormatType.HINT_TITLE));
        float bodyWidth = 0;
        for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
            float lineHintWidth;
            lineHintWidth = hintValuePaint.measureText(columnDataSource.formatValue(columnDataSource.getValue(selectedRow), ValueFormatType.HINT_VALUE));
            lineHintWidth = Math.max(lineHintWidth, hintNamePaint.measureText(columnDataSource.getName()));
            if (bodyWidth > 0.01f) {
                bodyWidth += hintHorzMargin;
            }
            bodyWidth += lineHintWidth;
        }
        calculatedHintWidth = 2 * hintHorzPadding + Math.max(titleTextWidth, bodyWidth);
        calculatedHintHeight = hintVertPadding + getFontHeight(hintTitlePaint) + hintVertPadding + getFontHeight(hintValuePaint) + hintVertPadding + getFontHeight(hintNamePaint) + hintVertPadding;
        drawHintBitmap();
        placeHint();
    }

    private void drawHintBitmap() {
        if (hintBitmap != null && (hintBitmap.getWidth() != calculatedHintWidth || hintBitmap.getHeight() != calculatedHintHeight)) {
            hintBitmap.recycle();
            hintBitmap = null;
        }

        if (selectedRow < 0) {
            return;
        }

        if (hintBitmap == null) {
            hintBitmap = Bitmap.createBitmap((int) (calculatedHintWidth + 4 * hintShadowRadius), (int) (calculatedHintHeight + 4 * hintShadowRadius), Bitmap.Config.ARGB_8888);
            hintBitmapSrcRect.left = 0;
            hintBitmapSrcRect.top = 0;
            hintBitmapSrcRect.right = hintBitmap.getWidth();
            hintBitmapSrcRect.bottom = hintBitmap.getHeight();
        }

        Canvas canvas = new Canvas(hintBitmap);
        RectF hintRect = new RectF(hintShadowRadius * 2, hintShadowRadius * 2, calculatedHintWidth + hintShadowRadius * 2, calculatedHintHeight + hintShadowRadius * 2);
        canvas.drawRoundRect(hintRect, hintBorderRadius, hintBorderRadius, hintBodyPaint);
        canvas.drawText(xColumnSource.formatValue(xColumnSource.getValue(selectedRow), ValueFormatType.HINT_TITLE), hintRect.left + hintHorzPadding, hintRect.top + hintVertPadding + getFontHeight(hintTitlePaint), hintTitlePaint);
        float currentLeft = hintRect.left + hintHorzPadding;
        float currentTop = hintRect.top + hintVertPadding + getFontHeight(hintTitlePaint) + hintVertPadding;
        for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
            ColumnDataSource columnDataSource = visibleLineColumnSources.get(c);
            float lineHintWidth;
            hintValuePaint.setColor(columnDataSource.getColor());
            hintValuePaint.setAlpha(chartPaints[c].getAlpha());
            String value = columnDataSource.formatValue(columnDataSource.getValue(selectedRow), ValueFormatType.HINT_VALUE);
            canvas.drawText(value, currentLeft, currentTop + hintValuePaint.getTextSize(), hintValuePaint);
            lineHintWidth = hintValuePaint.measureText(value);

            hintNamePaint.setColor(columnDataSource.getColor());
            hintNamePaint.setAlpha(chartPaints[c].getAlpha());
            canvas.drawText(columnDataSource.getName(), currentLeft, currentTop + hintValuePaint.getTextSize() + hintVertPadding / 2 + hintNamePaint.getTextSize(), hintNamePaint);
            lineHintWidth = Math.max(lineHintWidth, hintNamePaint.measureText(columnDataSource.getName()));

            currentLeft += lineHintWidth + hintHorzMargin;
        }

        invalidate();
    }

    private void placeHint() {
        if (selectedRow < 0) {
            return;
        }
        if (hintState == HINT_DISAPPEARING || hintState == HINT_APPEARING) { // already animating to appear/disappear
            return;
        }
        float x = gridToScreenX(selectedRow);
        boolean shouldBeVisible;
        if (clipToPadding) {
            shouldBeVisible = x > leftGridOffset && x < leftGridOffset + gridWidth;
        } else {
            shouldBeVisible = x > 0 && x < getMeasuredWidth();
        }
        if (shouldBeVisible && hintState == HINT_INVISIBLE) {
            hintState = HINT_APPEARING;
        }
        if (!shouldBeVisible && hintState == HINT_VISIBLE) {
            hintState = HINT_DISAPPEARING;
        }

        float maxAvailableHintWidth = clipToPadding ? gridWidth : (getMeasuredWidth() - 4 * hintShadowRadius);
        float hintScale = Math.min(1.0f, Math.max(0.3f, maxAvailableHintWidth / hintBitmap.getWidth()));
        final float hintWidth = hintBitmap.getWidth() * hintScale;
        final float hintHeight = hintBitmap.getHeight() * hintScale;

        float screenPercentage = (gridToScreenX(selectedRow) - leftGridOffset) / gridWidth;
        float hintHorzOffset = 1f * hintScale * hintHorzPadding + hintScale * (hintWidth - 2 * hintHorzPadding) * screenPercentage;

        float hintLeft = x - hintHorzOffset;
        hintLeft = Math.max(hintLeft, clipToPadding ? leftGridOffset : 2 * hintScale * hintShadowRadius);
        float hintRight = hintLeft + hintWidth;
        hintRight = Math.min(hintRight, getMeasuredWidth() - (clipToPadding ? rightGridOffset : 2 * hintShadowRadius));
        hintLeft = hintRight - hintWidth;

        float hintTop = getPaddingTop();
        if (!clipToPadding) {
            for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
                for (int rowOffset = -3; rowOffset <= 3; ++rowOffset) {
                    if (selectedRow + rowOffset < 0 || selectedRow + rowOffset >= chartDataSource.getRowsCount()) {
                        continue;
                    }
                    float y = gridToScreenY(columnDataSource.getYAxis(), columnDataSource.getValue(selectedRow + rowOffset));
                    hintTop = Math.max(hintShadowRadius, Math.min(hintTop, y - hintHeight - 2 * hintShadowRadius));
                }
            }
        }
        float hintBottom = hintTop + hintHeight;

        if (hintBitmapDstRect.left != (int) hintLeft || hintBitmapDstRect.top != (int) hintTop
                || hintState == HINT_APPEARING || hintState == HINT_DISAPPEARING) {
            startHintAnimation(hintLeft, hintTop, hintRight, hintBottom);
        } else {
//            hintBitmapDstRect.left = (int) hintLeft;
//            hintBitmapDstRect.top = (int) hintTop;
            hintBitmapDstRect.right = (int) hintRight;
            hintBitmapDstRect.bottom = (int) hintBottom;
        }

        invalidate();
    }

    private void startHintAnimation(float hintLeft, float hintTop, float hintRight, float hintBottom) {
        if (hintAnimator != null) {
            hintAnimator.cancel();
        }
        hintAnimator = new ValueAnimator();
        PropertyValuesHolder alphaProperty = PropertyValuesHolder.ofInt("alpha",
                hintState == HINT_APPEARING ? 0 : hintState == HINT_DISAPPEARING ? 255 : 255,
                hintState == HINT_APPEARING ? 255 : hintState == HINT_DISAPPEARING ? 0 : 255);
        PropertyValuesHolder leftProperty = PropertyValuesHolder.ofInt("left",
                hintBitmapDstRect.left + (int) ((hintLeft - hintBitmapDstRect.left) * 0.2f), (int) hintLeft);
        PropertyValuesHolder topProperty = PropertyValuesHolder.ofInt("top",
                hintBitmapDstRect.top + (int) ((hintTop - hintBitmapDstRect.top) * 0.2f), (int) hintTop);
        hintAnimator.setValues(alphaProperty, leftProperty, topProperty);
        hintAnimator.setDuration(animationSpeed / 2);
        hintAnimator.setInterpolator(new DecelerateInterpolator());
        final boolean appearing = hintState == HINT_APPEARING;
        final boolean disappearing = hintState == HINT_DISAPPEARING;
        final boolean animatingAlpha = appearing || disappearing;
        hintAnimator.addListener(new EmptyAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (disappearing) {
                    hintState = HINT_INVISIBLE;
                    setSelectedRow(-1);
                } else if (appearing) {
                    hintState = HINT_VISIBLE;
                }
                hintAnimator = null;
            }

//            @Override
//            public void onAnimationCancel(Animator animation) {
//                hintAnimator = null;
//            }
        });
        if (hintState == HINT_APPEARING) {
            hintBitmapDstRect.left = (int) hintLeft;
            hintBitmapDstRect.top = (int) hintTop;
            hintBitmapDstRect.right = (int) hintRight;
            hintBitmapDstRect.bottom = (int) hintBottom;
        }
        final float hintWidth = hintRight - hintLeft;
        final float hintHeight = hintBottom - hintTop;
        hintAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (animatingAlpha) {
                    hintCopyPaint.setAlpha((int) animation.getAnimatedValue("alpha"));
                } else {
                    hintCopyPaint.setAlpha(255);
                    hintBitmapDstRect.left = (int) animation.getAnimatedValue("left");
                    hintBitmapDstRect.top = (int) animation.getAnimatedValue("top");
                    hintBitmapDstRect.right = (int) (hintBitmapDstRect.left + hintWidth);
                    hintBitmapDstRect.bottom = (int) (hintBitmapDstRect.top + hintHeight);
                }
                invalidate();
            }
        });
        hintAnimator.start();
    }

    private String formatGridValue(boolean left, float value) {
        // TODO: bad conversion from float back to long, the value might be broken
        if (left) {
            return yLeftColumnSource.formatValue((long) value, ValueFormatType.VERT_GRID);
        } else {
            return yRightColumnSource.formatValue((long) value, ValueFormatType.VERT_GRID);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (chartDataSource == null) {
            return;
        }

        if (clipToPadding) {
            canvas.save();
            canvas.clipRect(getPaddingLeft(), getPaddingTop(), getMeasuredWidth() - getPaddingRight(), getMeasuredHeight() - getPaddingBottom());
        }

        // draw vertical line for selected row
        if (selectedRow > -1) {
            float x = gridToScreenX(selectedRow);
            float lineTop = topGridOffset;
            if (hintState == HINT_VISIBLE) {
                lineTop = Math.min(lineTop, hintBitmapDstRect.bottom - 2 * hintShadowRadius);
            }
            canvas.drawLine(x, lineTop, x, topGridOffset + gridHeight, selectedLinePaint);
        }

        // draw vertical grid
        if (drawOldVertGrid) {
            oldVertGridPainter.draw(canvas);
        }
        vertGridPainter.draw(canvas);

        // draw lines
        for (int lineIdx = 0; lineIdx < visibleLineColumnSources.size(); ++lineIdx) {
            if (chartLineLengths[lineIdx] > 0) {
                canvas.drawLines(chartLines[lineIdx], 0, chartLineLengths[lineIdx], chartPaints[lineIdx]);
            }
        }

        // draw horizontal grid
        float horzGridTop = topGridOffset + gridHeight + getFontHeight(horzGridTextPaint);
        if (drawOldHorzGrid) {
            for (int c = 0; c < oldHorzGridRows.length; ++c) {
                oldHorzGridTextPaint.setTextAlign(oldHorzGridRows[c] == 0 ? Paint.Align.LEFT : Paint.Align.CENTER);
                canvas.drawText(oldHorzGridValues[c], gridToScreenX(oldHorzGridRows[c]), horzGridTop, oldHorzGridTextPaint);
            }
        }
        for (int c = 0; c < horzGridRows.length; ++c) {
            horzGridTextPaint.setTextAlign(horzGridRows[c] == 0 ? Paint.Align.LEFT : Paint.Align.CENTER);
            canvas.drawText(horzGridValues[c], gridToScreenX(horzGridRows[c]), horzGridTop, horzGridTextPaint);
        }

        // draw hint
        if (selectedRow > -1) {
            float x = gridToScreenX(selectedRow);
            if (x >= 0 && x <= getMeasuredWidth()) {
                int lineIdx = 0;
                for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
                    float y = gridToScreenY(columnDataSource.getYAxis(), columnDataSource.getValue(selectedRow));
                    selectedCircleFillPaint.setAlpha(chartPaints[lineIdx].getAlpha());
                    canvas.drawCircle(x, y, selectedCircleRadius, selectedCircleFillPaint);
                    canvas.drawCircle(x, y, selectedCircleRadius, chartPaints[lineIdx++]);
                }
            }
        }

        if (hintState != HINT_INVISIBLE) {
            canvas.drawBitmap(hintBitmap, hintBitmapSrcRect, hintBitmapDstRect, hintCopyPaint);
        }

        if (clipToPadding) {
            canvas.restore();
        }
    }

    @Override
    public void onRangeSelected(float startRow, float endRow) {
        setBounds(startRow, endRow, isDragging);
    }

    private void startRangeAnimation() {
        if (chartDataSource == null) {
            return;
        }
        calculateVertBounds();
        boolean reuseOldVertGrid = false;
        boolean reuseOldHorzGrid = false;
        if (chartAnimator != null) {
            boolean animationIsInEarlyStage = chartAnimator.getCurrentPlayTime() < animationSpeed / 2;
            reuseOldVertGrid = drawOldVertGrid && animationIsInEarlyStage;
            reuseOldHorzGrid = drawOldHorzGrid && animationIsInEarlyStage;
            chartAnimator.cancel();
        }
        if (!reuseOldVertGrid) {
            vertGridPainter.copyTo(oldVertGridPainter);
            vertGridPainter.init();
        }
        if (!reuseOldHorzGrid) {
            saveOldHorzGrid();
        }
        // draw old grids only if new grids are really different
        drawOldVertGrid = updateVertGrid(true);
        drawOldHorzGrid = updateHorzGrid();

        chartAnimator = new ValueAnimator();
        chartAnimator.setDuration(animationSpeed);
        PropertyValuesHolder oldGridAlphaProperty = PropertyValuesHolder.ofInt("oldGridAlpha", 255, 0);
        PropertyValuesHolder newGridAlphaProperty = PropertyValuesHolder.ofInt("newGridAlpha", 0, 255);
        PropertyValuesHolder topBoundProperty = PropertyValuesHolder.ofFloat("topBound",
                topBound + (calculatedTopBound - topBound) * 0.1f, calculatedTopBound);
        PropertyValuesHolder bottomBoundProperty = PropertyValuesHolder.ofFloat("bottomBound",
                bottomBound + (calculatedBottomBound - bottomBound) * 0.1f, calculatedBottomBound);
        chartAnimator.setValues(oldGridAlphaProperty, newGridAlphaProperty, topBoundProperty, bottomBoundProperty);
        chartAnimator.setInterpolator(new LinearInterpolator());
        chartAnimator.addListener(new EmptyAnimatorListener() {
            boolean canceled = false;

            @Override
            public void onAnimationEnd(Animator animation) {
                chartAnimator = null;
                drawOldVertGrid = false;
                drawOldHorzGrid = false;
                if (!canceled) {
                    updateChart(false);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
//                onAnimationEnd(animation);
            }
        });
        chartAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                int oldGridAlpha = (Integer) animator.getAnimatedValue("oldGridAlpha");
                int newGridAlpha = (Integer) animator.getAnimatedValue("newGridAlpha");
                if (drawOldVertGrid) {
                    oldVertGridPainter.setAlpha(oldGridAlpha);
                    vertGridPainter.setAlpha(newGridAlpha);
                } else {
                    oldVertGridPainter.setAlpha(255);
                    vertGridPainter.setAlpha(255);
                }
                if (drawOldHorzGrid) {
                    oldHorzGridTextPaint.setAlpha(oldGridAlpha);
                    horzGridTextPaint.setAlpha(newGridAlpha);
                } else {
                    oldHorzGridTextPaint.setAlpha(255);
                    horzGridTextPaint.setAlpha(255);
                }

                if (!topBoundFixed) {
                    topBound = (float) animator.getAnimatedValue("topBound");
                }
                if (!bottomBoundFixed) {
                    bottomBound = (float) animator.getAnimatedValue("bottomBound");
                }
                updateGridOffsets();
                updateChart(false);
            }
        });
        chartAnimator.start();
    }

    @Override
    public void onStartDragging() {
        isDragging = true;
    }

    @Override
    public void onStopDragging() {
        isDragging = false;
        invalidate(); // redraw with high quality
    }

}
