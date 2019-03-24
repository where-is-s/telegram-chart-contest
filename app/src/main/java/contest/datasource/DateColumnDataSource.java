package contest.datasource;


import android.text.format.DateFormat;

/**
 * Created by Alex K on 19/03/2019.
 */
public class DateColumnDataSource extends BaseColumnDataSource {

    private final String dateFormat = "MMM dd";
    private final String dateFormatLong = "EEE, MMM dd";

    public DateColumnDataSource(ColumnType type, String name, int color, long values[]) {
        super(type, name, color, values);
    }

    @Override
    public String formatValue(long value, ValueFormatType valueFormatType) {
        return String.valueOf(DateFormat.format(valueFormatType.equals(ValueFormatType.HINT_TITLE) ? dateFormatLong : dateFormat, value));
    }

}
