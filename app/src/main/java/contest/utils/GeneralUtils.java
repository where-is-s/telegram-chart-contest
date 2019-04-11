package contest.utils;

import android.content.Context;
import android.graphics.Paint;
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

    public static Typeface getBoldTypeface() {
        return Typeface.create("sans-serif", Typeface.BOLD);
    }

    public static Typeface getMediumTypeface() {
        if (Build.VERSION.SDK_INT >= 21) {
            return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        } else {
            return getBoldTypeface();
        }
    }

    public static float getFontHeight(Paint textPaint) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        return fm.descent - fm.ascent;
    }

}
