package contest.example;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import contest.datasource.ChartDataSource;
import contest.datasource.ColumnDataSource;
import contest.datasource.IntegerColumnDataSource;
import contest.utils.GeneralUtils;
import contest.view.ChartGroup;
import contest.view.ChartView;

public class MainActivity extends Activity {

    private List<ChartGroup> chartGroups = new ArrayList<>();
    private boolean darkMode = false;
    private LinearLayout settingsLayout;
    private List<CheckBox> settingCheckBoxes = new ArrayList<>();
    private List<View> separators = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setBackgroundDrawable(new ColorDrawable(0xfff0f0f0));
        if (getActionBar() != null) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(0xff517da2));
            getActionBar().setTitle(Html.fromHtml("<font color='#FFFFFF'>Statistics</font>"));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xff426382);
        }

        ScrollView scrollView = new ScrollView(this);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        chartGroups.clear();
        for (int i = 0; i < Data.chartDataSources.length; ++i) {
            ChartGroup chartGroup = new ChartGroup(this);
            chartGroup.setHeaderText("Chart #" + i);
            chartGroup.setChartDataSource(Data.chartDataSources[i]);
            chartGroups.add(chartGroup);
            linearLayout.addView(chartGroup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            addSpacer(linearLayout);
        }

        settingsLayout = new LinearLayout(this);
        settingsLayout.setOrientation(LinearLayout.VERTICAL);
        int dp8 = GeneralUtils.dp2px(this, 8);
        int dp16 = GeneralUtils.dp2px(this, 16);
        settingsLayout.setPadding(dp16, dp16, dp16, dp8);
        settingsLayout.setBackgroundColor(Color.WHITE);

        TextView settingsHeader = new TextView(this);
        settingsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        settingsHeader.setTextColor(0xff808080);
        settingsHeader.setTypeface(GeneralUtils.getBoldTypeface());
        settingsHeader.setPadding(0, dp8, dp16, dp8);
        settingsHeader.setText("Settings");
        settingsLayout.addView(settingsHeader, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addSetting("Disable chart gestures", new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (ChartGroup chartGroup: chartGroups) {
                    chartGroup.getChartView().setGesturesEnabled(!isChecked);
                }
            }
        });
        addSetting("Clip to padding", new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (ChartGroup chartGroup: chartGroups) {
                    chartGroup.getChartView().setClipToPadding(isChecked);
                }
            }
        });
        addSetting("Enable short formatting", new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (ChartDataSource chartDataSource: Data.chartDataSources) {
                    for (int c = 0; c < chartDataSource.getColumnsCount(); ++c) {
                        ColumnDataSource columnDataSource = chartDataSource.getColumn(c);
                        if (columnDataSource instanceof IntegerColumnDataSource) {
                            ((IntegerColumnDataSource) columnDataSource).setReadableFormatting(isChecked);
                        }
                    }
                }
                for (ChartGroup chartGroup: chartGroups) {
                    chartGroup.getChartView().update();
                }
            }
        });
        addSetting("Animate bottom bound", new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (ChartGroup chartGroup: chartGroups) {
                    chartGroup.getChartView().setBottomBound(isChecked ? ChartView.NO_BOUND : 0f);
                }
            }
        });

        linearLayout.addView(settingsLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addSpacer(linearLayout);

        scrollView.addView(linearLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scrollView);
    }

    private void addSetting(String title, CompoundButton.OnCheckedChangeListener listener) {
        if (!settingCheckBoxes.isEmpty()) {
            View separator = new View(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
            layoutParams.leftMargin = GeneralUtils.dp2px(this, 40);
            settingsLayout.addView(separator, layoutParams);
            separator.setBackgroundColor(0xfff5f5f5);
            separators.add(separator);
        }

        CheckBox settingCheckBox = new CheckBox(this);
        settingCheckBox.setText(" " + title);
        settingCheckBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        settingCheckBox.setTextColor(Color.BLACK);
        settingCheckBox.setOnCheckedChangeListener(listener);
        settingCheckBox.setPadding(settingCheckBox.getPaddingLeft(), GeneralUtils.dp2px(this, 12), 0, GeneralUtils.dp2px(this, 12));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settingCheckBox.setButtonTintList(ColorStateList.valueOf(0xff808080));
        }
        settingCheckBoxes.add(settingCheckBox);
        settingsLayout.addView(settingCheckBox, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addSpacer(LinearLayout linearLayout) {
        View spacer = new View(this);
        spacer.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0x10000000, 0x00000000}));
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, GeneralUtils.dp2px(this, 2));
        layoutParams.bottomMargin = GeneralUtils.dp2px(this, 24);
        linearLayout.addView(spacer, layoutParams);
    }

    private Bitmap getMoonBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(GeneralUtils.dp2px(this, 32), GeneralUtils.dp2px(this, 32), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint whitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        whitePaint.setColor(Color.WHITE);
        canvas.drawCircle(bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f, bitmap.getWidth() * 0.28f, whitePaint);
        Paint transparentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        transparentPaint.setColor(Color.TRANSPARENT);
        transparentPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        canvas.drawCircle(bitmap.getWidth() * 0.7f, bitmap.getHeight() * 0.34f, bitmap.getWidth() * 0.25f, transparentPaint);
        return bitmap;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 100, 0, ("Toggle dark mode"))
            .setIcon(new BitmapDrawable(getResources(), getMoonBitmap()))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 100) {
            toggleDarkMode();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        getWindow().setBackgroundDrawable(new ColorDrawable(darkMode ? 0xff151e27 : 0xfff0f0f0));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(darkMode ? 0xff1a242e : 0xff426382);
        }
        if (getActionBar() != null) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(darkMode ? 0xff212d3b : 0xff517da2));
            getActionBar().setTitle(Html.fromHtml("<font color='#FFFFFF'>Statistics</font>"));
        }

        settingsLayout.setBackgroundColor(darkMode ? 0xff1d2733 : 0xffffffff);
        for (CheckBox checkBox: settingCheckBoxes) {
            checkBox.setTextColor(darkMode ? Color.WHITE : Color.BLACK);
        }
        for (View separator: separators) {
            separator.setBackgroundColor(darkMode ? 0xff1a2330 : 0xfff5f5f5);
        }

        for (ChartGroup chartGroup: chartGroups) {
            chartGroup.setHeaderColor(darkMode ? 0xff7bc4fb : 0xff3896d4);
            chartGroup.setBackgroundColor(darkMode ? 0xff1d2733 : 0xffffffff);
            chartGroup.getChartView().setHintTitleTextColor(darkMode ? 0xffe5eff5 : 0xff000000);
            chartGroup.getChartView().setGridLineColor(darkMode ? 0x19ffffff : 0x12182d3b);
            chartGroup.getChartView().setGridTextColor(darkMode ? 0xffa3b1c2 : 0x99a3b1c2);
            chartGroup.getChartView().setSelectedCircleFillColor(darkMode ? 0xff1d2733 : 0xffffffff);
            chartGroup.getChartView().setHintBackgroundColor(darkMode ? 0xff202b38 : 0xffffffff);
            chartGroup.getChartNavigationView().setBackgroundColor(darkMode ? 0xa00f1e30 : 0xa0f5f6f9);
            chartGroup.getChartNavigationView().setWindowColor(darkMode ? 0x606b869b : 0x60a5bed1);
            chartGroup.getChartLegendView().setTextColor(darkMode ? Color.WHITE : Color.BLACK);
            chartGroup.getChartLegendView().setSeparatorColor(darkMode ? 0xff1a2330 : 0xfff5f5f5);
        }
    }
}
