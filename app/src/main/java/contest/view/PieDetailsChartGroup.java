package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

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

    @Override
    protected float getLeftDetailsRowToAnimateTo() {
        return detailsDataSource.getRowsCount() / 2;
    }

    @Override
    protected float getRightDetailsRowToAnimateTo() {
        return detailsDataSource.getRowsCount() / 2;
    }

    @Override
    protected void configureDetailsInAnimator(SimpleAnimator simpleAnimator) {
        simpleAnimator.addValue(ANIMATE_DETAILS_ALPHA, 0f, 1f, new LateLinearInterpolator(0.3f));
        simpleAnimator.addValue(ANIMATE_MAIN_ALPHA, 1f, 0f, new EarlyLinearInterpolator(0.6f));
    }

    @Override
    protected void configureDetailsOutAnimator(SimpleAnimator simpleAnimator) {
        simpleAnimator.addValue(ANIMATE_DETAILS_ALPHA, 0f, 1f, new LateLinearInterpolator(0.3f));
        simpleAnimator.addValue(ANIMATE_MAIN_ALPHA, 1f, 0f, new EarlyLinearInterpolator(0.6f));
    }
}
