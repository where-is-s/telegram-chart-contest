package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
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
import telegram.contest.chart.R;

/**
 * Created by Alex K on 19/03/2019.
 */
public abstract class BaseDetailsChartGroup extends FrameLayout implements ChartView.DetailsListener {

    static final int ANIMATE_MAIN_CENTER = 1;
    static final int ANIMATE_MAIN_WIDTH = 2;
    static final int ANIMATE_DETAILS_ALPHA = 3;
    static final int ANIMATE_MAIN_ALPHA = 4;
    static final int ANIMATE_DETAILS_LEFT = 5;
    static final int ANIMATE_DETAILS_RIGHT = 6;
    static final int ANIMATE_DETAILS_SCALE = 7;
    static final int ANIMATE_MAIN_SCALE = 8;

    ChartGroup mainChartGroup;
    ChartGroup detailsChartGroup;
    String assetBaseName;
    ChartDataSource mainDataSource;
    ChartDataSource detailsDataSource;
    float mainSavedCenter;
    float mainSavedWidth;
    protected SimpleAnimator activeAnimator;
    protected boolean useSimpleAnimations = true;

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
        mainChartGroup = new ChartGroup(getContext());
        mainChartGroup.getChartView().setDetailsListener(this);
        addView(mainChartGroup, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        detailsChartGroup = new ChartGroup(getContext());
        detailsChartGroup.setVisibility(INVISIBLE);
        detailsChartGroup.setSingleRangeText(true);
        detailsChartGroup.setHeaderText("Zoom Out");
        detailsChartGroup.setHeaderIcon(R.drawable.zoom_out);
        detailsChartGroup.setHeaderColor(0xFF158BE3);
        detailsChartGroup.getChartView().setGesturesEnabled(false);
        detailsChartGroup.setHeaderClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activeAnimator != null) {
                    return;
                }
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
        if (activeAnimator != null) {
            return;
        }
        handleDetailsClick(row);
    }

    public void setHeaderText(String headerText) {
        mainChartGroup.setHeaderText(headerText);
    }

    protected abstract void prepareDetailsChartGroup(int row);

    protected void handleDetailsClick(int row) {
        prepareDetailsChartGroup(row);

        detailsChartGroup.setChartDataSource(detailsDataSource);
        detailsChartGroup.getChartView().setSelectedItem(-1);
        detailsChartGroup.getChartLegendView().update();

        activeAnimator = new SimpleAnimator();
        float mainLeftBound = mainChartGroup.getChartView().leftBound;
        float mainRightBound = mainChartGroup.getChartView().rightBound;
        mainSavedCenter = (mainLeftBound + mainRightBound) / 2;
        mainSavedWidth = mainRightBound - mainLeftBound + 1;
        activeAnimator.addValue(ANIMATE_DETAILS_ALPHA, 0f, 1f, new LateLinearInterpolator(0.3f));
        activeAnimator.addValue(ANIMATE_MAIN_ALPHA, 1f, 0f, new EarlyLinearInterpolator(0.8f));
        if (useSimpleAnimations) {
            activeAnimator.addValue(ANIMATE_DETAILS_SCALE, 0.5f, 1f, new LateLinearInterpolator(0.1f));
            activeAnimator.addValue(ANIMATE_MAIN_SCALE, 1f, 0.7f, new EarlyLinearInterpolator(0.8f));
        } else {
            activeAnimator.addValue(ANIMATE_MAIN_CENTER, mainSavedCenter, row, new EarlyDecelerateInterpolator(0.3f));
            activeAnimator.addValue(ANIMATE_MAIN_WIDTH, mainSavedWidth, 5, new EarlyDecelerateInterpolator(0.8f));
        }
        configureDetailsInAnimator(activeAnimator);
        activeAnimator.setDuration(useSimpleAnimations ? 250 : 400);
        final int savedSpeed = detailsChartGroup.getChartView().getAnimationSpeed();
        detailsChartGroup.getChartView().setAnimationSpeed(100); // for faster animation in the end
        mainChartGroup.getChartView().onStartDragging();
        detailsChartGroup.getChartView().onStartDragging();
        activeAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                activeAnimator = null;
                mainChartGroup.getChartView().onStopDragging();
                detailsChartGroup.getChartView().onStopDragging();
                mainChartGroup.setVisibility(INVISIBLE);
                detailsChartGroup.getChartView().setAnimationSpeed(savedSpeed);
                mainChartGroup.getChartView().setScaleX(1f);
                mainChartGroup.getChartView().setScaleY(1f);
                detailsChartGroup.getChartView().setScaleX(1f);
                detailsChartGroup.getChartView().setScaleY(1f);
            }

            @Override
            public void onUpdate() {
                if (setAlphaAndVisibility(mainChartGroup, activeAnimator.getFloatValue(ANIMATE_MAIN_ALPHA))) {
                    if (useSimpleAnimations) {
                        mainChartGroup.getChartView().setScaleX(activeAnimator.getFloatValue(ANIMATE_MAIN_SCALE));
                        mainChartGroup.getChartView().setScaleY(activeAnimator.getFloatValue(ANIMATE_MAIN_SCALE));
                    } else {
                        float center = activeAnimator.getFloatValue(ANIMATE_MAIN_CENTER);
                        float width = activeAnimator.getFloatValue(ANIMATE_MAIN_WIDTH);
                        mainChartGroup.getChartView().setBounds(center - width / 2, center + width / 2, true);
                    }
                }
                if (setAlphaAndVisibility(detailsChartGroup, activeAnimator.getFloatValue(ANIMATE_DETAILS_ALPHA))) {
                    if (useSimpleAnimations) {
                        detailsChartGroup.getChartView().setScaleX(activeAnimator.getFloatValue(ANIMATE_DETAILS_SCALE));
                        detailsChartGroup.getChartView().setScaleY(activeAnimator.getFloatValue(ANIMATE_DETAILS_SCALE));
                    } else {
                        detailsChartGroup.getChartView().setBounds(activeAnimator.getFloatValue(ANIMATE_DETAILS_LEFT),
                                activeAnimator.getFloatValue(ANIMATE_DETAILS_RIGHT), true);
                    }
                }
            }
        });
        activeAnimator.start();
    }

    protected void configureDetailsInAnimator(SimpleAnimator animator) {
        if (useSimpleAnimations) {
            detailsChartGroup.getChartView().setBounds(
                    0, detailsDataSource.getRowsCount() - 1, true);
        } else {
            animator.addValue(ANIMATE_DETAILS_LEFT, (float) 0, 0, new LateDecelerateInterpolator(0.3f));
            animator.addValue(ANIMATE_DETAILS_RIGHT, (float) detailsDataSource.getRowsCount() - 1, detailsDataSource.getRowsCount() - 1, new LateDecelerateInterpolator(0.3f));
        }
    }

    protected void handleZoomOutClick() {
        activeAnimator = new SimpleAnimator();
        float mainCenter = (mainChartGroup.getChartView().leftBound + mainChartGroup.getChartView().rightBound) / 2;
        float mainWidth = mainChartGroup.getChartView().rightBound - mainChartGroup.getChartView().leftBound;
        activeAnimator.addValue(ANIMATE_DETAILS_ALPHA, 1f, 0f, new EarlyLinearInterpolator(0.8f));
        activeAnimator.addValue(ANIMATE_MAIN_ALPHA, 0f, 1f, new LateLinearInterpolator(0.3f));
        if (useSimpleAnimations) {
            activeAnimator.addValue(ANIMATE_DETAILS_SCALE, 1f, 0.7f, new EarlyLinearInterpolator(0.9f));
            activeAnimator.addValue(ANIMATE_MAIN_SCALE, 0.5f, 1f, new LateLinearInterpolator(0.2f));
        } else {
            activeAnimator.addValue(ANIMATE_MAIN_CENTER, mainCenter, mainSavedCenter, new LateDecelerateInterpolator(0.5f));
            activeAnimator.addValue(ANIMATE_MAIN_WIDTH, mainWidth, mainSavedWidth, new LateDecelerateInterpolator(0.4f));
        }
        configureDetailsOutAnimator(activeAnimator);
        activeAnimator.setDuration(useSimpleAnimations ? 250 : 400);
        final int savedSpeed = mainChartGroup.getChartView().getAnimationSpeed();
        mainChartGroup.getChartView().setAnimationSpeed(100); // for faster animation in the end
        mainChartGroup.getChartView().onStartDragging();
        detailsChartGroup.getChartView().onStartDragging();
        activeAnimator.setListener(new SimpleAnimator.Listener() {
            @Override
            public void onEnd() {
                activeAnimator = null;
                mainChartGroup.getChartView().onStopDragging();
                detailsChartGroup.getChartView().onStopDragging();
                detailsChartGroup.setVisibility(INVISIBLE);
                mainChartGroup.getChartView().setAnimationSpeed(savedSpeed);
                mainChartGroup.getChartView().setScaleX(1f);
                mainChartGroup.getChartView().setScaleY(1f);
                detailsChartGroup.getChartView().setScaleX(1f);
                detailsChartGroup.getChartView().setScaleY(1f);
            }

            @Override
            public void onUpdate() {
                if (setAlphaAndVisibility(mainChartGroup, activeAnimator.getFloatValue(ANIMATE_MAIN_ALPHA))) {
                    if (useSimpleAnimations) {
                        mainChartGroup.getChartView().setScaleX(activeAnimator.getFloatValue(ANIMATE_MAIN_SCALE));
                        mainChartGroup.getChartView().setScaleY(activeAnimator.getFloatValue(ANIMATE_MAIN_SCALE));
                    } else {
                        float center = activeAnimator.getFloatValue(ANIMATE_MAIN_CENTER);
                        float width = activeAnimator.getFloatValue(ANIMATE_MAIN_WIDTH);
                        mainChartGroup.getChartView().setBounds(center - width / 2, center + width / 2, true);
                    }
                }
                if (setAlphaAndVisibility(detailsChartGroup, activeAnimator.getFloatValue(ANIMATE_DETAILS_ALPHA))) {
                    if (useSimpleAnimations) {
                        detailsChartGroup.getChartView().setScaleX(activeAnimator.getFloatValue(ANIMATE_DETAILS_SCALE));
                        detailsChartGroup.getChartView().setScaleY(activeAnimator.getFloatValue(ANIMATE_DETAILS_SCALE));
                    } else {
                        detailsChartGroup.getChartView().setBounds(activeAnimator.getFloatValue(ANIMATE_DETAILS_LEFT),
                                activeAnimator.getFloatValue(ANIMATE_DETAILS_RIGHT), true);
                    }
                }
            }
        });
        activeAnimator.start();
    }

    protected void configureDetailsOutAnimator(SimpleAnimator animator) {
        if (!useSimpleAnimations) {
            animator.addValue(ANIMATE_DETAILS_LEFT, detailsChartGroup.getChartView().leftBound, 0, new EarlyLinearInterpolator(0.7f));
            animator.addValue(ANIMATE_DETAILS_RIGHT, detailsChartGroup.getChartView().rightBound, detailsDataSource.getRowsCount() - 1, new EarlyLinearInterpolator(0.7f));
        }
    }

    protected boolean setAlphaAndVisibility(View view, float alpha) {
        if (view.getVisibility() == INVISIBLE) {
            if (alpha < 0.001f) {
                return false;
            }
            view.setVisibility(VISIBLE);
            view.setAlpha(alpha);
            return true;
        } else { // visible
            if (alpha > 0.001f) {
                view.setAlpha(alpha);
                return true;
            }
            view.setVisibility(INVISIBLE);
            return false;
        }
    }

    protected int floorIndexInArray(long value, long values[]) {
        int idx = -1;
        if (values.length == 0) {
            return idx;
        }
        if (values[values.length - 1] < value) {
            return values.length;
        }
        for (int i = 0; i < values.length; ++i) {
            if (values[i] <= value) {
                idx = i;
            } else {
                break;
            }
        }
        return idx;
    }

    public void setHeaderColor(int color) {
        mainChartGroup.setHeaderColor(color);
        mainChartGroup.setRangeTextColor(color);
        detailsChartGroup.setRangeTextColor(color);
    }

    public void setChartBackgroundColor(int color) {
        mainChartGroup.getChartView().setChartBackgroundColor(color);
        detailsChartGroup.getChartView().setChartBackgroundColor(color);
        mainChartGroup.getChartNavigationView().setChartBackgroundColor(color);
        detailsChartGroup.getChartNavigationView().setChartBackgroundColor(color);
    }

    public void setHintTitleTextColor(int color) {
        mainChartGroup.getChartView().setHintTitleTextColor(color);
        detailsChartGroup.getChartView().setHintTitleTextColor(color);
    }

    public void setGridLineColor(int color) {
        mainChartGroup.getChartView().setGridLineColor(color);
        detailsChartGroup.getChartView().setGridLineColor(color);
    }

    public void setGridTextColor(int color) {
        mainChartGroup.getChartView().setGridTextColor(color);
        detailsChartGroup.getChartView().setGridTextColor(color);
    }

    public void setSelectedCircleFillColor(int color) {
        mainChartGroup.getChartView().setSelectedCircleFillColor(color);
        detailsChartGroup.getChartView().setSelectedCircleFillColor(color);
    }

    public void setHintBackgroundColor(int color) {
        mainChartGroup.getChartView().setHintBackgroundColor(color);
        detailsChartGroup.getChartView().setHintBackgroundColor(color);
    }

    public void setNavigationBackgroundColor(int color) {
        mainChartGroup.getChartNavigationView().setBackgroundColor(color);
        detailsChartGroup.getChartNavigationView().setBackgroundColor(color);
    }

    public void setNavigationWindowColor(int color) {
        mainChartGroup.getChartNavigationView().setWindowColor(color);
        detailsChartGroup.getChartNavigationView().setWindowColor(color);
    }

    public ChartGroup getMainChartGroup() {
        return mainChartGroup;
    }

    public void setSimpleAnimations(boolean useSimpleAnimations) {
        this.useSimpleAnimations = useSimpleAnimations;
    }

}
