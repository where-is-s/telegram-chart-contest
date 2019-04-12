package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.datasource.ValueFormatType;
import contest.utils.ChartUtils;
import contest.utils.GeneralUtils;
import contest.utils.SimpleAnimator;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartView extends View implements RangeListener {

    public static final float NO_BOUND = Float.NaN;

    private static final int HINT_INVISIBLE = 0;
    private static final int HINT_APPEARING = 1;
    private static final int HINT_VISIBLE = 2;
    private static final int HINT_DISAPPEARING = 3;

    private static final int ANIMATE_VALUE = 0;
    private static final int ANIMATE_OLD_ALPHA = 1;
    private static final int ANIMATE_NEW_ALPHA = 2;
    private static final int ANIMATE_TOP_BOUND = 3;
    private static final int ANIMATE_BOTTOM_BOUND = 4;
    private static final int ANIMATE_ALPHA = 5;
    private static final int ANIMATE_LEFT = 6;
    private static final int ANIMATE_TOP = 7;

    private ChartDataSource chartDataSource;
    private List<ColumnDataSource> visibleLineColumnSources = new ArrayList<>(); // includes animated column (appearing/disappearing)
    private ColumnDataSource xColumnSource;
    private ColumnDataSource yLeftColumnSource;
    private ColumnDataSource yRightColumnSource;
    private ColumnDataSource animatingColumn;
    private float animatingColumnOpacity; // 1f for opaque (alpha = 255), 0f for transparent (alpha = 0)

    private float chartLineWidth;
    private float vertGridLineInterval;
    private float horzGridValueInterval;
    private int selectedCircleRadius;
    private float gridLineWidth;
    private float hintVertPadding;
    private float hintHorzPadding;
    private float hintHorzMargin;
    private float hintPercentageOffset;
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

    private VertGridPainter vertGridPainter = new VertGridPainter();
    private HorzGridPainter horzGridPainter = new HorzGridPainter();
    private ChartPainter chartPainter;
    private ColumnType type;

    private Bitmap hintBitmap;
    private float calculatedHintWidth;
    private float calculatedHintHeight;
    private Rect hintBitmapSrcRect = new Rect();
    private Rect hintBitmapDstRect = new Rect();
    private int hintState = HINT_INVISIBLE;

    private float calculatedTopBound; // target bound for animations, calculated only for left Y axis
    private float calculatedBottomBound; // target bound for animations, calculated only for left Y axis
    private float rightYAxisMultiplier;

    private int selectedRow = -1;

    private Paint hintTitlePaint;
    private Paint hintBodyPaint;
    private Paint hintPercentagePaint;
    private Paint hintNamePaint;
    private Paint hintValuePaint;
    private Paint hintCopyPaint;

    private int selectedLineColor;
    private int selectedCircleFillColor;
    private float selectedLineWidth;

    private static abstract class ChartPainter {
        abstract void update();
        abstract void draw(Canvas canvas);
    }

    private class LineChartPainter extends ChartPainter {
        Paint chartPaints[] = new Paint[] {};
        float chartLines[][] = new float[][] {};
        int chartLinesLength;

        Paint selectedLinePaint;
        Paint selectedCircleFillPaint;

        LineChartPainter() {
            selectedLinePaint = new Paint();
            selectedLinePaint.setStyle(Paint.Style.STROKE);
            selectedCircleFillPaint = new Paint();
            selectedCircleFillPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void update() {
            int lefterBound = getLefterBound();
            int righterBound = getRighterBound();
            chartLinesLength = 4 * (righterBound - lefterBound);
            if (chartLinesLength < 0) {
                return;
            }
            if (chartLines.length < visibleLineColumnSources.size()) {
                chartLines = new float[visibleLineColumnSources.size()][];
                chartPaints = new Paint[visibleLineColumnSources.size()];
            }

            for (int i = 0; i < chartLines.length; ++i) {
                if (chartLines[i] == null || chartLines[i].length < chartLinesLength) {
                    chartLines[i] = new float[chartLinesLength];
                }
            }

            for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
                Paint paint = chartPaints[c];
                if (paint == null) {
                    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStrokeCap(Paint.Cap.BUTT);
                    paint.setStyle(Paint.Style.STROKE);
                }
                paint.setStrokeWidth(chartLineWidth);
                paint.setColor(visibleLineColumnSources.get(c).getColor());
                chartPaints[c] = paint;
            }

            int lineIdx = 0;
            float gridToScreenYFast = topGridOffset + gridHeight + bottomBound * gridStepY; // - gridY * gridStepY
            for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
                long valuesFast[] = columnDataSource.getValues();
                ChartDataSource.YAxis yAxis = columnDataSource.getYAxis();
                float multiplier = yAxis.equals(ChartDataSource.YAxis.RIGHT) ? rightYAxisMultiplier : 1f;
                float firstX = gridToScreenX(lefterBound);
                float currentLines[] = chartLines[lineIdx];
                for (int row = 0; row < righterBound - lefterBound; ++row) {
                    int offset = 4 * row;

                    currentLines[offset] = row == 0 ? firstX : currentLines[offset + 2 - 4];
                    currentLines[offset + 1] = row == 0 ? (gridToScreenYFast - gridStepY * (multiplier * valuesFast[row + lefterBound])) : currentLines[offset + 3 - 4];
                    currentLines[offset + 2] = firstX + (row + 1) * gridStepX;
                    currentLines[offset + 3] = gridToScreenYFast - gridStepY * (multiplier * valuesFast[row + lefterBound + 1]);
                }
                lineIdx++;
            }
        }

        @Override
        public void draw(Canvas canvas) {
            selectedLinePaint.setColor(selectedLineColor);
            selectedCircleFillPaint.setColor(selectedCircleFillColor);
            selectedLinePaint.setStrokeWidth(selectedLineWidth);

            // draw vertical line for selected row
            if (selectedRow > -1) {
                float x = gridToScreenX(selectedRow);
                float lineTop = topGridOffset;
//                if (hintState == HINT_VISIBLE) {
//                    lineTop = Math.min(lineTop, hintBitmapDstRect.bottom - 2 * hintShadowRadius);
//                }
                canvas.drawLine(x, lineTop, x, topGridOffset + gridHeight, selectedLinePaint);
            }

            // draw lines
            for (int lineIdx = 0; lineIdx < visibleLineColumnSources.size(); ++lineIdx) {
                canvas.drawLines(chartLines[lineIdx], 0, chartLinesLength, chartPaints[lineIdx]);
            }

            // draw selected circles
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
        }
    }

    private class BarStackPainter extends ChartPainter {
        Paint chartPaints[] = new Paint[] {};
        float chartLines[][] = new float[][] {};
        int chartLinesLength;

        public BarStackPainter() {
        }

        @Override
        public void update() {
            int lefterBound = getLefterBound();
            int righterBound = getRighterBound();
            chartLinesLength = 4 * (righterBound - lefterBound + 1);
            if (chartLinesLength < 0) {
                return;
            }
            if (chartLines.length < visibleLineColumnSources.size()) {
                chartLines = new float[visibleLineColumnSources.size()][];
                chartPaints = new Paint[visibleLineColumnSources.size()];
            }
            for (int i = 0; i < chartLines.length; ++i) {
                if (chartLines[i] == null || chartLines[i].length < chartLinesLength) {
                    chartLines[i] = new float[chartLinesLength];
                }
            }
            for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
                Paint paint = chartPaints[c];
                if (paint == null) {
                    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStrokeCap(Paint.Cap.BUTT);
                    paint.setStyle(Paint.Style.STROKE);
                }
                paint.setColor(visibleLineColumnSources.get(c).getColor());
                chartPaints[c] = paint;
            }
            int chartIdx = 0;
            float gridBottom = topGridOffset + gridHeight;
            float gridToScreenYFast = topGridOffset + gridHeight + bottomBound * gridStepY; // - gridY * gridStepY
            int animatingColumnIdx = visibleLineColumnSources.indexOf(animatingColumn);
            for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
                long valuesFast[] = columnDataSource.getValues();
                ChartDataSource.YAxis yAxis = columnDataSource.getYAxis();
                float multiplier = gridStepY * (yAxis.equals(ChartDataSource.YAxis.RIGHT) ? rightYAxisMultiplier : 1f);
                float firstX = gridToScreenX(lefterBound) + gridStepX / 2 - gridStepX * lefterBound / columnDataSource.getRowsCount();
                float fixedGridStepX = gridStepX - gridStepX * 1 / columnDataSource.getRowsCount();
                float currentLines[] = chartLines[chartIdx];
                chartPaints[chartIdx].setStrokeWidth(gridStepX + 1f);
                if (chartIdx == animatingColumnIdx) {
                    multiplier *= animatingColumnOpacity;
                }
                if (chartIdx == 0) {
                    for (int row = 0; row <= righterBound - lefterBound; ++row) {
                        int offset = 4 * row;
                        currentLines[offset] = row == 0 ? firstX : (currentLines[offset + 2 - 4] + fixedGridStepX);
                        currentLines[offset + 1] = gridToScreenYFast - multiplier * valuesFast[row + lefterBound];
                        currentLines[offset + 2] = currentLines[offset];
                        currentLines[offset + 3] = gridBottom;
                    }
                } else {
                    for (int row = 0; row <= righterBound - lefterBound; ++row) {
                        int offset = 4 * row;
                        currentLines[offset] = chartLines[chartIdx - 1][offset];
                        currentLines[offset + 1] = chartLines[chartIdx - 1][offset + 1] - multiplier * valuesFast[row + lefterBound];
                        currentLines[offset + 2] = chartLines[chartIdx - 1][offset];
                        currentLines[offset + 3] = chartLines[chartIdx - 1][offset + 1];
                    }
                }
                chartIdx++;
            }
        }

        @Override
        public void draw(Canvas canvas) {
            // draw lines
            for (int lineIdx = 0; lineIdx < visibleLineColumnSources.size(); ++lineIdx) {
                int color = 0;
                if (selectedRow > -1) {
                    color = chartPaints[lineIdx].getColor();
                    int backgroundColor = Color.WHITE; // TODO!
                    chartPaints[lineIdx].setColor(Color.rgb(
                            (Color.red(color) + Color.red(backgroundColor)) / 2,
                            (Color.green(color) + Color.green(backgroundColor)) / 2,
                            (Color.blue(color) + Color.blue(backgroundColor)) / 2
                    ));
                }
                chartPaints[lineIdx].setAlpha(animatingColumn == visibleLineColumnSources.get(lineIdx) ? (int) (animatingColumnOpacity * 255) : 255);
                canvas.drawLines(chartLines[lineIdx], 0, chartLinesLength, chartPaints[lineIdx]);
                if (selectedRow > -1) {
                    chartPaints[lineIdx].setColor(color);
                }
            }
            if (selectedRow > -1) {
                int selectedStartIdx = 4 * (selectedRow - getLefterBound());
                if (selectedStartIdx >= 0 && selectedStartIdx < chartLinesLength) {
                    for (int lineIdx = 0; lineIdx < visibleLineColumnSources.size(); ++lineIdx) {
                        chartPaints[lineIdx].setAlpha(animatingColumn == visibleLineColumnSources.get(lineIdx) ? (int) (animatingColumnOpacity * 255) : 255);
                        canvas.drawLines(chartLines[lineIdx], selectedStartIdx, 4, chartPaints[lineIdx]);
                    }
                }
            }
        }
    }

    private class PercentagePainter extends ChartPainter {
        Paint selectedLinePaint;
        Paint chartPaints[] = new Paint[] {};
        Path chartPaths[] = new Path[] {};
        int chartLinesLength;

        public PercentagePainter() {
            selectedLinePaint = new Paint();
            selectedLinePaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void update() {
            int lefterBound = getLefterBound();
            int righterBound = getRighterBound();
            chartLinesLength = 4 * (righterBound - lefterBound);
            if (chartLinesLength < 0) {
                return;
            }
            if (chartPaths.length < visibleLineColumnSources.size()) {
                chartPaths = new Path[visibleLineColumnSources.size()];
                chartPaints = new Paint[visibleLineColumnSources.size()];
            }
            for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
                Paint paint = chartPaints[c];
                if (paint == null) {
                    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.FILL);
                }
                paint.setColor(visibleLineColumnSources.get(c).getColor());
                chartPaints[c] = paint;
            }

            float gridBottom = topGridOffset + gridHeight;
            float gridToScreenYFast = topGridOffset + gridHeight + bottomBound * gridStepY; // - gridY * gridStepY

            long[][] fastValues = new long[visibleLineColumnSources.size()][];
            int i = 0;
            int animatingColumnIdx = -1;
            for (ColumnDataSource column: visibleLineColumnSources) {
                if (column == animatingColumn) {
                    animatingColumnIdx = i;
                }
                fastValues[i++] = column.getValues();
            }

            float firstX = gridToScreenX(lefterBound);
            for (int chartIdx = 0; chartIdx < fastValues.length; ++chartIdx) {
                Path currentPath = new Path();
                chartPaths[chartIdx] = currentPath;
                chartPaths[chartIdx].moveTo(firstX, gridBottom);
            }

            for (int row = 0; row <= righterBound - lefterBound; ++row) {
                float curValue = gridToScreenYFast;
                float total = 0f;
                for (int chartIdx = 0; chartIdx < fastValues.length; ++chartIdx) {
                    float opacityMultiplier = chartIdx != animatingColumnIdx ? 1f : animatingColumnOpacity;
                    total += fastValues[chartIdx][row + lefterBound] * opacityMultiplier;
                }
                for (int chartIdx = 0; chartIdx < fastValues.length; ++chartIdx) {
                    float opacityMultiplier = chartIdx != animatingColumnIdx ? 1f : animatingColumnOpacity;
                    curValue -= fastValues[chartIdx][row + lefterBound] * opacityMultiplier / total * gridHeight;
                    chartPaths[chartIdx].lineTo(firstX + row * gridStepX, curValue);
                }
            }

            for (int chartIdx = 0; chartIdx < fastValues.length; ++chartIdx) {
                chartPaths[chartIdx].lineTo(firstX + (righterBound - lefterBound) * gridStepX, gridBottom);
                chartPaths[chartIdx].close();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            for (int p = visibleLineColumnSources.size() - 1; p >= 0; --p) {
                canvas.drawPath(chartPaths[p], chartPaints[p]);
            }

            // draw vertical line for selected row
            if (selectedRow > -1) {
                selectedLinePaint.setColor(selectedLineColor);
                selectedLinePaint.setStrokeWidth(selectedLineWidth);
                float x = gridToScreenX(selectedRow);
                float lineTop = topGridOffset;
                canvas.drawLine(x, lineTop, x, topGridOffset + gridHeight, selectedLinePaint);
            }
        }
    }

    static class VertGridLine {
        int alphaLeft = 0;
        int destAlphaLeft = 0;
        int alphaRight = 0;
        int destAlphaRight = 0;
    }

    private class VertGridPainter extends SimpleAnimator.Listener {

        float valueSpacing;
        LongSparseArray<VertGridLine> lines = new LongSparseArray<>();

        SimpleAnimator animator = new SimpleAnimator();

        Paint linePaint;
        Paint textPaint;
        int textColor;

        VertGridPainter() {
            linePaint = new Paint();
            linePaint.setStyle(Paint.Style.STROKE);
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            animator.setListener(this);
            animator.setInterpolator(new DecelerateInterpolator());
        }

        void setLineColor(int gridLineColor) {
            linePaint.setColor(gridLineColor);
        }

        void setTextColor(int textColor) {
            this.textColor = textColor;
        }

        void setTextSize(float gridTextSize) {
            textPaint.setTextSize(gridTextSize);
        }

        void setStrokeWidth(float gridLineWidth) {
            linePaint.setStrokeWidth(gridLineWidth);
        }

        void animateFrame() {
            for (int i = 0; i < lines.size(); ++i) {
                VertGridLine line = lines.valueAt(i);
                line.alphaLeft = (int) (line.alphaLeft + (line.destAlphaLeft - line.alphaLeft) * 0.1f);
                line.alphaRight = (int) (line.alphaRight + (line.destAlphaRight - line.alphaRight) * 0.1f);
            }
            invalidate();
        }

        void animate() {
            animator.cancel();
            animator.setDuration(animationSpeed / 2);
            for (int i = 0; i < lines.size(); ++i) {
                VertGridLine line = lines.valueAt(i);
                if (line.alphaLeft == line.destAlphaLeft && line.alphaRight == line.destAlphaRight) {
                    continue;
                }
                animator.addValue(2 * i, (int) (line.alphaLeft + (line.destAlphaLeft - line.alphaLeft) * 0.1f), line.destAlphaLeft);
                animator.addValue(2 * i + 1, (int) (line.alphaRight + (line.destAlphaRight - line.alphaRight) * 0.1f), line.destAlphaRight);
            }
            animator.start();
        }

        @Override
        public void onUpdate() {
            for (int i = 0; i < lines.size(); ++i) {
                if (!animator.hasValue(2 * i)) {
                    continue;
                }
                lines.valueAt(i).alphaLeft = animator.getIntValue(2 * i);
                lines.valueAt(i).alphaRight = animator.getIntValue(2 * i + 1);
            }
            invalidate();
        }

        VertGridLine getLine(long value) {
            VertGridLine line = lines.get(value);
            if (line == null) {
                line = new VertGridLine();
                lines.put(value, line);
            }
            return line;
        }

        void update() {
            int linesCount = Math.max(2, (int) Math.floor(gridHeight / vertGridLineInterval));
            valueSpacing = (float) Math.floor((calculatedTopBound - calculatedBottomBound) / (linesCount - 1));
            valueSpacing = gridRound(valueSpacing);
            linesCount = (int) Math.floor((calculatedTopBound - calculatedBottomBound) / valueSpacing) + 1;
            boolean drawLeft = yRightColumnSource == null || (yLeftColumnSource != null && chartDataSource.isColumnVisible(yLeftColumnSource));
            boolean drawRight = yRightColumnSource != null && chartDataSource.isColumnVisible(yRightColumnSource);
            for (int i = 0; i < lines.size(); ++i) {
                VertGridLine line = lines.valueAt(i);
                line.destAlphaLeft = 0;
                line.destAlphaRight = 0;
            }
            for (int lineIdx = 0; lineIdx < linesCount; ++lineIdx) {
                VertGridLine line = getLine((long) (calculatedBottomBound + lineIdx * valueSpacing));
                line.destAlphaLeft = drawLeft ? 255 : 0;
                line.destAlphaRight = drawRight ? 255 : 0;
            }
            for (int i = 0; i < lines.size(); ++i) {
                VertGridLine line = lines.valueAt(i);
                if (line.destAlphaRight == 0 && line.alphaRight == 0 && line.destAlphaLeft == 0 && line.alphaLeft == 0) {
                    lines.removeAt(i);
                }
            }
            animator.cancel();
        }

        void draw(Canvas canvas) {
            float fontHeight = GeneralUtils.getFontHeight(textPaint);
            int viewWidth = getMeasuredWidth();

            for (int i = 0; i < lines.size(); ++i) {
                VertGridLine line = lines.valueAt(i);

                float x1 = leftGridOffset;
                float y = gridToScreenY(ChartDataSource.YAxis.LEFT, lines.keyAt(i));
                float x2 = viewWidth - rightGridOffset;

                int alpha = linePaint.getAlpha();
                linePaint.setAlpha(alpha * Math.max(line.alphaLeft, line.alphaRight) / 255);
                if (linePaint.getAlpha() != 0) {
                    canvas.drawLine(x1, y, x2, y, linePaint);
                }
                linePaint.setAlpha(alpha);
                if (line.alphaLeft != 0) {
                    textPaint.setTextAlign(Paint.Align.LEFT);
                    textPaint.setColor(yRightColumnSource == null ? textColor : yLeftColumnSource.getColor());
                    textPaint.setAlpha(line.alphaLeft);
                    canvas.drawText(formatGridValue(true, lines.keyAt(i)),
                            x1 + gridLineWidth,
                            y - fontHeight / 3,
                            textPaint);
                }
                if (line.alphaRight != 0) {
                    textPaint.setTextAlign(Paint.Align.RIGHT);
                    textPaint.setColor(yRightColumnSource.getColor());
                    textPaint.setAlpha(line.alphaRight);
                    canvas.drawText(formatGridValue(false, lines.keyAt(i) / rightYAxisMultiplier),
                            x2 - gridLineWidth,
                            y - fontHeight / 3,
                            textPaint);
                }
            }
        }

        void reset() {
            update();
            for (int i = 0; i < lines.size(); ++i) {
                VertGridLine line = lines.valueAt(i);
                line.alphaLeft = line.destAlphaLeft;
                line.alphaRight = line.destAlphaRight;
            }
        }
    }

    static class HorzGridLine {
        int alpha;
        int destAlpha;
        String value;
    }

    private class HorzGridPainter extends SimpleAnimator.Listener {
        LongSparseArray<HorzGridLine> lines = new LongSparseArray<>();

        SimpleAnimator animator = new SimpleAnimator();

        Paint textPaint;

        HorzGridPainter() {
            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextAlign(Paint.Align.CENTER);
            animator.setListener(this);
            animator.setInterpolator(new DecelerateInterpolator());
        }

        void setTextColor(int gridLineColor) {
            textPaint.setColor(gridLineColor);
        }

        void setTextSize(float gridTextSize) {
            textPaint.setTextSize(gridTextSize);
        }

        void animateFrame() {
            for (int i = 0; i < lines.size(); ++i) {
                HorzGridLine line = lines.valueAt(i);
                line.alpha = (int) (line.alpha + (line.destAlpha - line.alpha) * 0.1f);
            }
            invalidate();
        }

        void animate() {
            animator.cancel();
            animator.setDuration(animationSpeed / 2);
            for (int i = 0; i < lines.size(); ++i) {
                HorzGridLine line = lines.valueAt(i);
                if (line.alpha == line.destAlpha) {
                    continue;
                }
                animator.addValue(i, (int) (line.alpha + (line.destAlpha - line.alpha) * 0.1f), line.destAlpha);
            }
            animator.start();
        }

        @Override
        public void onUpdate() {
            for (int i = 0; i < lines.size(); ++i) {
                if (!animator.hasValue(i)) {
                    continue;
                }
                lines.valueAt(i).alpha = animator.getIntValue(i);
            }
            invalidate();
        }

        HorzGridLine getLine(long value) {
            HorzGridLine line = lines.get(value);
            if (line == null) {
                line = new HorzGridLine();
                lines.put(value, line);
            }
            return line;
        }

        void update() {
            if (getMeasuredWidth() == 0 || getMeasuredHeight() == 0) {
                return;
            }

            float markerSample1 = textPaint.measureText(xColumnSource.formatValue(xColumnSource.getValue(0), ValueFormatType.HORZ_GRID));
            float markerSample2 = textPaint.measureText(xColumnSource.formatValue(xColumnSource.getValue(chartDataSource.getRowsCount() - 1), ValueFormatType.HORZ_GRID));
            float fullXValueWidth = Math.max(markerSample1, markerSample2) + horzGridValueInterval;
            float xValuesPerScreen = (float) Math.floor(gridWidth / fullXValueWidth);
            float gridDistancePerMarker = (rightBound - leftBound) / xValuesPerScreen;
            double degree2 = Math.ceil(Math.log10(gridDistancePerMarker) / Math.log10(2));
            gridDistancePerMarker = (float) Math.pow(2, degree2);
            int rowsPerHorzMarker = (int) gridDistancePerMarker;

            if (rowsPerHorzMarker == 0) {
                return;
            }

            float textFitLeftBound = screenToGridX(0);
            float textFitRightBound = screenToGridX(getMeasuredWidth());
            int leftRow = (int) (Math.max(0, Math.floor(textFitLeftBound / rowsPerHorzMarker) * rowsPerHorzMarker));
            int rightRow = (int) (Math.min(chartDataSource.getRowsCount() - 1, Math.ceil(textFitRightBound / rowsPerHorzMarker) * rowsPerHorzMarker));
            int horzGridValuesCount = (rightRow - leftRow) / rowsPerHorzMarker + 1;

            for (int i = 0; i < lines.size(); ++i) {
                HorzGridLine line = lines.valueAt(i);
                line.destAlpha = 0;
            }
            for (int c = 0; c < horzGridValuesCount; ++c) {
                int row = leftRow + c * rowsPerHorzMarker;
                HorzGridLine line = getLine(row);
                line.value = xColumnSource.formatValue(xColumnSource.getValue(row), ValueFormatType.HORZ_GRID);
                line.destAlpha = 255;
            }
            for (int i = 0; i < lines.size(); ++i) {
                HorzGridLine line = lines.valueAt(i);
                if (line.destAlpha == 0 && line.alpha == 0) {
                    lines.removeAt(i);
                }
            }
            animator.cancel();
        }

        void draw(Canvas canvas) {
            // draw horizontal grid
            float horzGridTop = topGridOffset + gridHeight + GeneralUtils.getFontHeight(textPaint);
            for (int c = 0; c < lines.size(); ++c) {
                HorzGridLine line = lines.valueAt(c);
                long row = lines.keyAt(c);
                textPaint.setTextAlign(row == 0 ? Paint.Align.LEFT : Paint.Align.CENTER);
                textPaint.setAlpha(line.alpha);
                canvas.drawText(line.value, gridToScreenX(row), horzGridTop, textPaint);
            }
        }

        void reset() {
            update();
            for (int i = 0; i < lines.size(); ++i) {
                HorzGridLine line = lines.valueAt(i);
                line.alpha = line.destAlpha;
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
        private SimpleAnimator flingAnimator;

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
            flingAnimator = new SimpleAnimator();
            flingAnimator.addValue(ANIMATE_VALUE, 0, rows);
            flingAnimator.setDuration(500);
            flingAnimator.setInterpolator(new DecelerateInterpolator());
            final float initialLeftBound = leftBound;
            final float initialRightBound = rightBound;
            flingAnimator.setListener(new SimpleAnimator.Listener() {
                @Override
                public void onUpdate() {
                    float value = flingAnimator.getFloatValue(ANIMATE_VALUE);
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

    private Set<RangeListener> rangeListeners = new HashSet<>();

    private ChartDataSource.Listener chartDataSourceListener = new ChartDataSource.Listener() {
        @Override
        public void onSetColumnVisibility(int column, boolean visible) {
            final ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            animatingColumn = columnDataSource;
            float savedTopBound = topBound;
            float savedBottomBound = bottomBound;
            if (chartAnimator != null) {
                chartAnimator.cancel();
                animatingColumn = columnDataSource; // restore animating source because it is nulled on animation cancel
                updateColumns();
            }
            chartAnimator = new SimpleAnimator();
            chartAnimator.setDuration(animationSpeed);
            chartAnimator.addValue(ANIMATE_ALPHA, visible ? 0f : 1f, visible ? 1f : 0f);
            calculateVertBounds();
            chartAnimator.addValue(ANIMATE_TOP_BOUND, savedTopBound, calculatedTopBound);
            chartAnimator.addValue(ANIMATE_BOTTOM_BOUND, savedBottomBound, calculatedBottomBound);
            chartAnimator.setInterpolator(new DecelerateInterpolator());
            if (visible) {
                updateColumns();
            }
            vertGridPainter.update();
            horzGridPainter.update();
            chartAnimator.setListener(new SimpleAnimator.Listener() {
                public void onEnd() {
                    onCancel();
                    vertGridPainter.animate();
                    horzGridPainter.animate();
                }

                @Override
                public void onCancel() {
                    chartAnimator = null;
                    animatingColumn = null;
                    updateColumns(); // invisible column will finally be removed from visibleLineColumnSources
                    updateHint();
                    updateChart(true);
                }

                @Override
                public void onUpdate() {
                    vertGridPainter.animateFrame();
                    horzGridPainter.animateFrame();
                    animatingColumnOpacity = chartAnimator.getFloatValue(ANIMATE_ALPHA);
                    updateHint();
                    if (!topBoundFixed) {
                        topBound = chartAnimator.getFloatValue(ANIMATE_TOP_BOUND);
                    }
                    if (!bottomBoundFixed) {
                        bottomBound = chartAnimator.getFloatValue(ANIMATE_BOTTOM_BOUND);
                    }
                    updateGridOffsets();
                    updateChart(false);
                }
            });
            chartAnimator.start();
        }
    };

    private SimpleAnimator chartAnimator;
    private SimpleAnimator hintAnimator;
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
        hintTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hintTitlePaint.setTypeface(GeneralUtils.getBoldTypeface());
        hintTitlePaint.setStyle(Paint.Style.FILL);
        hintBodyPaint = new Paint();
        hintBodyPaint.setStyle(Paint.Style.FILL);
        hintNamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hintNamePaint.setStyle(Paint.Style.FILL);
        hintPercentagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hintPercentagePaint.setStyle(Paint.Style.FILL);
        hintPercentagePaint.setTypeface(GeneralUtils.getBoldTypeface());
        hintValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hintValuePaint.setStyle(Paint.Style.FILL);
        hintValuePaint.setTypeface(GeneralUtils.getMediumTypeface());
        hintValuePaint.setTextAlign(Paint.Align.RIGHT);
        hintCopyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        setType(ColumnType.BAR_STACK);
        setChartLineWidth(GeneralUtils.dp2px(getContext(), 2));
        setVertGridLineInterval(GeneralUtils.dp2px(getContext(), 40));
        setHorzGridValueInterval(GeneralUtils.dp2px(getContext(), 20));
        setGridLineColor(0x12182d3b);
        setGridTextColor(0x99a3b1c2);
        setGridTextSize(GeneralUtils.sp2px(getContext(), 12));
        setSelectedCircleRadius(GeneralUtils.dp2px(getContext(), 4));
        setSelectedCircleFillColor(Color.WHITE);
        setGridLineWidth(GeneralUtils.dp2px(getContext(), 1));
        setHintBackgroundColor(Color.WHITE);
        setHintTitleTextColor(0xff222222);
        setHintTitleTextSize(GeneralUtils.sp2px(getContext(), 13));
        setHintVertPadding(GeneralUtils.dp2px(getContext(), 6));
        setHintHorzPadding(GeneralUtils.dp2px(getContext(), 14));
        setHintHorzMargin(GeneralUtils.dp2px(getContext(), 20));
        setHintChartValueTextSize(GeneralUtils.sp2px(getContext(), 13));
        setClipToPadding(false);
        setFingerSize(GeneralUtils.dp2px(getContext(), 24));
        setAnimationSpeed(300);
        setHintShadowColor(0x20000000);
        setHintShadowRadius(GeneralUtils.dp2px(getContext(), 2));
        setHintBorderRadius(GeneralUtils.dp2px(getContext(), 4));
        setGesturesEnabled(true);
        updateGridOffsets();
    }

    public void addListener(RangeListener listener) {
        this.rangeListeners.add(listener);
    }

    public void removeListener(RangeListener listener) {
        this.rangeListeners.remove(listener);
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
        vertGridPainter.setLineColor(gridLineColor);
        selectedLineColor = gridLineColor;
        invalidate();
    }

    public void setGridTextColor(int gridTextColor) {
        vertGridPainter.setTextColor(gridTextColor);
        horzGridPainter.setTextColor(gridTextColor);
        updateChart(true);
    }

    public void setGridTextSize(float gridTextSize) {
        vertGridPainter.setTextSize(gridTextSize);
        horzGridPainter.setTextSize(gridTextSize);
        updateChart(true);
    }

    public void setSelectedCircleRadius(int selectedCircleRadius) {
        this.selectedCircleRadius = selectedCircleRadius;
        updateChart(true);
    }

    public void setSelectedCircleFillColor(int color) {
        selectedCircleFillColor = color;
        invalidate();
    }

    public void setGridLineWidth(float gridLineWidth) {
        this.gridLineWidth = gridLineWidth;
        vertGridPainter.setStrokeWidth(gridLineWidth);
        selectedLineWidth = gridLineWidth;
        invalidate();
    }

    public void setHintBackgroundColor(int color) {
        hintBodyPaint.setColor(color);
        drawHintBitmap();
    }

    public void setHintTitleTextColor(int hintTitleTextColor) {
        hintTitlePaint.setColor(hintTitleTextColor);
        hintNamePaint.setColor(hintTitleTextColor);
        hintPercentagePaint.setColor(hintTitleTextColor);
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
        hintNamePaint.setTextSize(hintChartValueTextSize);
        hintPercentagePaint.setTextSize(hintChartValueTextSize);
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

    public void setType(ColumnType type) {
        this.type = type;
        switch (type) {
            case LINE:
                chartPainter = new LineChartPainter();
                break;
            case BAR_STACK:
                chartPainter = new BarStackPainter();
                break;
            case PERCENTAGE:
                chartPainter = new PercentagePainter();
                break;
        }
        updateChart(true);
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
        chartDataSource.addListener(chartDataSourceListener);
        setBounds(0, chartDataSource.getRowsCount() - 1, false);
        updateChart(true);
    }

    public ChartDataSource getChartDataSource() {
        return chartDataSource;
    }

    private void updateColumns() {
        visibleLineColumnSources.clear();
        for (int column = 0; column < chartDataSource.getColumnsCount(); ++column) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            if (!chartDataSource.isColumnVisible(column) && animatingColumn != columnDataSource) {
                continue;
            }
            if (columnDataSource.getType().equals(ColumnType.LINE)) {
                visibleLineColumnSources.add(columnDataSource);
            }
        }
        xColumnSource = chartDataSource.getColumn(chartDataSource.getXAxisValueSourceColumn());
        yLeftColumnSource = chartDataSource.getColumn(chartDataSource.getYAxisValueSourceColumn(ChartDataSource.YAxis.LEFT));
        yRightColumnSource = chartDataSource.getColumn(chartDataSource.getYAxisValueSourceColumn(ChartDataSource.YAxis.RIGHT));
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
            return topGridOffset + gridHeight - (gridY * rightYAxisMultiplier - bottomBound) * gridStepY;
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

    private boolean isStacking() {
        return type.equals(ColumnType.BAR_STACK) || type.equals(ColumnType.PERCENTAGE);
    }

    private void calculateVertBounds() {
        if (type.equals(ColumnType.PERCENTAGE)) {
            calculatedBottomBound = 0f;
            calculatedTopBound = 100f;
            return;
        }
        ChartUtils.VertBounds vertBounds = ChartUtils.calculateVertBounds(
                chartDataSource, getLefterBound(), getRighterBound(), isStacking());
        calculatedTopBound = vertBounds.calculatedTopBound;
        calculatedBottomBound = vertBounds.calculatedBottomBound;
        if (bottomBoundFixed) {
            calculatedBottomBound = bottomBound;
        } else {
            vertGridPainter.update();
            float valueSpacing = vertGridPainter.valueSpacing;
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

    private void updateGridOffsets() {
        topGridOffset = getPaddingTop() + GeneralUtils.getFontHeight(vertGridPainter.textPaint);
        leftGridOffset = getPaddingLeft() + gridLineWidth / 2;
        rightGridOffset = getPaddingRight() + gridLineWidth / 2;
        bottomGridOffset = getPaddingBottom() + GeneralUtils.getFontHeight(horzGridPainter.textPaint);
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
        for (RangeListener rangeListener: rangeListeners) {
            rangeListener.onRangeSelected(leftBound, rightBound);
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

        chartPainter.update();

        if (reset) {
            vertGridPainter.update();
            horzGridPainter.update();
        }
        invalidate();
    }

    public void update() {
        updateColumns();
        updateHint();
        updateChart(true);
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
        vertGridPainter.reset();
        horzGridPainter.reset();
    }

    private void updateHint() {
        if (selectedRow < 0 || xColumnSource == null) {
            calculatedHintWidth = 0;
            calculatedHintHeight = 0;
            return;
        }

        float bodyWidth = hintTitlePaint.measureText(xColumnSource.formatValue(xColumnSource.getValue(selectedRow), ValueFormatType.HINT_TITLE));
        if (type.equals(ColumnType.PERCENTAGE)) {
            hintPercentageOffset = Math.max(GeneralUtils.dp2px(getContext(), 25), hintPercentagePaint.measureText("99%")) + hintHorzMargin / 3;
        } else {
            hintPercentageOffset = 0;
        }
        for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
            float lineHintWidth = hintPercentageOffset;
            lineHintWidth += hintNamePaint.measureText(columnDataSource.getName());
            lineHintWidth += hintHorzMargin;
            lineHintWidth += hintValuePaint.measureText(columnDataSource.formatValue(columnDataSource.getValue(selectedRow), ValueFormatType.HINT_VALUE));
            bodyWidth = Math.max(bodyWidth, lineHintWidth);
        }
        calculatedHintWidth = 4 * hintShadowRadius + 2 * hintHorzPadding + bodyWidth;
        calculatedHintHeight = 4 * hintShadowRadius + hintVertPadding + GeneralUtils.getFontHeight(hintTitlePaint) + hintVertPadding
                + (GeneralUtils.getFontHeight(hintValuePaint) + hintVertPadding) * visibleLineColumnSources.size() + hintVertPadding;
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
            hintBitmap = Bitmap.createBitmap((int) calculatedHintWidth, (int) calculatedHintHeight, Bitmap.Config.ARGB_8888);
            hintBitmapSrcRect.left = 0;
            hintBitmapSrcRect.top = 0;
            hintBitmapSrcRect.right = hintBitmap.getWidth();
            hintBitmapSrcRect.bottom = hintBitmap.getHeight();
        }

        Canvas canvas = new Canvas(hintBitmap);
        RectF hintRect = new RectF(hintShadowRadius * 2, hintShadowRadius * 2, calculatedHintWidth - hintShadowRadius * 2, calculatedHintHeight - hintShadowRadius * 2);
        canvas.drawRoundRect(hintRect, hintBorderRadius, hintBorderRadius, hintBodyPaint);
        canvas.drawText(xColumnSource.formatValue(xColumnSource.getValue(selectedRow), ValueFormatType.HINT_TITLE), hintRect.left + hintHorzPadding, hintRect.top + hintVertPadding + GeneralUtils.getFontHeight(hintTitlePaint), hintTitlePaint);
        float left = hintRect.left + hintHorzPadding;
        float right = hintRect.right - hintHorzPadding;
        float fontHeight = GeneralUtils.getFontHeight(hintValuePaint);
        float currentTop = hintRect.top + hintVertPadding + GeneralUtils.getFontHeight(hintTitlePaint) + hintVertPadding + fontHeight;

        float total = 0;
        if (type.equals(ColumnType.PERCENTAGE)) {
            for (ColumnDataSource columnDataSource : visibleLineColumnSources) {
                total += columnDataSource.getValue(selectedRow);
            }
        }

        for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
            float currentLeft = left;
            if (type.equals(ColumnType.PERCENTAGE)) {
                hintPercentagePaint.setAlpha(columnDataSource != animatingColumn ? 255 : (int) (255 * animatingColumnOpacity));
                canvas.drawText(String.format(Locale.getDefault(), "%d%%", (int) (columnDataSource.getValue(selectedRow) / total * 100)), left, currentTop, hintPercentagePaint);
                currentLeft += hintPercentageOffset;
            }

            hintNamePaint.setAlpha(columnDataSource != animatingColumn ? 255 : (int) (255 * animatingColumnOpacity));
            canvas.drawText(columnDataSource.getName(), currentLeft, currentTop, hintNamePaint);

            hintValuePaint.setColor(columnDataSource.getColor());
            hintValuePaint.setAlpha(columnDataSource != animatingColumn ? 255 : (int) (255 * animatingColumnOpacity));
            String value = columnDataSource.formatValue(columnDataSource.getValue(selectedRow), ValueFormatType.HINT_VALUE);
            canvas.drawText(value, right, currentTop, hintValuePaint);

            currentTop += fontHeight + hintVertPadding;
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

        final float hintWidth = hintBitmap.getWidth();
        final float hintHeight = hintBitmap.getHeight();

        float screenPercentage = (gridToScreenX(selectedRow) - leftGridOffset) / gridWidth;
        float hintHorzOffset = screenPercentage > 0.5f ? (- hintWidth - hintHorzPadding) : hintHorzPadding;

        float hintLeft = x + hintHorzOffset;
        hintLeft = Math.max(hintLeft, clipToPadding ? leftGridOffset : 2 * hintShadowRadius);
        float hintRight = hintLeft + hintWidth;
        hintRight = Math.min(hintRight, getMeasuredWidth() - (clipToPadding ? rightGridOffset : 2 * hintShadowRadius));
        hintLeft = hintRight - hintWidth;

        float hintTop = topGridOffset;
        if (!clipToPadding) {
            if (type.equals(ColumnType.PERCENTAGE)) {
                //
            } else if (isStacking()) {
                for (int rowOffset = -3; rowOffset <= 3; ++rowOffset) {
                    if (selectedRow + rowOffset < 0 || selectedRow + rowOffset >= chartDataSource.getRowsCount()) {
                        continue;
                    }
                    if (isStacking()) {
                        float stack = 0;
                        for (ColumnDataSource columnDataSource : visibleLineColumnSources) {
                            stack += columnDataSource.getValue(selectedRow + rowOffset);
                        }
                        float y = gridToScreenY(ChartDataSource.YAxis.NONE, stack);
                        hintTop = Math.max(hintShadowRadius, Math.min(hintTop, y - hintHeight - 2 * hintShadowRadius));
                    } else {
                        for (ColumnDataSource columnDataSource : visibleLineColumnSources) {
                            float y = gridToScreenY(columnDataSource.getYAxis(), columnDataSource.getValue(selectedRow + rowOffset));
                            hintTop = Math.max(hintShadowRadius, Math.min(hintTop, y - hintHeight - 2 * hintShadowRadius));
                        }
                    }
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
        hintAnimator = new SimpleAnimator();
        hintAnimator.addValue(ANIMATE_ALPHA,
                hintState == HINT_APPEARING ? 0 : hintState == HINT_DISAPPEARING ? 255 : 255,
                hintState == HINT_APPEARING ? 255 : hintState == HINT_DISAPPEARING ? 0 : 255);
        hintAnimator.addValue(ANIMATE_LEFT,
                hintBitmapDstRect.left + (int) ((hintLeft - hintBitmapDstRect.left) * 0.2f), (int) hintLeft);
        hintAnimator.addValue(ANIMATE_TOP,
                hintBitmapDstRect.top + (int) ((hintTop - hintBitmapDstRect.top) * 0.2f), (int) hintTop);
        hintAnimator.setDuration(animationSpeed / 2);
        hintAnimator.setInterpolator(new DecelerateInterpolator());
        final boolean appearing = hintState == HINT_APPEARING;
        final boolean disappearing = hintState == HINT_DISAPPEARING;
        final boolean animatingAlpha = appearing || disappearing;

        if (hintState == HINT_APPEARING) {
            hintBitmapDstRect.left = (int) hintLeft;
            hintBitmapDstRect.top = (int) hintTop;
            hintBitmapDstRect.right = (int) hintRight;
            hintBitmapDstRect.bottom = (int) hintBottom;
        }
        final float hintWidth = hintRight - hintLeft;
        final float hintHeight = hintBottom - hintTop;
        hintAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                if (disappearing) {
                    hintState = HINT_INVISIBLE;
                    setSelectedRow(-1);
                } else if (appearing) {
                    hintState = HINT_VISIBLE;
                }
                hintAnimator = null;
            }

            @Override
            public void onCancel() {
                onEnd();
            }

            @Override
            public void onUpdate() {
                if (animatingAlpha) {
                    hintCopyPaint.setAlpha(hintAnimator.getIntValue(ANIMATE_ALPHA));
                } else {
                    hintCopyPaint.setAlpha(255);
                    hintBitmapDstRect.left = hintAnimator.getIntValue(ANIMATE_LEFT);
                    hintBitmapDstRect.top = hintAnimator.getIntValue(ANIMATE_TOP);
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

        // draw main chart
        chartPainter.draw(canvas);

        // draw vertical grid
        vertGridPainter.draw(canvas);

        // draw horizontal grid
        horzGridPainter.draw(canvas);

        // draw hint
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
        if (chartAnimator != null) {
            chartAnimator.cancel();
        }
        vertGridPainter.update();
        horzGridPainter.update();

        chartAnimator = new SimpleAnimator();
        chartAnimator.setDuration(animationSpeed);
        chartAnimator.addValue(ANIMATE_OLD_ALPHA, 255, 0);
        chartAnimator.addValue(ANIMATE_NEW_ALPHA, 0, 255);
        chartAnimator.addValue(ANIMATE_TOP_BOUND,  topBound + (calculatedTopBound - topBound) * 0.1f, calculatedTopBound);
        chartAnimator.addValue(ANIMATE_BOTTOM_BOUND, bottomBound + (calculatedBottomBound - bottomBound) * 0.1f, calculatedBottomBound);
        chartAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                onCancel();
                updateChart(false);
                vertGridPainter.animate();
                horzGridPainter.animate();
            }

            @Override
            public void onCancel() {
                chartAnimator = null;
            }

            @Override
            public void onUpdate() {
                if (!topBoundFixed) {
                    topBound = chartAnimator.getFloatValue(ANIMATE_TOP_BOUND);
                }
                if (!bottomBoundFixed) {
                    bottomBound = chartAnimator.getFloatValue(ANIMATE_BOTTOM_BOUND);
                }
                vertGridPainter.animateFrame();
                horzGridPainter.animateFrame();
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

    public ColumnType getType() {
        return type;
    }
}
