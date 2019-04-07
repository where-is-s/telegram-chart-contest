package contest.datasource;

/**
 * Created by Alex K on 19/03/2019.
 */
public interface ChartDataSource {

    interface Listener {
        void onSetColumnVisibility(int column, boolean visible);
    }

    int getColumnsCount();
    int getRowsCount();
    ColumnDataSource getColumn(int column);

    int getXAxisValueSourceColumn();
    int getYAxisValueSourceColumn();

    boolean isColumnVisible(int column);
    void setColumnVisibility(int column, boolean visible);

    void addListener(Listener listener);
    void removeListener(Listener listener);

}
