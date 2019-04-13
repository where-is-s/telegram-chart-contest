package contest.datasource;

/**
 * Created by Alex K on 19/03/2019.
 */
public interface ChartDataSource {

    enum YAxis {
        NONE, LEFT, RIGHT
    }

    interface Listener {
        void onSetColumnVisibility(int column, boolean visible);
    }

    ChartType getChartType();

    int getColumnsCount();
    int getRowsCount();
    ColumnDataSource getColumn(int column);

    int getXAxisValueSourceColumn();
    int getYAxisValueSourceColumn(YAxis yAxis);

    float getRightYAxisMultiplier();

    boolean isColumnVisible(int column);
    boolean isColumnVisible(ColumnDataSource columnDataSource);

    void setColumnVisibility(int column, boolean visible);

    void addListener(Listener listener);
    void removeListener(Listener listener);

}
