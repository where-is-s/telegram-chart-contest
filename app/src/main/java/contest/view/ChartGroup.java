package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import contest.datasource.ChartDataSource;
import contest.datasource.ChartType;
import contest.datasource.ColumnDataSource;
import contest.datasource.ValueFormatType;
import contest.utils.GeneralUtils;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartGroup extends LinearLayout implements RangeListener {

    private LinearLayout headerLayout;
    private TextView headerText;
    private TextView rangeText;
    private ChartView chartView;
    private ChartNavigationView chartNavigationView;
    private ChartLegendView chartLegendView;
    private boolean singleRangeText;

    public ChartGroup(Context context) {
        super(context);
        init();
    }

    public ChartGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChartGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChartGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        int dp16 = GeneralUtils.dp2px(getContext(), 16);

        setOrientation(LinearLayout.VERTICAL);

        headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(HORIZONTAL);

        headerText = new TextView(getContext());
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        headerText.setTextColor(Color.BLACK);
        headerText.setTypeface(GeneralUtils.getBoldTypeface());
        headerText.setPadding(dp16, GeneralUtils.dp2px(getContext(), 24), dp16, dp16);
        headerText.setLines(1);
        headerText.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.weight = 1;
        headerLayout.addView(headerText, layoutParams);

        rangeText = new TextView(getContext());
        rangeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        rangeText.setTextColor(Color.BLACK);
        rangeText.setTypeface(GeneralUtils.getMediumTypeface());
        rangeText.setPadding(0, GeneralUtils.dp2px(getContext(), 24), dp16, dp16);
        layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLayout.addView(rangeText, layoutParams);

        addView(headerLayout);

        chartView = new ChartView(getContext());
        chartView.setChartLineWidth(GeneralUtils.dp2px(getContext(), 1.5f));
        chartView.setBottomBound(0f);
        chartView.setPadding(dp16, dp16/4, dp16, dp16);
        chartView.addListener(this);
        addView(chartView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, GeneralUtils.dp2px(getContext(), 360)));
        setClipToPadding(false);
        setClipChildren(false);

        chartNavigationView = new ChartNavigationView(getContext());
        chartNavigationView.setChartLineWidth(GeneralUtils.dp2px(getContext(), 1));
        chartNavigationView.setBottomBound(0f);
        chartNavigationView.setPadding(dp16, 0, dp16, 0);
        chartNavigationView.setListener(chartView);
        chartView.addListener(chartNavigationView);
        addView(chartNavigationView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, GeneralUtils.dp2px(getContext(), 48)));

        chartLegendView = new ChartLegendView(getContext());
        chartLegendView.setPadding(0, 0, 0, dp16);
        addView(chartLegendView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    
    public void setChartDataSource(ChartDataSource chartDataSource) {
        chartView.setChartDataSource(chartDataSource);
        chartNavigationView.setChartDataSource(chartDataSource);
        chartLegendView.setChartDataSource(chartDataSource);
        float bottomBound = chartDataSource.getChartType().equals(ChartType.LINE) ? ChartView.NO_BOUND : 0f;
        chartView.setBottomBound(bottomBound);
        chartNavigationView.setBottomBound(bottomBound);
    }

    public void setHeaderText(String text) {
        headerText.setText(text);
    }

    public void setHeaderColor(int color) {
        headerText.setTextColor(color);
    }

    public void setHeaderIcon(int resId) {
        headerText.setCompoundDrawables(getResources().getDrawable(resId), null, null, null);
    }

    public void setHeaderClickListener(OnClickListener onClickListener) {
        headerText.setOnClickListener(onClickListener);
    }

    public ChartView getChartView() {
        return chartView;
    }

    public ChartNavigationView getChartNavigationView() {
        return chartNavigationView;
    }

    public ChartLegendView getChartLegendView() {
        return chartLegendView;
    }

    @Override
    public void onRangeSelected(float startRow, float endRow) {
        ChartDataSource chartDataSource = chartView.getChartDataSource();
        int xAxisColumnIdx = chartDataSource.getXAxisValueSourceColumn();
        ColumnDataSource xAxisColumn = chartDataSource.getColumn(xAxisColumnIdx);
        if (singleRangeText) {
            String left = xAxisColumn.formatValue(xAxisColumn.getValue((int) startRow), ValueFormatType.RANGE_TITLE_LONG);
            rangeText.setText(left);
        } else {
            String left = xAxisColumn.formatValue(xAxisColumn.getValue((int) startRow), ValueFormatType.RANGE_TITLE_SHORT);
            String right = xAxisColumn.formatValue(xAxisColumn.getValue((int) endRow), ValueFormatType.RANGE_TITLE_SHORT);
            if (left.equals(right)) {
                rangeText.setText(xAxisColumn.formatValue(xAxisColumn.getValue((int) startRow), ValueFormatType.RANGE_TITLE_LONG));
            } else {
                rangeText.setText(String.format("%s - %s", left, right));
            }
        }

    }

    @Override
    public void onStartDragging() {
    }

    @Override
    public void onStopDragging() {
    }

    public void setSingleRangeText(boolean singleRangeText) {
        this.singleRangeText = singleRangeText;
    }
}
