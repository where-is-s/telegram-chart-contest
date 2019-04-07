package contest.datasource;

/**
 * Created by Alex K on 19/03/2019.
 */
public interface ColumnDataSource {

    int getRowsCount();
    String getName();
    int getColor();
    long getValue(int row);
    long[] getValues();
    ColumnType getType();
    ChartDataSource.YAxis getYAxis();
    String formatValue(long value, ValueFormatType valueFormatType);

}
