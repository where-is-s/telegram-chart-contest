package contest.utils;

import java.util.ArrayList;
import java.util.List;

import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;

/**
 * Created by Alex Kravchenko on 12/04/2019.
 */
public class ChartUtils {

    public static class VertBounds {
        public float calculatedBottomBound;
        public float calculatedTopBound;
    }

    public static VertBounds calculateVertBounds(ChartDataSource chartDataSource, int lefterBound, int righterBound, boolean stacking) {
        float calculatedBottomBound = stacking ? 0 : Float.MAX_VALUE;
        float calculatedTopBound = stacking ? 0 : Float.MIN_VALUE;
        float rightYAxisMultiplier = chartDataSource.getRightYAxisMultiplier();
        List<long[]> fastValuesList = new ArrayList<>();
        ArrayList<Float> multiplierList = new ArrayList<>();
        for (int column = 0; column < chartDataSource.getColumnsCount(); ++column) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(column);
            if (columnDataSource.getType().equals(ColumnType.X)
                    || !chartDataSource.isColumnVisible(column)) {
                continue;
            }
            fastValuesList.add(columnDataSource.getValues());
            ChartDataSource.YAxis yAxis = columnDataSource.getYAxis();
            float multiplier = yAxis.equals(ChartDataSource.YAxis.RIGHT) ? rightYAxisMultiplier : 1f;
            multiplierList.add(multiplier);
        }
        long[][] fastValues = new long[fastValuesList.size()][];
        for (int i = 0; i < fastValuesList.size(); ++i) {
            fastValues[i] = fastValuesList.get(i);
        }
        Float multipliers[] = multiplierList.toArray(new Float[multiplierList.size()]);
        for (int row = lefterBound; row <= righterBound; ++row) {
            float aggValue = 0;
            for (int c = 0; c < fastValues.length; ++c) {
                long value = fastValues[c][row];
                value *= multipliers[c];
                if (stacking) {
                    aggValue += value;
                    continue;
                }
                if (calculatedBottomBound > value) {
                    calculatedBottomBound = value;
                }
                if (calculatedTopBound < value) {
                    calculatedTopBound = value;
                }
            }
            if (stacking && calculatedTopBound < aggValue) {
                calculatedTopBound = aggValue;
            }
        }
        if (stacking) {
            calculatedBottomBound = 0;
        }
        VertBounds bounds = new VertBounds();
        bounds.calculatedTopBound = calculatedTopBound;
        bounds.calculatedBottomBound = calculatedBottomBound;
        return bounds;
    }

}
