package contest.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.ColumnType;
import contest.utils.GeneralUtils;

/**
 * Created by Alex K on 19/03/2019.
 */
public class ChartLegendView extends LinearLayout {

    private ChartDataSource chartDataSource;
    private CompoundButton.OnCheckedChangeListener checkedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            chartDataSource.setColumnVisibility((Integer) buttonView.getTag(), isChecked);
        }
    };
    private ChartDataSource.Listener chartDataSourceListener = new ChartDataSource.Listener() {
        @Override
        public void onSetColumnVisibility(int column, boolean visible) {
            for (int c = 0; c < getChildCount(); ++c) {
                CheckBox checkBox = (CheckBox) getChildAt(c * 2);
                if (checkBox.getTag().equals(column)) {
                    checkBox.setChecked(visible);
                    break;
                }
            }
        }

        @Override
        public void onDataSetChanged() {
            update();
        }
    };
    private List<CheckBox> checkBoxes = new ArrayList<>();
    private List<View> separators = new ArrayList<>();

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
    }

    public void setChartDataSource(ChartDataSource chartDataSource) {
        this.chartDataSource = chartDataSource;
        chartDataSource.addListener(chartDataSourceListener);
        update();
    }

    private void update() {
        removeAllViews();
        separators.clear();
        checkBoxes.clear();
        for (int c = 0; c < chartDataSource.getColumnsCount(); c++) {
            ColumnDataSource columnDataSource = chartDataSource.getColumn(c);
            if (!columnDataSource.getType().equals(ColumnType.LINE)) {
                continue;
            }

            if (getChildCount() > 0) {
                View separator = new View(getContext());
                LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, 2);
                layoutParams.leftMargin = GeneralUtils.dp2px(getContext(), 40);
                this.addView(separator, layoutParams);
                separators.add(separator);
            }

            CheckBox checkBox = new CheckBox(getContext());
            checkBox.setText(columnDataSource.getName());
            checkBox.setChecked(chartDataSource.isColumnVisible(c));
            checkBox.setTag(c);
            checkBox.setOnCheckedChangeListener(checkedChangeListener);
            checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            checkBox.setPadding(checkBox.getPaddingLeft(), GeneralUtils.dp2px(getContext(), 12), 0, GeneralUtils.dp2px(getContext(), 12));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                checkBox.setButtonTintList(ColorStateList.valueOf(columnDataSource.getColor()));
            }
            checkBoxes.add(checkBox);
            this.addView(checkBox, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        setSeparatorColor(0xfff0f0f0);
        setTextColor(Color.BLACK);
    }

    public void setTextColor(int color) {
        for (CheckBox checkBox: checkBoxes) {
            checkBox.setTextColor(color);
        }
    }

    public void setSeparatorColor(int color) {
        for (View separator: separators) {
            separator.setBackgroundColor(color);
        }
    }
}
