package contest.datasource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Alex K on 19/03/2019.
 */
public class SimpleChartDataSource implements ChartDataSource {

    private List<ColumnDataSource> columns = new ArrayList<>();
    private Set<ColumnDataSource> invisibleColumns = new HashSet<>();
    private Set<Listener> listeners = new HashSet<>();
    private int rowsCount = 0;

    public SimpleChartDataSource() {}

    public SimpleChartDataSource(List<ColumnDataSource> columns) {
        addColumns(columns);
    }

    public int getColumnsCount() {
        return columns.size();
    }

    public int getRowsCount() {
        return rowsCount;
    }

    public ColumnDataSource getColumn(int column) {
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
    public int getYAxisValueSourceColumn() {
        // just return first found line column
        for (int c = 0; c < columns.size(); ++c) {
            if (columns.get(c).getType().equals(ColumnType.LINE)) {
                return c;
            }
        }
        return -1;
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
        return !invisibleColumns.contains(getColumn(column));
    }

    @Override
    public void setColumnVisibility(int column, boolean visible) {
        boolean changed = false;
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

    private void updateRowsCount() {
        int maxRows = 0;
        for (ColumnDataSource column: columns) {
            maxRows = Math.max(maxRows, column.getRowsCount());
        }
        rowsCount = maxRows;
    }

}
