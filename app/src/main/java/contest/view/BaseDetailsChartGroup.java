package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import contest.datasource.ChartDataSource;
import contest.datasource.SimpleChartDataSource;
import contest.utils.EarlyDecelerateInterpolator;
import contest.utils.EarlyLinearInterpolator;
import contest.utils.LateDecelerateInterpolator;
import contest.utils.LateLinearInterpolator;
import contest.utils.SimpleAnimator;

/**
 * Created by Alex K on 19/03/2019.
 */
public abstract class BaseDetailsChartGroup extends FrameLayout implements ChartView.DetailsListener {

    private static final int ANIMATE_MAIN_CENTER = 1;
    private static final int ANIMATE_MAIN_WIDTH = 2;
    private static final int ANIMATE_DETAILS_ALPHA = 3;
    private static final int ANIMATE_MAIN_ALPHA = 4;
    private static final int ANIMATE_DETAILS_LEFT = 5;
    private static final int ANIMATE_DETAILS_RIGHT = 6;

    ChartGroup mainChartGroup;
    ChartGroup detailsChartGroup;
    String assetBaseName;
    ChartDataSource mainDataSource;
    SimpleChartDataSource detailsDataSource;
    float mainSavedCenter;
    float mainSavedWidth;

    public BaseDetailsChartGroup(Context context) {
        super(context);
        init();
    }

    public BaseDetailsChartGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BaseDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public BaseDetailsChartGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    protected void init() {
        setBackgroundColor(Color.WHITE);

        mainChartGroup = new ChartGroup(getContext());
        mainChartGroup.getChartView().setDetailsListener(this);
        addView(mainChartGroup, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        detailsChartGroup = new ChartGroup(getContext());
        detailsChartGroup.setVisibility(INVISIBLE);
        detailsChartGroup.setShortRangeText(true);
        detailsChartGroup.setHeaderText("Zoom Out");
        detailsChartGroup.getChartView().setGesturesEnabled(false);
        detailsChartGroup.setHeaderClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleZoomOutClick();
            }
        });
        addView(detailsChartGroup, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    
    public void setChartDataSource(ChartDataSource chartDataSource, String assetBaseName, final SimpleChartDataSource detailsDataSource) {
        this.mainDataSource = chartDataSource;
        this.detailsDataSource = detailsDataSource;
        chartDataSource.addListener(new ChartDataSource.Listener() {
            @Override
            public void onSetColumnVisibility(int column, boolean visible) {
                detailsDataSource.setColumnVisibility(column, visible);
            }
        });
        detailsDataSource.addListener(new ChartDataSource.Listener() {
            @Override
            public void onSetColumnVisibility(int column, boolean visible) {
                mainDataSource.setColumnVisibility(column, visible);
            }
        });
        mainChartGroup.setChartDataSource(chartDataSource);
        this.assetBaseName = assetBaseName;
    }

    @Override
    public boolean isDetailsAvailable(int row) {
        return true;
    }

    @Override
    public void onDetailsClick(int row) {
        handleDetailsClick(row);
    }

    public void setHeaderText(String headerText) {
        mainChartGroup.setHeaderText(headerText);
    }

    protected float getLeftDetailsRowToAnimateTo() {
        return 0;
    }

    protected float getRightDetailsRowToAnimateTo() {
        return detailsDataSource.getRowsCount() - 1;
    }

    protected abstract void prepareDetailsChartGroup(int row);

    protected void handleDetailsClick(int row) {
        prepareDetailsChartGroup(row);

        detailsChartGroup.setChartDataSource(detailsDataSource);
        detailsChartGroup.getChartView().setSelectedItem(-1);
        detailsChartGroup.getChartLegendView().update();
        detailsChartGroup.setVisibility(VISIBLE);

        final SimpleAnimator simpleAnimator = new SimpleAnimator();
        float mainLeftBound = mainChartGroup.getChartView().leftBound;
        float mainRightBound = mainChartGroup.getChartView().rightBound;
        mainSavedCenter = (mainLeftBound + mainRightBound) / 2;
        mainSavedWidth = mainRightBound - mainLeftBound + 1;
        simpleAnimator.addValue(ANIMATE_MAIN_CENTER, mainSavedCenter, row, new EarlyDecelerateInterpolator(0.4f));
        simpleAnimator.addValue(ANIMATE_MAIN_WIDTH, mainSavedWidth, 5, new EarlyDecelerateInterpolator(0.8f));
        simpleAnimator.addValue(ANIMATE_DETAILS_ALPHA, 0f, 1f, new LateLinearInterpolator(0.1f));
        simpleAnimator.addValue(ANIMATE_MAIN_ALPHA, 1f, 0f, new EarlyLinearInterpolator(0.8f));
        simpleAnimator.addValue(ANIMATE_DETAILS_LEFT, (float) 0, getLeftDetailsRowToAnimateTo(), new LateDecelerateInterpolator(0.3f));
        simpleAnimator.addValue(ANIMATE_DETAILS_RIGHT, (float) detailsDataSource.getRowsCount() - 1, getRightDetailsRowToAnimateTo(), new LateDecelerateInterpolator(0.3f));
        simpleAnimator.setDuration(400);
        final int savedSpeed = detailsChartGroup.getChartView().getAnimationSpeed();
        detailsChartGroup.getChartView().setAnimationSpeed(100); // for faster animation in the end
        simpleAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                mainChartGroup.setVisibility(INVISIBLE);
                detailsChartGroup.getChartView().setAnimationSpeed(savedSpeed);
            }

            @Override
            public void onUpdate() {
                float center = simpleAnimator.getFloatValue(ANIMATE_MAIN_CENTER);
                float width = simpleAnimator.getFloatValue(ANIMATE_MAIN_WIDTH);
                mainChartGroup.setAlpha(simpleAnimator.getFloatValue(ANIMATE_MAIN_ALPHA));
                mainChartGroup.getChartView().setBounds(center - width / 2, center + width / 2, true);
                detailsChartGroup.setAlpha(simpleAnimator.getFloatValue(ANIMATE_DETAILS_ALPHA));
                detailsChartGroup.getChartView().setBounds(simpleAnimator.getFloatValue(ANIMATE_DETAILS_LEFT),
                        simpleAnimator.getFloatValue(ANIMATE_DETAILS_RIGHT), true);
            }
        });
        simpleAnimator.start();
    }

    protected void handleZoomOutClick() {
        mainChartGroup.setVisibility(VISIBLE);
        final SimpleAnimator simpleAnimator = new SimpleAnimator();
        float mainCenter = (mainChartGroup.getChartView().leftBound + mainChartGroup.getChartView().rightBound) / 2;
        float mainWidth = mainChartGroup.getChartView().rightBound - mainChartGroup.getChartView().leftBound;
        simpleAnimator.addValue(ANIMATE_MAIN_CENTER, mainCenter, mainSavedCenter, new LateDecelerateInterpolator(0.5f));
        simpleAnimator.addValue(ANIMATE_MAIN_WIDTH, mainWidth, mainSavedWidth, new LateDecelerateInterpolator(0.4f));
        simpleAnimator.addValue(ANIMATE_DETAILS_ALPHA, 1f, 0f, new EarlyLinearInterpolator(0.8f));
        simpleAnimator.addValue(ANIMATE_MAIN_ALPHA, 0f, 1f, new LateLinearInterpolator(0.3f));
        simpleAnimator.addValue(ANIMATE_DETAILS_LEFT, detailsChartGroup.getChartView().leftBound, 0, new EarlyLinearInterpolator(0.7f));
        simpleAnimator.addValue(ANIMATE_DETAILS_RIGHT, detailsChartGroup.getChartView().rightBound, detailsDataSource.getRowsCount() - 1, new EarlyLinearInterpolator(0.7f));
        simpleAnimator.setDuration(400);
        final int savedSpeed = mainChartGroup.getChartView().getAnimationSpeed();
        mainChartGroup.getChartView().setAnimationSpeed(100); // for faster animation in the end
        simpleAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                detailsChartGroup.setVisibility(INVISIBLE);
                mainChartGroup.getChartView().setAnimationSpeed(savedSpeed);
            }

            @Override
            public void onUpdate() {
                float center = simpleAnimator.getFloatValue(ANIMATE_MAIN_CENTER);
                float width = simpleAnimator.getFloatValue(ANIMATE_MAIN_WIDTH);
                mainChartGroup.setAlpha(simpleAnimator.getFloatValue(ANIMATE_MAIN_ALPHA));
                mainChartGroup.getChartView().setBounds(center - width / 2, center + width / 2, true);
                detailsChartGroup.setAlpha(simpleAnimator.getFloatValue(ANIMATE_DETAILS_ALPHA));
                detailsChartGroup.getChartView().setBounds(simpleAnimator.getFloatValue(ANIMATE_DETAILS_LEFT),
                        simpleAnimator.getFloatValue(ANIMATE_DETAILS_RIGHT), true);
            }
        });
        simpleAnimator.start();
    }

}
