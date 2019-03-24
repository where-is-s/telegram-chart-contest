package contest.datasource;

import java.util.Locale;

/**
 * Created by Alex K on 19/03/2019.
 */
public class IntegerColumnDataSource extends BaseColumnDataSource {

    private boolean readableFormatting = false;

    public IntegerColumnDataSource(ColumnType type, String name, int color, long values[]) {
        super(type, name, color, values);
    }

    public void setReadableFormatting(boolean readableFormatting) {
        this.readableFormatting = readableFormatting;
    }

    @Override
    public String formatValue(long value, ValueFormatType valueFormatType) {
        if (!readableFormatting) {
            return String.valueOf(value);
        }
        if (value < 1000) {
            return String.valueOf(value);
        } else if (value < 10000) {
            return String.format(Locale.getDefault(), "%.01fk", value/1000f);
        } else if (value < 1000000) {
            return String.format(Locale.getDefault(), "%dk", (int) (value/1000f));
        } else if (value < 10000000) {
            return String.format(Locale.getDefault(), "%.01fm", value/1000000f);
        } else if (value < 1000000000) {
            return String.format(Locale.getDefault(), "%dm", (int) (value/1000000f));
        } else if (value < 10000000000L) {
            return String.format(Locale.getDefault(), "%.01fb", value/1000000000f);
        } else {
            return String.format(Locale.getDefault(), "%db", (int) (value/1000000000f));
        }
    }

}
