package contest.datasource;

/**
 * Created by Alex K on 19/03/2019.
 */
public interface ColumnDataSource {

    int getRowsCount();
    String getName();
    int getColor();
    long getValue(int row);
    ColumnType getType();
    String formatValue(long value, ValueFormatType valueFormatType);

}
