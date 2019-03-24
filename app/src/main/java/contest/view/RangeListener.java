package contest.view;

/**
 * Created by Alex Kravchenko on 23/03/2019.
 */
public interface RangeListener {

    void onRangeSelected(float startRow, float endRow);
    void onStartDragging();
    void onStopDragging();

}
