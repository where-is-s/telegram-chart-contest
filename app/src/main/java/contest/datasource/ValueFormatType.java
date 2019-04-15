package contest.datasource;

/**
 * Created by Alex K on 19/03/2019.
 */
public enum ValueFormatType {

    HORZ_GRID(1), VERT_GRID(2), HINT_TITLE(3), HINT_VALUE(4), RANGE_TITLE_LONG(5), RANGE_TITLE_SHORT(6), LEGEND(7);

    private int value;

    ValueFormatType(int value) {
        this.value = value;
    }

    public int asInt() {
        return value;
    }
}
