package contest.datasource;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Created by Alex K on 19/03/2019.
 */
public class IntegerColumnDataSource extends BaseColumnDataSource {

    private final ChartDataSource.YAxis yAxis;
    private final DecimalFormat formatter;

    public IntegerColumnDataSource(ChartDataSource.YAxis yAxis, ColumnType type, String name, int color, long values[]) {
        super(type, name, color, values);
        this.yAxis = yAxis;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator(' ');
        formatter = new DecimalFormat("###,###", symbols);
    }

    public ChartDataSource.YAxis getYAxis() {
        return yAxis;
    }

    @Override
    public String formatValue(long value, ValueFormatType valueFormatType) {
        switch (valueFormatType) {
            case VERT_GRID:
                if (value < 10000) {
                    return String.valueOf(value);
                } else if (value < 1000000) {
                    return String.format(Locale.getDefault(), "%dK", (int) (value/1000));
                } else if (value < 1000000000L) {
                    return String.format(Locale.getDefault(), "%dM", value/1000000L);
                } else {
                    return String.format(Locale.getDefault(), "%dB", (int) (value/1000000000L));
                }
            case HINT_VALUE:
                return formatter.format(value);
            default:
                return "not specified";
        }
    }

}
