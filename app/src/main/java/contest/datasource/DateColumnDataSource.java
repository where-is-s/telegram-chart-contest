package contest.datasource;


import android.text.format.DateFormat;

/**
 * Created by Alex K on 19/03/2019.
 */
public class DateColumnDataSource extends BaseColumnDataSource {

    private final String dateFormat = "MMM d";
    private final String dateFormatMedium = "d MMM yyyy";
    private final String dateFormatLong = "EEE, MMM d yyyy";

    public DateColumnDataSource(ColumnType type, String name, int color, long values[]) {
        super(type, name, color, values);
    }

    @Override
    public String formatValue(long value, ValueFormatType valueFormatType) {
        switch (valueFormatType) {
            case HINT_TITLE:
                return String.valueOf(DateFormat.format(dateFormatLong, value));
            case RANGE_TITLE:
                return String.valueOf(DateFormat.format(dateFormatMedium, value));
            default:
                return String.valueOf(DateFormat.format(dateFormat, value));
        }
    }

}