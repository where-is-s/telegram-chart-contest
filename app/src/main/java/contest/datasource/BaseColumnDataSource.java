package contest.datasource;

import android.util.LruCache;

/**
 * Created by Alex K on 19/03/2019.
 */
public abstract class BaseColumnDataSource implements ColumnDataSource {

    private ColumnType type;
    private String name;
    private int color;
    private long values[];
    private LruCache<Long, String> formatCache;

    public BaseColumnDataSource(ColumnType type, String name, int color, long values[]) {
        this.type = type;
        this.name = name;
        this.color = color;
        this.values = values;
        this.formatCache = new LruCache<Long, String>(2000);
    }

    public int getRowsCount() {
        return values.length;
    }

    public long getValue(int row) {
        return values[row];
    }

    public long[] getValues() {
        return values;
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

    @Override
    public ChartDataSource.YAxis getYAxis() {
        return ChartDataSource.YAxis.NONE;
    }

    public void setValues(long values[]) {
        this.values = values;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String formatValue(long value, ValueFormatType valueFormatType) {
        boolean useCache = shouldPutInCache(value, valueFormatType);
        if (useCache) {
            long index = value | ((long) valueFormatType.asInt()) << 56; // TODO: this won't work for really large values, fix later
            String result = formatCache.get(index);
            if (result != null) {
                return result;
            }
        }
        String result = doFormatValue(value, valueFormatType);
        if (result != null && useCache) {
            long index = value | ((long) valueFormatType.asInt()) << 56;
            formatCache.put(index, result);
        }
        return result;
    }

    protected boolean shouldPutInCache(long value, ValueFormatType valueFormatType) {
        return true;
    }
    protected abstract String doFormatValue(long value, ValueFormatType valueFormatType);
}
