package contest.utils;

import android.view.animation.Interpolator;

/**
 * Created by Alex Kravchenko on 09/04/2019.
 */
public class EarlyLinearInterpolator implements Interpolator {

    private float finishBy;

    public EarlyLinearInterpolator(float finishBy) {
        this.finishBy = finishBy;
    }

    @Override
    public float getInterpolation(float input) {
        if (input > finishBy) {
            return 1f;
        }
        return input / finishBy;
    }

}
