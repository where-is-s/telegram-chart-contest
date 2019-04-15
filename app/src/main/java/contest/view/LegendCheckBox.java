package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import contest.utils.GeneralUtils;
import contest.utils.SimpleAnimator;
import telegram.contest.chart.R;

/**
 * Created by Alex K on 19/03/2019.
 */
public class LegendCheckBox extends View {

    public interface Listener {
        void onCheckedChanged(LegendCheckBox checkBox, boolean isChecked);
        void onLongTap(LegendCheckBox checkBox, boolean isChecked);
    }

    private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF drawRect = new RectF();
    private Rect textBounds = new Rect();
    private Bitmap checkBitmap;
    private Bitmap checkedBitmap;
    private Bitmap uncheckedBitmap;
    private Paint copyPaint = new Paint();

    private String text;
    private boolean checked = true;
    private float checkedOpacity;
    private SimpleAnimator checkedAnimator;
    private float edgeRadius;

    private Listener listener;

    public LegendCheckBox(Context context) {
        super(context);
        init();
    }

    public LegendCheckBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LegendCheckBox(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LegendCheckBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        checkBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.check)).getBitmap();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(GeneralUtils.getMediumTypeface());
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setStrokeWidth(GeneralUtils.dp2px(getContext(), 2));
        setEdgeRadius(GeneralUtils.dp2px(getContext(), 18));
        setTextSize(GeneralUtils.sp2px(getContext(), 16));
        setClickable(true);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setChecked(!isChecked(), true);
                if (listener != null) {
                    listener.onCheckedChanged(LegendCheckBox.this, isChecked());
                }
                invalidate();
            }
        });
        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (listener != null) {
                    listener.onLongTap(LegendCheckBox.this, isChecked());
                }
                invalidate();
                return true;
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float width = getPaddingLeft() + getPaddingRight() + 2 * edgeRadius + checkBitmap.getWidth() + textPaint.measureText(text);
        float height = getPaddingTop() + getPaddingBottom() + 2 * edgeRadius;
        setMeasuredDimension((int) width, (int) height);

        updateBitmaps();
    }

    private void updateBitmaps() {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        if (width == 0 || height == 0) {
            return;
        }
        checkedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        uncheckedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(checkedBitmap);
        drawRect.left = getPaddingLeft();
        drawRect.top = getPaddingTop();
        drawRect.right = width - getPaddingRight();
        drawRect.bottom = height - getPaddingBottom();
        backgroundPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(drawRect, edgeRadius, edgeRadius, backgroundPaint);
        canvas.drawBitmap(checkBitmap, drawRect.left + edgeRadius * 0.8f, drawRect.centerY() - checkBitmap.getHeight() * 0.5f, null);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        textPaint.setColor(Color.WHITE);
        canvas.drawText(text, drawRect.left + edgeRadius + checkBitmap.getWidth(), drawRect.centerY() - textBounds.exactCenterY(), textPaint);

        canvas = new Canvas(uncheckedBitmap);
        float offset = backgroundPaint.getStrokeWidth() * 0.5f;
        drawRect.left = getPaddingLeft() + offset;
        drawRect.top = getPaddingTop() + offset;
        drawRect.right = width - offset - getPaddingRight();
        drawRect.bottom = height - offset - getPaddingBottom();
        backgroundPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(drawRect, edgeRadius, edgeRadius, backgroundPaint);
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        textPaint.setColor(backgroundPaint.getColor());
        canvas.drawText(text, drawRect.centerX() - textBounds.exactCenterX(), drawRect.centerY() - textBounds.exactCenterY(), textPaint);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(final boolean checked, boolean animate) {
        this.checked = checked;
        if (checkedAnimator != null) {
            checkedAnimator.cancel();
        }
        if (animate) {
            checkedAnimator = new SimpleAnimator();
            checkedAnimator.setDuration(100);
            checkedAnimator.setListener(new SimpleAnimator.Listener() {
                @Override
                public void onEnd() {
                    checkedAnimator = null;
                }

                @Override
                public void onUpdate() {
                    checkedOpacity = checked ? checkedAnimator.getFraction() : (1 - checkedAnimator.getFraction());
                    invalidate();
                }
            });
            checkedAnimator.start();
        } else {
            checkedOpacity = checked ? 1f : 0f;
        }
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setEdgeRadius(float edgeRadius) {
        this.edgeRadius = edgeRadius;
        requestLayout();
    }

    public void setTextSize(float textSize) {
        textPaint.setTextSize(textSize);
    }

    public void setBackgroundColor(int color) {
        backgroundPaint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (checkedOpacity == 1f) {
            canvas.drawBitmap(checkedBitmap, 0, 0, null);
        } else if (checkedOpacity == 0f) {
            canvas.drawBitmap(uncheckedBitmap, 0, 0, null);
        } else {
            copyPaint.setAlpha((int) (255 * (1f - checkedOpacity)));
            canvas.drawBitmap(uncheckedBitmap, 0, 0, copyPaint);
            copyPaint.setAlpha((int) (255 * checkedOpacity));
            canvas.drawBitmap(checkedBitmap, 0, 0, copyPaint);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

}
