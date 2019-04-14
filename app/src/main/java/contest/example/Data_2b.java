package contest.example;

import java.util.Arrays;

import contest.datasource.ChartDataSource;
import contest.datasource.ChartType;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.datasource.IntegerColumnDataSource;
import contest.datasource.SimpleChartDataSource;
import contest.datasource.TimeColumnDataSource;

/**
 * Generated automatically
 */
public class Data_2b {
    public static final SimpleChartDataSource chartDataSource = new SimpleChartDataSource(ChartType.LINE, true, Arrays.<ColumnDataSource>asList(
            new TimeColumnDataSource(ColumnType.X, "", 0, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Views", 0xFF108BE3, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.RIGHT, ColumnType.LINE, "Shares", 0xFFE8AF14, new long[] {})
    ));
}
