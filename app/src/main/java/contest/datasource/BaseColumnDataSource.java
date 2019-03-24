package contest.datasource;

/**
 * Created by Alex K on 19/03/2019.
 */
public abstract class BaseColumnDataSource implements ColumnDataSource {

    private ColumnType type;
    private String name;
    private int color;
    private long values[];

    public BaseColumnDataSource(ColumnType type, String name, int color, long values[]) {
        this.type = type;
        this.name = name;
        this.color = color;
        this.values = values;
    }

    public int getRowsCount() {
        return values.length;
    }

    public long getValue(int row) {
        return values[row];
    }

    public String getName() {
        return name;
    }
    public int getColor() {
        return color;
    }
    public ColumnType getType() {
        return type;
    }

}
