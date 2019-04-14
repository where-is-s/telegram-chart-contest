package contest.datasource;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Alex K on 19/03/2019.
 */
public class TimeColumnDataSource extends BaseColumnDataSource {

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault());

    public TimeColumnDataSource(ColumnType type, String name, int color, long values[]) {
        super(type, name, color, values);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        timeFormat.setTimeZone(utc);
        dateFormat.setTimeZone(utc);
    }

    @Override
    public String formatValue(long value, ValueFormatType valueFormatType) {
        if (valueFormatType.equals(ValueFormatType.RANGE_TITLE)) {
            return String.valueOf(dateFormat.format(new Date(value)));
        } else {
            return String.valueOf(timeFormat.format(new Date(value)));
        }
    }

}