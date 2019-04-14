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
public class Data_3b {
    public static final SimpleChartDataSource chartDataSource = new SimpleChartDataSource(ChartType.BAR_STACK, false, Arrays.<ColumnDataSource>asList(
            new TimeColumnDataSource(ColumnType.X, "undefined", 0, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Apples", 0xFF3497ED, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Oranges", 0xFF2373DB, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Lemons", 0xFF9ED448, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Apricots", 0xFF5FB641, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Kiwi", 0xFFF5BD25, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Mango", 0xFFF79E39, new long[] {}),
            new IntegerColumnDataSource(ChartDataSource.YAxis.LEFT, ColumnType.LINE, "Pears", 0xFFE65850, new long[] {})
    ));
}
