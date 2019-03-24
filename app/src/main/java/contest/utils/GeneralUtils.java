package contest.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TypedValue;

/**
 * Created by Alex K on 22/03/2019.
 */
public class GeneralUtils {

    public static int dp2px(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int sp2px(Context context, float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static Typeface getMediumTypeface() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } else {
            // rough fallback... sorry about that, just keeping the app small
            return Typeface.create("sans-serif", Typeface.BOLD);
        }
    }

}
