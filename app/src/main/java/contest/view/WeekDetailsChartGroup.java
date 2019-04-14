package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import contest.datasource.BaseColumnDataSource;
import contest.datasource.ColumnDataSource;
import contest.utils.BinaryUtils;
import contest.utils.Constants;

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
        detailsChartGroup.getChartNavigationView().setSnapToXDistance(Constants.DAY_MS);
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
                selectedDayTime - 3 * Constants.DAY_MS, selectedDayTime + 4 * Constants.DAY_MS);
        for (int i = 0; i < detailsDataSource.getColumnsCount(); ++i) {
            ((BaseColumnDataSource) detailsDataSource.getColumn(i)).setValues(arrays[i]);
        }
        leftRow = floorIndexInArray(selectedDayTime, arrays[0]);
        rightRow = floorIndexInArray(selectedDayTime + Constants.DAY_MS, arrays[0]);
        if (leftRow == -1) {
            leftRow = 0;
        }
        if (rightRow == -1) {
            rightRow = arrays[0].length;
        }
        rightRow--;
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
