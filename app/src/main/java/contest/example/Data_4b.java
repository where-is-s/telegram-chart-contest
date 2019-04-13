package contest.example;

import java.util.Arrays;

import contest.datasource.ChartDataSource;
import contest.datasource.ChartType;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.datasource.DateColumnDataSource;
import contest.datasource.IntegerColumnDataSource;
import contest.datasource.SimpleChartDataSource;

/**
 * Generated automatically
 */
public class Data_4b {
    public static final SimpleChartDataSource chartDataSource = new SimpleChartDataSource(ChartType.LINE, false, Arrays.<ColumnDataSource>asList(
            new DateColumnDataSource(ColumnType.X, "undefined", 0, null),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Views", 0xFF64ADED, null)
    ));
}
