package contest.utils;

import android.view.Choreographer;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alex Kravchenko on 09/04/2019.
 */
public class SimpleAnimator implements Choreographer.FrameCallback {

    public static abstract class Listener {
        public void onUpdate() {}
        public void onEnd() {}
        public void onCancel() {}
    }

    private static class IntValue {
        int from;
        int to;
        int current;
    }

    private static class FloatValue {
        float from;
        float to;
        float current;
    }

    private int duration;
    private long startTime;
    private long endTime;
    private boolean running = false;
    private Map<Integer, Object> animatedValues = new HashMap<>();
    private Listener listener;
    private Interpolator interpolator = new LinearInterpolator();
    private float fraction;

    public SimpleAnimator() {}

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void addValue(int id, int from, int to) {
        IntValue intValue = new IntValue();
        intValue.from = from;
        intValue.to = to;
        animatedValues.put(id, intValue);
    }

    public void addValue(int id, float from, float to) {
        FloatValue floatValue = new FloatValue();
        floatValue.from = from;
        floatValue.to = to;
        animatedValues.put(id, floatValue);
    }

    public boolean hasValue(int id) {
        return animatedValues.containsKey(id);
    }

    public int getIntValue(int id) {
        return ((IntValue) animatedValues.get(id)).current;
    }

    public float getFloatValue(int id) {
        return ((FloatValue) animatedValues.get(id)).current;
    }

    public float getFraction() {
        return fraction;
    }

    public void start() {
        Choreographer.getInstance().removeFrameCallback(this);
        startTime = System.nanoTime();
        endTime = startTime + duration * 1000000L;
        running = true;
        doFrame(startTime);
    }

    public void stop() {
        if (listener != null) {
            listener.onEnd();
        }
        doStop();
    }

    public void cancel() {
        if (listener != null) {
            listener.onCancel();
        }
        doStop();
    }

    private void doStop() {
        animatedValues.clear();
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setInterpolator(Interpolator interpolator) {
        this.interpolator = interpolator;
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) {
            return;
        }
        this.fraction = (frameTimeNanos - startTime) / (float) (endTime - startTime);
        fraction = Math.max(0f, Math.min(1f, fraction));
        fraction = interpolator.getInterpolation(fraction);
        for (Map.Entry<Integer, Object> entry: animatedValues.entrySet()) {
            Object animatedValue = entry.getValue();
            if (animatedValue instanceof IntValue) {
                IntValue value = (IntValue) animatedValue;
                value.current = (int) (value.from + (value.to - value.from) * fraction);
            } else if (animatedValue instanceof FloatValue) {
                FloatValue value = (FloatValue) animatedValue;
                value.current = value.from + (value.to - value.from) * fraction;
            }
        }
        if (listener != null) {
            listener.onUpdate();
        }
        if (fraction >= 1f) {
            stop();
        } else {
//            TODO: optimize excessive callbacks when animating through UI events (using finger or another animation)
//            if (fraction == 0f) {
//                Choreographer.getInstance().postFrameCallbackDelayed(this, 20);
//            } else {
            Choreographer.getInstance().postFrameCallback(this);
//            }
        }
    }

}
