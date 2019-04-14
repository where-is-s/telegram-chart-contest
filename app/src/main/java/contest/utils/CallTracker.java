package contest.utils;

import android.util.Log;

/**
 * Created by Alex Kravchenko on 13/04/2019.
 */
public class CallTracker {

    private int calls = 0;
    private long lastSecond = System.currentTimeMillis();
    private String name;

    public CallTracker(String name) {
        this.name = name;
    }

    public void call() {
        ++calls;
        if (System.currentTimeMillis() > lastSecond + 1000) {
            Log.i("ZZZ", name + "/s: " + calls);
            calls = 0;
            lastSecond = System.currentTimeMillis();
        }
    }

}
