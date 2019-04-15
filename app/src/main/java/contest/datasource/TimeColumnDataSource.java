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
    private final SimpleDateFormat legendFormat = new SimpleDateFormat("d MMM", Locale.getDefault());

    public TimeColumnDataSource(ColumnType type, String name, int color, long values[]) {
        super(type, name, color, values);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        timeFormat.setTimeZone(utc);
        dateFormat.setTimeZone(utc);
        legendFormat.setTimeZone(utc);
    }

    @Override
    protected String doFormatValue(long value, ValueFormatType valueFormatType) {
        switch (valueFormatType) {
            case LEGEND:
                return legendFormat.format(new Date(value));
            case RANGE_TITLE_LONG:
                return dateFormat.format(new Date(value));
            default:
                return timeFormat.format(new Date(value));
        }
    }

}