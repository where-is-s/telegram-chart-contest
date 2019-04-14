package contest.utils;

import android.view.animation.Interpolator;

/**
 * Created by Alex Kravchenko on 09/04/2019.
 */
public class LateDecelerateInterpolator implements Interpolator {

    private float startAt;

    public LateDecelerateInterpolator(float startAt) {
        this.startAt = startAt;
    }

    @Override
    public float getInterpolation(float input) {
        if (input < startAt) {
            return 0f;
        }
        return (float) Math.sqrt((input - startAt) / (1 - startAt));
    }

}
