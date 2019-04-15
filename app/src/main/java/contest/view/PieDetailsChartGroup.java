package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

import contest.datasource.BaseColumnDataSource;
import contest.datasource.ColumnDataSource;
import contest.utils.Constants;
import contest.utils.EarlyLinearInterpolator;
import contest.utils.LateLinearInterpolator;
import contest.utils.SimpleAnimator;

/**
 * Created by Alex K on 19/03/2019.
 */
public class PieDetailsChartGroup extends BaseDetailsChartGroup {

    private static final int ANIMATE_DETAILS_SCALE = 10;
    private static final int ANIMATE_MAIN_SCALE = 11;

    public PieDetailsChartGroup(Context context) {
        super(context);
    }

    public PieDetailsChartGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PieDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init() {
        super.init();
        detailsChartGroup.getChartNavigationView().setSnapToXDistance(Constants.DAY_MS);
        detailsChartGroup.getChartNavigationView().setAllowResizeWindow(true);
        detailsChartGroup.setSingleRangeText(false);
    }

    @Override
    protected void prepareDetailsChartGroup(int row) {
        ColumnDataSource xColumn = mainDataSource.getColumn(mainDataSource.getXAxisValueSourceColumn());
        long selectedDayTime = xColumn.getValue(row);

        int sourceLeftRow = floorIndexInArray(selectedDayTime - 3 * Constants.DAY_MS, xColumn.getValues());
        int sourceRightRow = floorIndexInArray(selectedDayTime + 4 * Constants.DAY_MS, xColumn.getValues());

        if (sourceLeftRow == -1) {
            sourceLeftRow = 0;
        }
        if (sourceRightRow == -1) {
            sourceRightRow = mainDataSource.getRowsCount();
        }
        sourceRightRow--;

        int length = sourceRightRow - sourceLeftRow + 1;

        for (int i = 0; i < detailsDataSource.getColumnsCount(); ++i) {
            long valuesCopy[] = new long[length];
            System.arraycopy(mainDataSource.getColumn(i).getValues(), sourceLeftRow, valuesCopy, 0, length);
            ((BaseColumnDataSource) detailsDataSource.getColumn(i)).setValues(valuesCopy);
        }

        detailsDataSource.updateRowsCount();
    }

    protected void configureDetailsInAnimator(SimpleAnimator animator) {
    }

    protected void configureDetailsOutAnimator(SimpleAnimator animator) {
    }

    protected void handleDetailsClick(int row) {
        prepareDetailsChartGroup(row);

        detailsChartGroup.setChartDataSource(detailsDataSource);
        detailsChartGroup.getChartView().setSelectedItem(-1);
        detailsChartGroup.getChartLegendView().update();
        detailsChartGroup.getChartView().setBounds(detailsDataSource.getRowsCount() / 2, detailsDataSource.getRowsCount() / 2, true);
        detailsChartGroup.setVisibility(VISIBLE);

        activeAnimator = new SimpleAnimator();
        activeAnimator.addValue(ANIMATE_DETAILS_ALPHA, 0f, 1f, new LateLinearInterpolator(0.1f));
        activeAnimator.addValue(ANIMATE_DETAILS_SCALE, 0.5f, 1f, new LateLinearInterpolator(0.1f));
        activeAnimator.addValue(ANIMATE_MAIN_ALPHA, 1f, 0f, new EarlyLinearInterpolator(0.8f));
        activeAnimator.addValue(ANIMATE_MAIN_SCALE, 1f, 0.7f, new EarlyLinearInterpolator(0.8f));
        activeAnimator.setDuration(400);
        activeAnimator.setInterpolator(new DecelerateInterpolator());

        mainChartGroup.getChartView().setPivotX(mainChartGroup.getChartView().getWidth() / 2);
        mainChartGroup.getChartView().setPivotY(mainChartGroup.getChartView().getHeight() / 2);
        activeAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                activeAnimator = null;
            }

            @Override
            public void onUpdate() {
                detailsChartGroup.setAlpha(activeAnimator.getFloatValue(ANIMATE_DETAILS_ALPHA));
                detailsChartGroup.getChartView().setScaleX(activeAnimator.getFloatValue(ANIMATE_DETAILS_SCALE));
                detailsChartGroup.getChartView().setScaleY(activeAnimator.getFloatValue(ANIMATE_DETAILS_SCALE));
                mainChartGroup.setAlpha(activeAnimator.getFloatValue(ANIMATE_MAIN_ALPHA));
                mainChartGroup.getChartView().setScaleX(activeAnimator.getFloatValue(ANIMATE_MAIN_SCALE));
                mainChartGroup.getChartView().setScaleY(activeAnimator.getFloatValue(ANIMATE_MAIN_SCALE));
            }
        });
        activeAnimator.start();
    }

    protected void handleZoomOutClick() {
        mainChartGroup.setVisibility(VISIBLE);

        activeAnimator = new SimpleAnimator();
        activeAnimator.addValue(ANIMATE_DETAILS_ALPHA, 1f, 0f, new EarlyLinearInterpolator(0.9f));
        activeAnimator.addValue(ANIMATE_DETAILS_SCALE, 1f, 0.7f, new EarlyLinearInterpolator(0.9f));
        activeAnimator.addValue(ANIMATE_MAIN_ALPHA, 0f, 1f, new LateLinearInterpolator(0.2f));
        activeAnimator.addValue(ANIMATE_MAIN_SCALE, 0.5f, 1f, new LateLinearInterpolator(0.2f));
        activeAnimator.setDuration(400);
        activeAnimator.setInterpolator(new DecelerateInterpolator());

        activeAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                activeAnimator = null;
                detailsChartGroup.setVisibility(INVISIBLE);
            }

            @Override
            public void onUpdate() {
                detailsChartGroup.setAlpha(activeAnimator.getFloatValue(ANIMATE_DETAILS_ALPHA));
                detailsChartGroup.getChartView().setScaleX(activeAnimator.getFloatValue(ANIMATE_DETAILS_SCALE));
                detailsChartGroup.getChartView().setScaleY(activeAnimator.getFloatValue(ANIMATE_DETAILS_SCALE));
                mainChartGroup.setAlpha(activeAnimator.getFloatValue(ANIMATE_MAIN_ALPHA));
                mainChartGroup.getChartView().setScaleX(activeAnimator.getFloatValue(ANIMATE_MAIN_SCALE));
                mainChartGroup.getChartView().setScaleY(activeAnimator.getFloatValue(ANIMATE_MAIN_SCALE));
            }
        });
        activeAnimator.start();
    }
}
