package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import contest.datasource.BaseColumnDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.ValueFormatType;
import contest.utils.BinaryUtils;
import contest.utils.Constants;
import contest.utils.GeneralUtils;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ThreeDayDetailsChartGroup extends BaseDetailsChartGroup {

    public ThreeDayDetailsChartGroup(Context context) {
        super(context);
    }

    public ThreeDayDetailsChartGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThreeDayDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ThreeDayDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init() {
        super.init();
        mainChartGroup.getChartLegendView().setVisibility(GONE);
        detailsChartGroup.getChartNavigationView().setVisibility(GONE);
        mainChartGroup.setPadding(0, 0, 0, GeneralUtils.dp2px(getContext(), 16));
    }

    @Override
    public boolean isDetailsAvailable(int row) {
        return true;
    }

    @Override
    protected void prepareDetailsChartGroup(int row) {
        ColumnDataSource xColumn = mainDataSource.getColumn(mainDataSource.getXAxisValueSourceColumn());
        long selectedDayTime = xColumn.getValue(row);

        long wholeArrays[][] = BinaryUtils.readDataArrays(getContext(), assetBaseName, 2,
                selectedDayTime - 7 * Constants.DAY_MS, selectedDayTime + Constants.DAY_MS);
        long arrays[][] = new long[4][];

        int selectedDayLeft = floorIndexInArray(selectedDayTime, wholeArrays[0]);
        int selectedDayRight = floorIndexInArray(selectedDayTime + Constants.DAY_MS, wholeArrays[0]);
        if (selectedDayRight == -1 && selectedDayLeft != -1) {
            selectedDayRight = wholeArrays[0].length;
        }
        int length = selectedDayRight - selectedDayLeft;
        for (int i = 0; i < 4; ++i) {
            arrays[i] = new long[length];
        }
        System.arraycopy(wholeArrays[0], selectedDayLeft, arrays[0], 0, length);
        System.arraycopy(wholeArrays[1], selectedDayLeft, arrays[1], 0, length);

        int yesterdayLeft = floorIndexInArray(selectedDayTime - Constants.DAY_MS, wholeArrays[0]);
        if (yesterdayLeft != -1) {
            System.arraycopy(wholeArrays[1], yesterdayLeft, arrays[2], 0, length);
        }

        int lastWeekDayLeft = floorIndexInArray(selectedDayTime - 7 * Constants.DAY_MS, wholeArrays[0]);
        if (lastWeekDayLeft != -1) {
            System.arraycopy(wholeArrays[1], lastWeekDayLeft, arrays[3], 0, length);
        }

        detailsDataSource.setColumnVisibility(2, yesterdayLeft != -1);
        detailsDataSource.setColumnVisibility(3, lastWeekDayLeft != -1);

        long dayTimes[] = new long[] {0, selectedDayTime, selectedDayTime - Constants.DAY_MS, selectedDayTime - 7 * Constants.DAY_MS};
        for (int i = 0; i < detailsDataSource.getColumnsCount(); ++i) {
            BaseColumnDataSource column = (BaseColumnDataSource) detailsDataSource.getColumn(i);
            column.setValues(arrays[i]);
            column.setName((i > 0 && detailsDataSource.isColumnVisible(i)) ? xColumn.formatValue(dayTimes[i], ValueFormatType.LEGEND) : "");
        }

        detailsDataSource.updateRowsCount();
    }

}
