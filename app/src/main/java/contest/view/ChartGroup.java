package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import contest.datasource.ChartDataSource;
import contest.utils.GeneralUtils;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartGroup extends LinearLayout {

    private TextView headerText;
    private ChartView chartView;
    private ChartNavigationView chartNavigationView;
    private ChartLegendView chartLegendView;

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
        setBackgroundColor(Color.WHITE);

        headerText = new TextView(getContext());
        headerText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        headerText.setTextColor(0xff3896d4);
        headerText.setTypeface(GeneralUtils.getMediumTypeface());
        headerText.setPadding(dp16, GeneralUtils.dp2px(getContext(), 24), dp16, dp16);
        addView(headerText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        chartView = new ChartView(getContext());
        chartView.setChartLineWidth(GeneralUtils.dp2px(getContext(), 1.5f));
        chartView.setBottomBound(0f);
        chartView.setPadding(dp16, GeneralUtils.dp2px(getContext(), 72), dp16, dp16);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, GeneralUtils.dp2px(getContext(), 360));
        layoutParams.topMargin = -GeneralUtils.dp2px(getContext(), 68);
        addView(chartView, layoutParams);
        setClipToPadding(false);
        setClipChildren(false);

        chartNavigationView = new ChartNavigationView(getContext());
        chartNavigationView.setChartLineWidth(GeneralUtils.dp2px(getContext(), 1));
        chartNavigationView.setBottomBound(0f);
        chartNavigationView.setPadding(dp16, 0, dp16, 0);
        chartNavigationView.setChartView(chartView);
        chartView.setChartNavigationView(chartNavigationView);
        addView(chartNavigationView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, GeneralUtils.dp2px(getContext(), 48)));

        chartLegendView = new ChartLegendView(getContext());
        chartLegendView.setPadding(dp16, dp16, 0, 0);
        addView(chartLegendView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    
    public void setChartDataSource(ChartDataSource chartDataSource) {
        chartView.setChartDataSource(chartDataSource);
        chartNavigationView.setChartDataSource(chartDataSource);
        chartLegendView.setChartDataSource(chartDataSource);
    }

    public void setHeaderText(String text) {
        headerText.setText(text);
    }

    public void setHeaderColor(int color) {
        headerText.setTextColor(color);
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

}
