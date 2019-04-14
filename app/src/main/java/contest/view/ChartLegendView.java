package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.utils.GeneralUtils;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartLegendView extends LinearLayout {

    private ChartDataSource chartDataSource;
    private LegendCheckBox.Listener checkedChangeListener = new LegendCheckBox.Listener() {
        @Override
        public void onCheckedChanged(LegendCheckBox checkBox, boolean isChecked) {
            chartDataSource.setColumnVisibility((Integer) checkBox.getTag(), isChecked);
        }

        @Override
        public void onLongTap(LegendCheckBox tappedCheckBox, boolean isChecked) {
            chartDataSource.setColumnVisibility((Integer) tappedCheckBox.getTag(), true);
            for (LegendCheckBox checkBox: checkBoxes) {
                int c = (Integer) checkBox.getTag();
                if (chartDataSource.isColumnVisible(c) && checkBox != tappedCheckBox) {
                    chartDataSource.setColumnVisibility(c, false);
                }
            }
        }
    };
    private ChartDataSource.Listener chartDataSourceListener = new ChartDataSource.Listener() {
        @Override
        public void onSetColumnVisibility(int column, boolean visible) {
            for (LegendCheckBox checkBox: checkBoxes) {
                if (checkBox.getTag().equals(column)) {
                    checkBox.setChecked(visible);
                    break;
                }
            }
        }
    };
    private List<LegendCheckBox> checkBoxes = new ArrayList<>();

    public ChartLegendView(Context context) {
        super(context);
        init();
    }

    public ChartLegendView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChartLegendView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChartLegendView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setPadding(0, 0, 0, 0);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        int dp8 = GeneralUtils.dp2px(getContext(), 8);
        int dp16 = GeneralUtils.dp2px(getContext(), 16);
        super.setPadding(left + dp8, top + dp8, right + dp16, bottom);
    }

    public void setChartDataSource(ChartDataSource chartDataSource) {
        this.chartDataSource = chartDataSource;
        chartDataSource.addListener(chartDataSourceListener);
        update();
    }

    public void update() {
        checkBoxes.clear();

        for (int c = 0; c < chartDataSource.getColumnsCount(); c++) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(c);
            if (TextUtils.isEmpty(columnDataSource.getName())) {
                continue;
            }

            LegendCheckBox checkBox = new LegendCheckBox(getContext());
            checkBox.setText(columnDataSource.getName());
            checkBox.setChecked(chartDataSource.isColumnVisible(c));
            checkBox.setTag(c);
            checkBox.setListener(checkedChangeListener);
            checkBox.setTextSize(GeneralUtils.sp2px(getContext(), 16));
            int dp8 = GeneralUtils.dp2px(getContext(), 8);
            checkBox.setPadding(dp8, dp8, 0, 0);
            checkBox.setBackgroundColor(columnDataSource.getColor());
            checkBoxes.add(checkBox);
        }

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();

        removeAllViews();
        for (LegendCheckBox checkBox: checkBoxes) {
            ViewParent parent = checkBox.getParent();
            if (parent == null) {
                continue;
            }
            ((ViewGroup) parent).removeView(checkBox);
        }

        LinearLayout horzLineLayout = null;
        int currentWidth = 0;

        for (int i = 0; i < checkBoxes.size(); i++) {
            LegendCheckBox checkBox = checkBoxes.get(i);
            checkBox.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

            if (horzLineLayout == null || currentWidth + checkBox.getMeasuredWidth() >= width) {
                horzLineLayout = new LinearLayout(getContext());
                horzLineLayout.setOrientation(LinearLayout.HORIZONTAL);
                horzLineLayout.setGravity(Gravity.START);
                horzLineLayout.addView(checkBox, new LayoutParams(checkBox.getMeasuredWidth(), checkBox.getMeasuredHeight()));
                addView(horzLineLayout, new LayoutParams(LayoutParams.MATCH_PARENT, checkBox.getMeasuredHeight()));
                currentWidth = 0;
            } else {
                horzLineLayout.addView(checkBox);
            }
            currentWidth += checkBox.getMeasuredWidth();
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
