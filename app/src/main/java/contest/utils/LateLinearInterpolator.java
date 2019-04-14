package contest.utils;

import android.view.animation.Interpolator;

/**
 * Created by Alex Kravchenko on 09/04/2019.
 */
public class LateLinearInterpolator implements Interpolator {

    private float startAt;

    public LateLinearInterpolator(float startAt) {
        this.startAt = startAt;
    }

    @Override
    public float getInterpolation(float input) {
        if (input < startAt) {
            return 0f;
        }
        return (input - startAt) / (1 - startAt);
    }

}
