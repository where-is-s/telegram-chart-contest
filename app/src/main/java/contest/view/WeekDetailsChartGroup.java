package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import contest.datasource.BaseColumnDataSource;
import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.SimpleChartDataSource;
import contest.utils.BinaryUtils;
import contest.utils.SimpleAnimator;

/**
 * Created by Alex K on 19/03/2019.
 */
public class WeekDetailsChartGroup extends FrameLayout implements ChartView.DetailsListener {

    private ChartGroup mainChartGroup;
    private ChartGroup detailsChartGroup;
    private String assetBaseName;
    private ChartDataSource mainDataSource;
    private SimpleChartDataSource detailsDataSource;

    public WeekDetailsChartGroup(Context context) {
        super(context);
        init();
    }

    public WeekDetailsChartGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeekDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WeekDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mainChartGroup = new ChartGroup(getContext());
        mainChartGroup.getChartView().setDetailsListener(this);
        addView(mainChartGroup, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        detailsChartGroup = new ChartGroup(getContext());
        detailsChartGroup.setVisibility(GONE);
        addView(detailsChartGroup, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    
    public void setChartDataSource(ChartDataSource chartDataSource, String assetBaseName, SimpleChartDataSource detailsDataSource) {
        this.mainDataSource = chartDataSource;
        this.detailsDataSource = detailsDataSource;
        mainChartGroup.setChartDataSource(chartDataSource);
        this.assetBaseName = assetBaseName;
    }

    @Override
    public boolean isDetailsAvailable(int row) {
        return true;
    }

    @Override
    public void onDetailsClick(int row) {
        ColumnDataSource xColumn = mainDataSource.getColumn(mainDataSource.getXAxisValueSourceColumn());
        long selectedDayTime = xColumn.getValue(row);

        long arrays[][] = BinaryUtils.readDataArrays(getContext(), assetBaseName, detailsDataSource.getColumnsCount(),
                selectedDayTime - 3 * 24 * 3600 * 1000L, selectedDayTime + 4 * 24 * 3600 * 1000L);
        for (int i = 0; i < detailsDataSource.getColumnsCount(); ++i) {
            ((BaseColumnDataSource) detailsDataSource.getColumn(i)).setValues(arrays[i]);
        }
        detailsDataSource.updateRowsCount();

        detailsChartGroup.setChartDataSource(detailsDataSource);
        detailsChartGroup.setVisibility(VISIBLE);

        final SimpleAnimator simpleAnimator = new SimpleAnimator();
        simpleAnimator.addValue(0, mainChartGroup.getChartView().leftBound, row - 5);
        simpleAnimator.addValue(1, mainChartGroup.getChartView().rightBound, row + 5);
        simpleAnimator.addValue(2, 1f, 0f);
        simpleAnimator.setDuration(300);
        simpleAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                mainChartGroup.setVisibility(GONE);
            }

            @Override
            public void onUpdate() {
                float alpha = simpleAnimator.getFloatValue(2);
                mainChartGroup.getChartView().setAlpha(alpha);
                mainChartGroup.getChartView().setBounds(simpleAnimator.getFloatValue(0), simpleAnimator.getFloatValue(1), true);
                detailsChartGroup.getChartView().setAlpha(1f - alpha);
            }
        });
        simpleAnimator.start();
    }
}
