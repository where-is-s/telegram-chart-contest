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
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import contest.datasource.ChartDataSource;
import contest.datasource.ChartType;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.datasource.ValueFormatType;
import contest.utils.ChartUtils;
import contest.utils.GeneralUtils;
import contest.utils.LateLinearInterpolator;
import contest.utils.SimpleAnimator;
import telegram.contest.chart.R;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartView extends View implements RangeListener {

    interface DetailsListener {
        boolean isDetailsAvailable(int row);
        void onDetailsClick(int row);
    }

    public static final float NO_BOUND = Float.NaN;

    static final int HINT_INVISIBLE = 0;
    static final int HINT_APPEARING = 1;
    static final int HINT_VISIBLE = 2;
    static final int HINT_DISAPPEARING = 3;

    static final int ANIMATE_VALUE = 0;
    static final int ANIMATE_OLD_ALPHA = 1;
    static final int ANIMATE_NEW_ALPHA = 2;
    static final int ANIMATE_TOP_BOUND = 3;
    static final int ANIMATE_BOTTOM_BOUND = 4;
    static final int ANIMATE_ALPHA = 5;
    static final int ANIMATE_LEFT = 6;
    static final int ANIMATE_TOP = 7;

    ChartDataSource chartDataSource;
    List<ColumnDataSource> visibleLineColumnSources = new ArrayList<>(); // includes animated column (appearing/disappearing)
    ColumnDataSource xColumnSource;
    ColumnDataSource yLeftColumnSource;
    ColumnDataSource yRightColumnSource;
    ColumnDataSource animatingColumn;
    float animatingColumnOpacity; // 1f for opaque (alpha = 255), 0f for transparent (alpha = 0)
    Interpolator hintColumnAlphaInterpolator = new LateLinearInterpolator(0.5f);

    float chartLineWidth;
    float vertGridLineInterval;
    float horzGridValueInterval;
    int selectedCircleRadius;
    float gridLineWidth;
    float hintVertPadding;
    float hintHorzPadding;
    float hintHorzMargin;
    float hintPercentageOffset;
    long hintPieValue;
    boolean clipToPadding;
    float fingerSize;
    int animationSpeed;
    int hintShadowColor;
    float hintShadowRadius;
    int hintBorderRadius;
    boolean gesturesEnabled;
    int windowSizeMinRows = 10;

    float leftBound;
    float rightBound;
    float topBound; // calculated only for left Y axis
    float bottomBound; // calculated only for left Y axis
    boolean leftBoundFixed = false;
    boolean rightBoundFixed = false;
    boolean topBoundFixed = false;
    boolean bottomBoundFixed = false;

    float topGridOffset;
    float bottomGridOffset;
    float leftGridOffset;
    float rightGridOffset;
    float gridWidth;
    float gridHeight;
    float gridStepX;
    float gridStepY;

    VertGridPainter vertGridPainter = new VertGridPainter();
    HorzGridPainter horzGridPainter = new HorzGridPainter();
    ChartPainter chartPainter;

    Bitmap hintBitmap;
    float calculatedHintWidth;
    float calculatedHintHeight;
    Rect hintBitmapSrcRect = new Rect();
    Rect hintBitmapDstRect = new Rect();
    int hintState = HINT_INVISIBLE;
    Bitmap hintDetailsBitmap;

    float calculatedTopBound; // target bound for animations, calculated only for left Y axis
    float calculatedBottomBound; // target bound for animations, calculated only for left Y axis
    float rightYAxisMultiplier;

    int selectedItem = -1;

    Paint hintTitlePaint;
    Paint hintBodyPaint;
    Paint hintPercentagePaint;
    Paint hintNamePaint;
    Paint hintValuePaint;
    Paint hintCopyPaint;

    int selectedLineColor;
    int selectedCircleFillColor;
    float selectedLineWidth;

    interface ChartPainter {
        void update();
        void draw(Canvas canvas);
        void selectNearest(float screenX, float screenY, float searchSize);
    }

    class PieChartPainter implements ChartPainter {

        float chartValues[] = new float[] {};
        Paint chartPaints[] = new Paint[] {};
        RectF drawRect = new RectF();
        Rect textRect = new Rect();
        Paint valuePaint;
        float minTextSizeSp = 16;
        float maxTextSizeSp = 36;
        float suggestedHintX;
        float suggestedHintY;

        PieChartPainter() {
            valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            valuePaint.setStyle(Paint.Style.FILL);
            valuePaint.setColor(Color.WHITE);
            valuePaint.setTypeface(GeneralUtils.getMediumTypeface());
        }

        @Override
        public void update() {
            int lefterBound = getLefterBound();
            int righterBound = getRighterBound();
            if (chartValues.length < visibleLineColumnSources.size()) {
                chartValues = new float[visibleLineColumnSources.size()];
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

            int lineIdx = 0;
            float totalValue = 0;
            for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
                long valuesFast[] = columnDataSource.getValues();
                float multiplier = 1f;
                if (columnDataSource == animatingColumn) {
                    multiplier = animatingColumnOpacity;
                }
                for (int row = 0; row < righterBound - lefterBound; ++row) {
                    chartValues[lineIdx] += valuesFast[row + lefterBound] * multiplier;
                    totalValue += valuesFast[row + lefterBound] * multiplier;
                }
                lineIdx++;
            }

            for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
                chartValues[c] /= totalValue;
                chartValues[c] = Math.max(0f, Math.min(1f, chartValues[c]));
            }
        }

        void drawWedgeCaption(Canvas canvas, boolean selected, int item, int percentage, float currentAngle, float sweepAngle) {
            float centerX = leftGridOffset + gridWidth / 2;
            float centerY = topGridOffset + gridHeight / 2;
            float baseRadius = Math.min(centerX, centerY) * 0.8f;

            drawRect.left = centerX - baseRadius;
            drawRect.top = centerY - baseRadius;
            drawRect.right = centerX + baseRadius;
            drawRect.bottom = centerY + baseRadius;
            float midAngle = (float) Math.toRadians(currentAngle + sweepAngle / 2);
            float valueDistance = baseRadius * Math.max(0.4f, 0.7f - 0.3f * sweepAngle / 180);
            if (sweepAngle < 360) {
                if (selected) {
                    float dp8 = GeneralUtils.dp2px(getContext(), 8);
                    valueDistance += dp8;
                    drawRect.offset((float) (dp8 * Math.cos(midAngle)), (float) (dp8 * Math.sin(midAngle)));
                }
            } else {
                valueDistance = 0;
            }
            if (canvas != null) {
                canvas.drawArc(
                        drawRect,
                        currentAngle,
                        sweepAngle + (item == selectedItem || item == selectedItem - 1 ? 0f : 1f),
                        true,
                        chartPaints[item]
                );
            }
            float fontHeight = GeneralUtils.sp2px(getContext(), minTextSizeSp + (maxTextSizeSp - minTextSizeSp) * (sweepAngle - 5) / 360);
            valuePaint.setTextSize(fontHeight);
            float valueX = (float) (centerX + valueDistance * Math.cos(midAngle));
            float valueY = (float) (centerY + valueDistance * Math.sin(midAngle));
            String text = String.format(Locale.getDefault(), "%d%%", percentage);
            valuePaint.getTextBounds(text, 0, text.length(), textRect);
            textRect.offset((int) valueX - textRect.centerX(), (int) valueY - textRect.centerY());
            if (canvas != null && sweepAngle > 3) {
                canvas.drawText(text, textRect.left, textRect.bottom, valuePaint);
            }
            if (selected) {
                suggestedHintX = textRect.centerX();
                if (currentAngle + sweepAngle / 2 >= 360) {
                    suggestedHintY = textRect.bottom + GeneralUtils.dp2px(getContext(), 36);
                } else {
                    suggestedHintY = textRect.top - GeneralUtils.dp2px(getContext(), 36);
                }
            }
        }

        @Override
        public void draw(Canvas canvas) {
            float currentAngle = 180;
            for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
                float sweepAngle = 360 * chartValues[c];
                int percentage = (int) (100 * chartValues[c]);
                drawWedgeCaption(canvas, c == selectedItem, c, percentage, currentAngle, sweepAngle);
                currentAngle += sweepAngle;
            }
        }

        @Override
        public void selectNearest(float screenX, float screenY, float searchSize) {
            float centerX = leftGridOffset + gridWidth / 2;
            float centerY = topGridOffset + gridHeight / 2;
            if (screenY == centerY && screenX == centerX) {
                return;
            }
            float touchDegrees = (float) Math.toDegrees(Math.atan2(screenY - centerY, screenX - centerX)) + 360f;

            float currentAngle = 180;
            for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
                float sweepAngle = 360 * chartValues[c];
                if (touchDegrees > currentAngle && touchDegrees < currentAngle + sweepAngle) {
                    drawWedgeCaption(null, true, c, 100, currentAngle, sweepAngle);
                    setSelectedItem(c);
                    break;
                }
                currentAngle += sweepAngle;
            }
        }

        public void updateSuggestedHintPosition() {
            // TODO refactor this method, try to remove duplication with selectNearest
            float currentAngle = 180;
            for (int c = 0; c < visibleLineColumnSources.size(); ++c) {
                float sweepAngle = 360 * chartValues[c];
                if (c == selectedItem) {
                    // draw with null canvas -> just update suggested hint position
                    drawWedgeCaption(null, true, c, 100, currentAngle, sweepAngle);
                    break;
                }
                currentAngle += sweepAngle;
            }
        }
    }

    class LineChartPainter implements ChartPainter {
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
            if (selectedItem > -1) {
                float x = gridToScreenX(selectedItem);
                float lineTop = topGridOffset;
//                if (hintState == HINT_VISIBLE) {
//                    lineTop = Math.min(lineTop, hintBitmapDstRect.bottom - 2 * hintShadowRadius);
//                }
                canvas.drawLine(x, lineTop, x, topGridOffset + gridHeight, selectedLinePaint);
            }

            // draw lines
            for (int lineIdx = 0; lineIdx < visibleLineColumnSources.size(); ++lineIdx) {
                if (visibleLineColumnSources.get(lineIdx) == animatingColumn) {
                    chartPaints[lineIdx].setAlpha((int) (animatingColumnOpacity * 255));
                } else {
                    chartPaints[lineIdx].setAlpha(255);
                }

                canvas.drawLines(chartLines[lineIdx], 0, chartLinesLength, chartPaints[lineIdx]);
            }

            // draw selected circles
            if (selectedItem > -1) {
                float x = gridToScreenX(selectedItem);
                if (x >= 0 && x <= getMeasuredWidth()) {
                    int lineIdx = 0;
                    for (ColumnDataSource columnDataSource: visibleLineColumnSources) {
                        float y = gridToScreenY(columnDataSource.getYAxis(), columnDataSource.getValue(selectedItem));
                        selectedCircleFillPaint.setAlpha(chartPaints[lineIdx].getAlpha());
                        canvas.drawCircle(x, y, selectedCircleRadius, selectedCircleFillPaint);
                        canvas.drawCircle(x, y, selectedCircleRadius, chartPaints[lineIdx++]);
                    }
                }
            }
        }

        @Override
        public void selectNearest(float screenX, float screenY, float searchSize) {
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
            setSelectedItem(bestRow);
        }
    }

    class BarStackPainter implements ChartPainter {
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
                if (selectedItem > -1) {
                    color = chartPaints[lineIdx].getColor();
                    int backgroundColor = Color.WHITE; // TODO!
                    chartPaints[lineIdx].setColor(Color.rgb(
                            (Color.red(color) + Color.red(backgroundColor)) / 2,
                            (Color.green(color) + Color.green(backgroundColor)) / 2,
                            (Color.blue(color) + Color.blue(backgroundColor)) / 2
                    ));
                }
                canvas.drawLines(chartLines[lineIdx], 0, chartLinesLength, chartPaints[lineIdx]);
                if (selectedItem > -1) {
                    chartPaints[lineIdx].setColor(color);
                }
            }
            if (selectedItem > -1) {
                int selectedStartIdx = 4 * (selectedItem - getLefterBound());
                if (selectedStartIdx >= 0 && selectedStartIdx < chartLinesLength) {
                    for (int lineIdx = 0; lineIdx < visibleLineColumnSources.size(); ++lineIdx) {
                        canvas.drawLines(chartLines[lineIdx], selectedStartIdx, 4, chartPaints[lineIdx]);
                    }
                }
            }
        }

        @Override
        public void selectNearest(float screenX, float screenY, float searchSize) {
            int row = (int) screenToGridX(screenX);
            if (row >= 0 && row < chartDataSource.getRowsCount()) {
                setSelectedItem(row); // TODO: select nearest visible?
            }
        }
    }

    class PercentagePainter implements ChartPainter {
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
                chartPaths[c] = new Path();
                chartPaths[c].incReserve(righterBound - lefterBound + 10);
            }

            float gridBottom = topGridOffset + gridHeight;
            float gridToScreenYFast = topGridOffset + gridHeight + bottomBound * gridStepY; // - gridY * gridStepY

            long fastValues[][] = new long[visibleLineColumnSources.size()][];
            float bottoms[] = new float[visibleLineColumnSources.size()]; // reducing size filled area allows to speed up drawPath
            int i = 0;
            int animatingColumnIdx = -1;
            for (ColumnDataSource column: visibleLineColumnSources) {
                if (column == animatingColumn) {
                    animatingColumnIdx = i;
                }
                fastValues[i++] = column.getValues();
            }

            float firstX = gridToScreenX(lefterBound);
            // last path is not never needed actually, it can be replaced with rectangle
            for (int chartIdx = 0; chartIdx < fastValues.length - 1; ++chartIdx) {
                chartPaths[chartIdx].reset();
            }

            for (int row = 0; row <= righterBound - lefterBound; ++row) {
                float curValue = gridToScreenYFast;
                float total = 0f;
                for (int chartIdx = 0; chartIdx < fastValues.length; ++chartIdx) {
                    float opacityMultiplier = chartIdx != animatingColumnIdx ? 1f : animatingColumnOpacity;
                    total += fastValues[chartIdx][row + lefterBound] * opacityMultiplier;
                }
                for (int chartIdx = 0; chartIdx < fastValues.length - 1; ++chartIdx) {
                    float opacityMultiplier = chartIdx != animatingColumnIdx ? 1f : animatingColumnOpacity;
                    curValue -= fastValues[chartIdx][row + lefterBound] * opacityMultiplier / total * gridHeight;
                    if (row == 0) {
                        chartPaths[chartIdx].moveTo(firstX, curValue);
                    } else {
                        chartPaths[chartIdx].lineTo(firstX + row * gridStepX, curValue);
                    }
                    if (bottoms[chartIdx] < curValue) {
                        bottoms[chartIdx] = curValue;
                    }
                }
            }

            for (int chartIdx = 0; chartIdx < fastValues.length - 1; ++chartIdx) {
                float bottom = chartIdx == 0 ? gridBottom : bottoms[chartIdx - 1];
                chartPaths[chartIdx].lineTo(firstX + (righterBound - lefterBound) * gridStepX, bottom);
                chartPaths[chartIdx].lineTo(firstX, bottom);
                chartPaths[chartIdx].close();
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (visibleLineColumnSources.isEmpty()) {
                return;
            }
            canvas.drawRect(gridToScreenX(getLefterBound()), topGridOffset, gridToScreenX(getRighterBound()),
                    topGridOffset + gridHeight, chartPaints[visibleLineColumnSources.size() - 1]);
            for (int p = visibleLineColumnSources.size() - 2; p >= 0; --p) {
                canvas.drawPath(chartPaths[p], chartPaints[p]);
            }

            // draw vertical line for selected row
            if (selectedItem > -1) {
                selectedLinePaint.setColor(selectedLineColor);
                selectedLinePaint.setStrokeWidth(selectedLineWidth);
                float x = gridToScreenX(selectedItem);
                float lineTop = topGridOffset;
                canvas.drawLine(x, lineTop, x, topGridOffset + gridHeight, selectedLinePaint);
            }
        }

        @Override
        public void selectNearest(float screenX, float screenY, float searchSize) {
            int row = (int) screenToGridX(screenX);
            if (row >= 0 && row < chartDataSource.getRowsCount()) {
                setSelectedItem(row);
            }
        }
    }

    static class VertGridLine {
        int alphaLeft = 0;
        int destAlphaLeft = 0;
        int alphaRight = 0;
        int destAlphaRight = 0;
    }

    class VertGridPainter extends SimpleAnimator.Listener {

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
            if (isChartType(ChartType.PIE) || chartDataSource == null) {
                return;
            }

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
            if (isChartType(ChartType.PIE)) {
                return;
            }

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

    class HorzGridPainter extends SimpleAnimator.Listener {
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
            if (isChartType(ChartType.PIE) || chartDataSource == null) {
                return;
            }

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
            if (isChartType(ChartType.PIE)) {
                return;
            }

            // draw horizontal grid
            float horzGridTop = topGridOffset + gridHeight + GeneralUtils.getFontHeight(textPaint);
            for (int c = 0; c < lines.size(); ++c) {
                HorzGridLine line = lines.valueAt(c);
                long row = lines.keyAt(c);
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

    float touchBeginX;
    ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
        float oldFocusX;

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (!gesturesEnabled) {
                return false;
            }
            if (isChartType(ChartType.PIE)) {
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
            if (isChartType(ChartType.PIE)) {
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
    GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.OnGestureListener() {
        SimpleAnimator flingAnimator;

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
                if (detailsListener != null && detailsListener.isDetailsAvailable(selectedItem)) {
                    detailsListener.onDetailsClick(selectedItem);
                } else {
                    setSelectedItem(-1);
                    hintState = HINT_DISAPPEARING;
                    startHintAnimation(
                            hintBitmapDstRect.left,
                            hintBitmapDstRect.top,
                            hintBitmapDstRect.right,
                            hintBitmapDstRect.bottom
                    );
                }
            } else {
                chartPainter.selectNearest(event.getX(), event.getY(), fingerSize);
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (isChartType(ChartType.PIE)) {
                return false;
            }
            getParent().requestDisallowInterceptTouchEvent(true);
            if (!gesturesEnabled) {
                chartPainter.selectNearest(e2.getX(), e2.getY(), 0);
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
            if (isChartType(ChartType.PIE)) {
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

    Set<RangeListener> rangeListeners = new HashSet<>();
    DetailsListener detailsListener = new DetailsListener() {
        @Override
        public boolean isDetailsAvailable(int row) {
            return false;
        }

        @Override
        public void onDetailsClick(int row) {
        }
    };

    ChartDataSource.Listener chartDataSourceListener = new ChartDataSource.Listener() {
        @Override
        public void onSetColumnVisibility(int column, final boolean visible) {
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
                    // invisible column will now finally be removed from visibleLineColumnSources
                    updateColumns();
                    updateChart(true);
                    updateHint();
                }

                @Override
                public void onUpdate() {
                    vertGridPainter.animateFrame();
                    horzGridPainter.animateFrame();
                    animatingColumnOpacity = chartAnimator.getFloatValue(ANIMATE_ALPHA);
                    if (!topBoundFixed) {
                        topBound = chartAnimator.getFloatValue(ANIMATE_TOP_BOUND);
                    }
                    if (!bottomBoundFixed) {
                        bottomBound = chartAnimator.getFloatValue(ANIMATE_BOTTOM_BOUND);
                    }
                    updateGridOffsets();
                    updateChart(false);
                    updateHint();
                }
            });
            chartAnimator.start();
        }
    };

    SimpleAnimator chartAnimator;
    SimpleAnimator hintAnimator;
    boolean isDragging;

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

    void init() {
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
        hintDetailsBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.details)).getBitmap();

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
        setHintShadowColor(0x30000000);
        setHintShadowRadius(GeneralUtils.dp2px(getContext(), 2.5f));
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

    public int getAnimationSpeed() {
        return animationSpeed;
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

    float sqr(float val) {
        return val * val;
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
        this.topBoundFixed = !Float.isNaN(bound);
        if (this.topBoundFixed) {
            this.topBound = bound;
        }
        updateChart(true);
    }

    public void setBottomBound(float bound) {
        this.bottomBoundFixed = !Float.isNaN(bound);
        if (this.bottomBoundFixed) {
            this.bottomBound = bound;
        }
        updateChart(true);
    }

    public void setChartDataSource(final ChartDataSource chartDataSource) {
        if (this.chartDataSource != null) {
            this.chartDataSource.removeListener(chartDataSourceListener);
        }
        this.chartDataSource = chartDataSource;
        switch (chartDataSource.getChartType()) {
            case LINE:
                chartPainter = new LineChartPainter();
                break;
            case BAR_STACK:
                chartPainter = new BarStackPainter();
                break;
            case PERCENTAGE:
                chartPainter = new PercentagePainter();
                break;
            case PIE:
                chartPainter = new PieChartPainter();
                break;
        }
        updateColumns();
        updateChart(true);
        rightYAxisMultiplier = chartDataSource.getRightYAxisMultiplier();
        chartDataSource.addListener(chartDataSourceListener);
        setBounds(0, chartDataSource.getRowsCount() - 1, false);
        updateChart(true);
    }

    public ChartDataSource getChartDataSource() {
        return chartDataSource;
    }

    void updateColumns() {
        ColumnDataSource restoreSelectedColumn = null;
        if (isChartType(ChartType.PIE) && selectedItem > -1) {
            restoreSelectedColumn = visibleLineColumnSources.get(selectedItem);
        }

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

        if (isChartType(ChartType.PIE) && restoreSelectedColumn != null) {
            selectedItem = visibleLineColumnSources.indexOf(restoreSelectedColumn);
        }
        invalidate();
    }

    float gridToScreenX(float gridX) {
        return leftGridOffset + (gridX - leftBound) * gridStepX;
    }

    float gridToScreenY(ChartDataSource.YAxis yAxis, float gridY) {
        if (yAxis.equals(ChartDataSource.YAxis.LEFT)) {
            return topGridOffset + gridHeight - (gridY - bottomBound) * gridStepY;
        } else {
            return topGridOffset + gridHeight - (gridY * rightYAxisMultiplier - bottomBound) * gridStepY;
        }
    }

    float screenToGridX(float screenX) {
        float x = screenX - leftGridOffset;
        float valuesCount = (rightBound - leftBound);
        if (valuesCount == 0) {
            return leftBound + valuesCount / 2;
        }
        return leftBound + valuesCount * x / gridWidth;
    }

    float screenToGridY(float screenY) {
        float localY = bottomGridOffset - screenY;
        float valuesCount = (topBound - bottomBound);
        if (valuesCount == 0) {
            return bottomBound + valuesCount / 2;
        }
        return bottomBound + valuesCount * localY / gridHeight;
    }

    int getLefterBound() {
        return (int) Math.max(0, screenToGridX(clipToPadding ? leftGridOffset : 0));
    }

    int getRighterBound() {
        return (int) Math.min(chartDataSource.getRowsCount() - 1, screenToGridX(getMeasuredWidth() - (clipToPadding ? rightGridOffset : 0)) + 1);
    }

    boolean isStacking() {
        return isChartType(ChartType.BAR_STACK) || isChartType(ChartType.PERCENTAGE);
    }

    void calculateVertBounds() {
        if (isChartType(ChartType.PERCENTAGE)) {
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

    void updateGridOffsets() {
        topGridOffset = getPaddingTop() + GeneralUtils.getFontHeight(vertGridPainter.textPaint);
        leftGridOffset = getPaddingLeft() + gridLineWidth / 2;
        rightGridOffset = getPaddingRight() + gridLineWidth / 2;
        bottomGridOffset = getPaddingBottom() + GeneralUtils.getFontHeight(horzGridPainter.textPaint);
        gridWidth = getMeasuredWidth() - leftGridOffset - rightGridOffset;
        gridHeight = getMeasuredHeight() - topGridOffset - bottomGridOffset;
        float valuesCount = (rightBound - leftBound);
        gridStepX = valuesCount <= 1 ? 0 : (gridWidth / valuesCount);
        valuesCount = (topBound - bottomBound);
        gridStepY = valuesCount <= 1 ? 0 : (gridHeight / valuesCount);
    }

    void setBounds(float left, float right, boolean animation) {
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
        for (RangeListener rangeListener : rangeListeners) {
            rangeListener.onRangeSelected(leftBound, rightBound);
        }
        updateGridOffsets();
        if (animation) {
            startRangeAnimation();
            if (isChartType(ChartType.PIE)) {
                updateHint();
            } else {
                placeHint();
            }
        }
    }

    int updatesPerSecond = 0;
    long lastSecond = System.currentTimeMillis();

    void updateChart(boolean reset) {
        ++updatesPerSecond;
        if (System.currentTimeMillis() > lastSecond + 1000) {
            Log.i("ZZZ", "Updates/s: " + updatesPerSecond);
            updatesPerSecond = 0;
            lastSecond = System.currentTimeMillis();
        }

        if (getMeasuredWidth() == 0 || getMeasuredHeight() == 0 || chartDataSource == null) {
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
        updateChart(true);
        updateHint();
    }

    float gridRound(float value) {
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

    public void setSelectedItem(int selectedItem) {
        this.selectedItem = selectedItem;
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

    void updateHint() {
        if (selectedItem < 0 || xColumnSource == null) {
            if (isChartType(ChartType.PIE) && hintState != HINT_INVISIBLE) {
                placeHint();
                return;
            }
            calculatedHintWidth = 0;
            calculatedHintHeight = 0;
            hintState = HINT_INVISIBLE;
            return;
        }

        float bodyWidth;
        float bodyHeight;
        if (isChartType(ChartType.PIE)) {
            ColumnDataSource selectedColumn = visibleLineColumnSources.get(selectedItem);
            bodyWidth = hintNamePaint.measureText(selectedColumn.getName());
            int lefterBound = getLefterBound();
            int righterBound = getRighterBound();
            long sum = 0;
            long valuesFast[] = selectedColumn.getValues();
            for (int r = lefterBound; r < righterBound; ++r) {
                sum += valuesFast[r];
            }
            hintPieValue = sum;
            bodyWidth += hintValuePaint.measureText(selectedColumn.formatValue(hintPieValue, ValueFormatType.HINT_VALUE));
            bodyWidth += 2 * hintHorzMargin;
            bodyWidth = Math.min(bodyWidth, GeneralUtils.dp2px(getContext(), 120));
            bodyHeight = GeneralUtils.getFontHeight(hintValuePaint) + hintVertPadding;
        } else {
            bodyWidth = hintTitlePaint.measureText(xColumnSource.formatValue(xColumnSource.getValue(selectedItem), ValueFormatType.HINT_TITLE));
            if (detailsListener != null && detailsListener.isDetailsAvailable(selectedItem)) {
                bodyWidth += hintHorzMargin + hintDetailsBitmap.getWidth();
            }
            if (isChartType(ChartType.PERCENTAGE)) {
                hintPercentageOffset = Math.max(GeneralUtils.dp2px(getContext(), 25), hintPercentagePaint.measureText("99%")) + hintHorzMargin / 3;
            } else {
                hintPercentageOffset = 0;
            }
            for (ColumnDataSource columnDataSource : visibleLineColumnSources) {
                float lineHintWidth = hintPercentageOffset;
                lineHintWidth += hintNamePaint.measureText(columnDataSource.getName());
                lineHintWidth += hintHorzMargin;
                lineHintWidth += hintValuePaint.measureText(columnDataSource.formatValue(columnDataSource.getValue(selectedItem), ValueFormatType.HINT_VALUE));
                bodyWidth = Math.max(bodyWidth, lineHintWidth);
            }
            bodyHeight = GeneralUtils.getFontHeight(hintTitlePaint) + hintVertPadding
                    + (GeneralUtils.getFontHeight(hintValuePaint) + hintVertPadding) * visibleLineColumnSources.size();
            if (animatingColumn != null) {
                bodyHeight -= (GeneralUtils.getFontHeight(hintValuePaint) + hintVertPadding) * (1f - animatingColumnOpacity);
            }
        }
        calculatedHintWidth = 4 * hintShadowRadius + 2 * hintHorzPadding + bodyWidth;
        calculatedHintHeight = 4 * hintShadowRadius + 2 * hintVertPadding + bodyHeight;
        drawHintBitmap();
        placeHint();
    }

    void drawHintBitmap() {
        if (hintBitmap != null && (hintBitmap.getWidth() != calculatedHintWidth || hintBitmap.getHeight() != calculatedHintHeight)) {
            hintBitmap.recycle();
            hintBitmap = null;
        }

        if (selectedItem < 0) {
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

        float left = hintRect.left + hintHorzPadding;
        float right = hintRect.right - hintHorzPadding;

        if (isChartType(ChartType.PIE)) {
            ColumnDataSource selectedColumn = visibleLineColumnSources.get(selectedItem);
            canvas.drawText(selectedColumn.getName(), left, hintRect.top + hintVertPadding + GeneralUtils.getFontHeight(hintNamePaint), hintNamePaint);
            hintValuePaint.setColor(selectedColumn.getColor());
            canvas.drawText(selectedColumn.formatValue(hintPieValue, ValueFormatType.HINT_VALUE), right, hintRect.top + hintVertPadding + GeneralUtils.getFontHeight(hintValuePaint), hintValuePaint);
        } else {
            float currentTop = hintRect.top + hintVertPadding + GeneralUtils.getFontHeight(hintTitlePaint);
            String title = xColumnSource.formatValue(xColumnSource.getValue(selectedItem), ValueFormatType.HINT_TITLE);
            Rect textRect = new Rect();
            hintTitlePaint.getTextBounds(title, 0, title.length(), textRect);
            canvas.drawText(title, hintRect.left + hintHorzPadding, currentTop, hintTitlePaint);


            if (detailsListener != null && detailsListener.isDetailsAvailable(selectedItem)) {
                canvas.drawBitmap(hintDetailsBitmap, right - hintDetailsBitmap.getWidth(), currentTop + textRect.centerY() - hintDetailsBitmap.getHeight() * 0.5f, null);
            }

            float fontHeight = GeneralUtils.getFontHeight(hintValuePaint);
            currentTop += hintVertPadding + fontHeight;

            float total = 0;
            if (isChartType(ChartType.PERCENTAGE)) {
                for (ColumnDataSource columnDataSource : visibleLineColumnSources) {
                    total += columnDataSource.getValue(selectedItem);
                }
            }

            for (ColumnDataSource columnDataSource : visibleLineColumnSources) {
                float currentLeft = left;
                if (isChartType(ChartType.PERCENTAGE)) {
                    hintPercentagePaint.setAlpha(columnDataSource != animatingColumn ? 255 : (int) (255 * animatingColumnOpacity));
                    canvas.drawText(String.format(Locale.getDefault(), "%d%%", (int) (columnDataSource.getValue(selectedItem) / total * 100)), left, currentTop, hintPercentagePaint);
                    currentLeft += hintPercentageOffset;
                }

                int alpha = columnDataSource != animatingColumn ? 255
                        : (int) (255 * hintColumnAlphaInterpolator.getInterpolation(animatingColumnOpacity));
                hintNamePaint.setAlpha(alpha);
                canvas.drawText(columnDataSource.getName(), currentLeft, currentTop, hintNamePaint);

                hintValuePaint.setColor(columnDataSource.getColor());
                hintValuePaint.setAlpha(alpha);
                String value = columnDataSource.formatValue(columnDataSource.getValue(selectedItem), ValueFormatType.HINT_VALUE);
                canvas.drawText(value, right, currentTop, hintValuePaint);

                if (columnDataSource != animatingColumn) {
                    currentTop += fontHeight + hintVertPadding;
                } else {
                    currentTop += animatingColumnOpacity * (fontHeight + hintVertPadding);
                }
            }
        }

        invalidate();
    }

    void placeHint() {
        if (selectedItem < 0 && !isChartType(ChartType.PIE)) {
            return;
        }
        if (hintState == HINT_DISAPPEARING || hintState == HINT_APPEARING) { // already animating to appear/disappear
            return;
        }
        float hintLeft;
        float hintRight;
        float hintTop;
        float hintBottom;

        final float hintWidth = hintBitmap.getWidth();
        final float hintHeight = hintBitmap.getHeight();

        if (isChartType(ChartType.PIE)) {
            ((PieChartPainter) chartPainter).updateSuggestedHintPosition();

            hintLeft = ((PieChartPainter) chartPainter).suggestedHintX - hintWidth / 2;
            hintTop = ((PieChartPainter) chartPainter).suggestedHintY - hintHeight / 2;
            if (selectedItem > -1 && hintState == HINT_INVISIBLE) {
                hintState = HINT_APPEARING;
            }
            if (selectedItem == -1 && hintState == HINT_VISIBLE) {
                hintState = HINT_DISAPPEARING;
            }
        } else {
            float x = gridToScreenX(selectedItem);
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

            float screenPercentage = (gridToScreenX(selectedItem) - leftGridOffset) / gridWidth;
            float hintHorzOffset = screenPercentage > 0.5f ? (- hintWidth - hintHorzPadding) : hintHorzPadding;

            hintLeft = x + hintHorzOffset;
            hintTop = topGridOffset;
        }

        hintLeft = Math.max(hintLeft, clipToPadding ? leftGridOffset : 2 * hintShadowRadius);
        hintRight = hintLeft + hintWidth;
        hintRight = Math.min(hintRight, getMeasuredWidth() - (clipToPadding ? rightGridOffset : 2 * hintShadowRadius));
        hintLeft = hintRight - hintWidth;

        if (!clipToPadding && isStacking()) {
            for (int rowOffset = -3; rowOffset <= 3; ++rowOffset) {
                if (selectedItem + rowOffset < 0 || selectedItem + rowOffset >= chartDataSource.getRowsCount()) {
                    continue;
                }
                if (isStacking()) {
                    float stack = 0;
                    for (ColumnDataSource columnDataSource : visibleLineColumnSources) {
                        stack += columnDataSource.getValue(selectedItem + rowOffset);
                    }
                    float valueY = gridToScreenY(ChartDataSource.YAxis.NONE, stack);
                    hintTop = Math.max(hintShadowRadius, Math.min(hintTop, valueY - hintHeight - 2 * hintShadowRadius));
                } else {
                    for (ColumnDataSource columnDataSource : visibleLineColumnSources) {
                        float valueY = gridToScreenY(columnDataSource.getYAxis(), columnDataSource.getValue(selectedItem + rowOffset));
                        hintTop = Math.max(hintShadowRadius, Math.min(hintTop, valueY - hintHeight - 2 * hintShadowRadius));
                    }
                }
            }
        }
        hintBottom = hintTop + hintHeight;

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

    void startHintAnimation(float hintLeft, float hintTop, float hintRight, float hintBottom) {
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
                    setSelectedItem(-1);
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

    String formatGridValue(boolean left, float value) {
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

        // draw vertical grid if needed
        vertGridPainter.draw(canvas);

        // draw horizontal grid if needed
        horzGridPainter.draw(canvas);

        // draw hint
        if (hintState != HINT_INVISIBLE) {
            canvas.drawBitmap(hintBitmap, hintBitmapDstRect.left, hintBitmapDstRect.top, hintCopyPaint);
        }

        if (clipToPadding) {
            canvas.restore();
        }
    }

    @Override
    public void onRangeSelected(float startRow, float endRow) {
        setBounds(startRow, endRow, isDragging);
    }

    void startRangeAnimation() {
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

    boolean isChartType(ChartType chartType) {
        return chartDataSource != null && chartDataSource.getChartType().equals(chartType);
    }
    
    public void setDetailsListener(DetailsListener detailsListener) {
        this.detailsListener = detailsListener;
    }

}
