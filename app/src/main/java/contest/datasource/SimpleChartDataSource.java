package contest.datasource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Alex K on 19/03/2019.
 */
public class SimpleChartDataSource implements ChartDataSource {

    private final ChartType chartType;
    private final boolean doubleAxis;
    private final List<ColumnDataSource> columns = new ArrayList<>();
    private final Set<ColumnDataSource> invisibleColumns = new HashSet<>();
    private final Set<Listener> listeners = new HashSet<>();
    private int rowsCount = 0;
    private float rightYAxisMultiplier = 1f;

    public SimpleChartDataSource(ChartType chartType, boolean doubleAxis, List<ColumnDataSource> columns) {
        this.chartType = chartType;
        this.doubleAxis = doubleAxis;
        if (doubleAxis) {
            rightYAxisMultiplier = 20f; // TODO: calculate dynamically
        }
        addColumns(columns);
    }

    @Override
    public ChartType getChartType() {
        return chartType;
    }

    public int getColumnsCount() {
        return columns.size();
    }

    public int getRowsCount() {
        return rowsCount;
    }

    public ColumnDataSource getColumn(int column) {
        if (column < 0 || column >= columns.size()) {
            return null;
        }
        return columns.get(column);
    }

    @Override
    public int getXAxisValueSourceColumn() {
        // just return first found x column
        for (int c = 0; c < columns.size(); ++c) {
            if (columns.get(c).getType().equals(ColumnType.X)) {
                return c;
            }
        }
        return -1;
    }

    @Override
    public int getYAxisValueSourceColumn(YAxis yAxis) {
        for (int c = 0; c < columns.size(); ++c) {
            if (columns.get(c).getYAxis().equals(yAxis)) {
                return c;
            }
        }
        return -1;
    }

    @Override
    public float getRightYAxisMultiplier() {
        return rightYAxisMultiplier;
    }

    public void addColumn(ColumnDataSource column) {
        columns.add(column);
        updateRowsCount();
    }

    public void addColumns(List<ColumnDataSource> columns) {
        this.columns.addAll(columns);
        updateRowsCount();
    }

    @Override
    public boolean isColumnVisible(int column) {
        return isColumnVisible(getColumn(column));
    }

    @Override
    public boolean isColumnVisible(ColumnDataSource columnDataSource) {
        return !invisibleColumns.contains(columnDataSource);
    }

    @Override
    public void setColumnVisibility(int column, boolean visible) {
        boolean changed;
        if (visible) {
            changed = invisibleColumns.remove(getColumn(column));
        } else {
            changed = invisibleColumns.add(getColumn(column));
        }
        if (changed) {
            for (Listener listener : listeners) {
                listener.onSetColumnVisibility(column, visible);
            }
        }
    }

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void updateRowsCount() {
        int maxRows = 0;
        for (ColumnDataSource column: columns) {
            maxRows = Math.max(maxRows, column.getRowsCount());
        }
        rowsCount = maxRows;
    }

}
