package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import contest.datasource.BaseColumnDataSource;
import contest.datasource.ColumnDataSource;
import contest.utils.BinaryUtils;

/**
 * Created by Alex K on 19/03/2019.
 */
public class WeekDetailsChartGroup extends BaseDetailsChartGroup {

    private int leftRow;
    private int rightRow;

    public WeekDetailsChartGroup(Context context) {
        super(context);
    }

    public WeekDetailsChartGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WeekDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WeekDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init() {
        super.init();
        detailsChartGroup.getChartNavigationView().setAllowResizeWindow(false);
        detailsChartGroup.getChartNavigationView().setSnapToXDistance(24 * 3600 * 1000L);
    }

    @Override
    public boolean isDetailsAvailable(int row) {
        return true;
    }

    @Override
    protected void prepareDetailsChartGroup(int row) {
        ColumnDataSource xColumn = mainDataSource.getColumn(mainDataSource.getXAxisValueSourceColumn());
        long selectedDayTime = xColumn.getValue(row);

        long arrays[][] = BinaryUtils.readDataArrays(getContext(), assetBaseName, detailsDataSource.getColumnsCount(),
                selectedDayTime - 3 * 24 * 3600 * 1000L, selectedDayTime + 4 * 24 * 3600 * 1000L);
        for (int i = 0; i < detailsDataSource.getColumnsCount(); ++i) {
            ((BaseColumnDataSource) detailsDataSource.getColumn(i)).setValues(arrays[i]);
        }
        int leftRow = -1;
        int rightRow = -1;
        long dateArray[] = arrays[0];
        for (int i = 0; i < dateArray.length; ++i) {
            if (dateArray[i] >= selectedDayTime && leftRow == -1) {
                leftRow = i;
            }
            if (dateArray[i] >= selectedDayTime + 24 * 3600 * 1000L && rightRow == -1) {
                rightRow = i - 1;
            }
        }
        if (leftRow == -1) {
            this.leftRow = 0;
        }
        if (rightRow == -1) {
            this.rightRow = dateArray.length - 1;
        }
        detailsDataSource.updateRowsCount();
    }

    @Override
    protected float getLeftDetailsRowToAnimateTo() {
        return leftRow;
    }

    @Override
    protected float getRightDetailsRowToAnimateTo() {
        return rightRow;
    }
}
