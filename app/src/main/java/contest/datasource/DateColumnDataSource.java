package contest.datasource;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Alex K on 19/03/2019.
 */
public class DateColumnDataSource extends BaseColumnDataSource {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM", Locale.getDefault());
    private final SimpleDateFormat dateFormatMedium = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dateFormatLong = new SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dateFormatLonger = new SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault());

    public DateColumnDataSource(ColumnType type, String name, int color, long values[]) {
        super(type, name, color, values);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        dateFormat.setTimeZone(utc);
        dateFormatMedium.setTimeZone(utc);
        dateFormatLong.setTimeZone(utc);
    }

    @Override
    protected String doFormatValue(long value, ValueFormatType valueFormatType) {
        switch (valueFormatType) {
            case HINT_TITLE:
                return dateFormatLong.format(new Date(value));
            case RANGE_TITLE_LONG:
                return dateFormatLonger.format(new Date(value));
            case RANGE_TITLE_SHORT:
                return dateFormatMedium.format(new Date(value));
            default:
                return dateFormat.format(new Date(value));
        }
    }

}