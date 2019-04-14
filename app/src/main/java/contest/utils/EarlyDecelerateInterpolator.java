package contest.utils;

import android.view.animation.Interpolator;

/**
 * Created by Alex Kravchenko on 09/04/2019.
 */
public class EarlyDecelerateInterpolator implements Interpolator {

    private float finishBy;

    public EarlyDecelerateInterpolator(float finishBy) {
        this.finishBy = finishBy;
    }

    @Override
    public float getInterpolation(float input) {
        if (input > finishBy) {
            return 1f;
        }
        return (float) Math.sqrt(input / finishBy);
    }

}
