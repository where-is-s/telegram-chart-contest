package contest.example;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import contest.datasource.ChartDataSource;
import contest.datasource.SimpleChartDataSource;
import contest.utils.GeneralUtils;
import contest.view.BaseDetailsChartGroup;
import contest.view.PieDetailsChartGroup;
import contest.view.ThreeDayDetailsChartGroup;
import contest.view.WeekDetailsChartGroup;
import telegram.contest.chart.R;

public class MainActivity extends Activity {

    private List<BaseDetailsChartGroup> chartGroups = new ArrayList<>();
    private boolean darkMode = false;
    private LinearLayout settingsLayout;
    private List<CheckBox> settingCheckBoxes = new ArrayList<>();
    private List<View> separators = new ArrayList<>();
    private LinearLayout linearLayout;
    private List<FrameLayout> spacers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDarkMode(false);

        ScrollView scrollView = new ScrollView(this);

        linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        chartGroups.clear();
        addSpacer(linearLayout);
        addDetailsChartGroup("Followers", new WeekDetailsChartGroup(this),
                Data_1.chartDataSource, "Data_1b", Data_1b.chartDataSource);
        addDetailsChartGroup("Interactions", new WeekDetailsChartGroup(this),
                Data_2.chartDataSource, "Data_2b", Data_2b.chartDataSource);
        addDetailsChartGroup("Fruits", new WeekDetailsChartGroup(this),
                Data_3.chartDataSource, "Data_3b", Data_3b.chartDataSource);
        addDetailsChartGroup("Views", new ThreeDayDetailsChartGroup(this),
                Data_4.chartDataSource, "Data_4b", Data_4b.chartDataSource);
        addDetailsChartGroup("Fruits Ratio", new PieDetailsChartGroup(this),
                Data_5.chartDataSource, "Data_5b", Data_5b.chartDataSource);

        settingsLayout = new LinearLayout(this);
        settingsLayout.setOrientation(LinearLayout.VERTICAL);
        int dp8 = GeneralUtils.dp2px(this, 8);
        int dp16 = GeneralUtils.dp2px(this, 16);
        settingsLayout.setPadding(dp16, dp16, dp16, dp8);

        TextView settingsHeader = new TextView(this);
        settingsHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        settingsHeader.setTextColor(0xff808080);
        settingsHeader.setTypeface(GeneralUtils.getBoldTypeface());
        settingsHeader.setPadding(0, dp8, dp16, dp8);
        settingsHeader.setText("Settings");
        settingsLayout.addView(settingsHeader, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        addSetting("Detailed zoom transitions (slower)", new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (BaseDetailsChartGroup chartGroup: chartGroups) {
                    chartGroup.setSimpleAnimations(!isChecked);
                }
            }
        });
        addSetting("Disable main chart gestures", new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (BaseDetailsChartGroup chartGroup: chartGroups) {
                    chartGroup.getMainChartGroup().getChartView().setGesturesEnabled(!isChecked);
                }
            }
        });

        linearLayout.addView(settingsLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        View spacer = addSpacer(linearLayout);
        ViewGroup.LayoutParams layoutParams = spacer.getLayoutParams();
        layoutParams.height = GeneralUtils.dp2px(this, 60);
        spacer.setLayoutParams(layoutParams);

        TextView signature = new TextView(this);
        signature.setText("by Alex K, 2019");
        signature.setTextColor(0x80808080);
        signature.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        FrameLayout.LayoutParams signatureLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        signatureLayoutParams.gravity = Gravity.CENTER;
        ((FrameLayout) spacer).addView(signature, signatureLayoutParams);

        scrollView.addView(linearLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(scrollView);
    }

    private void addDetailsChartGroup(String name, BaseDetailsChartGroup detailsChartGroup, ChartDataSource mainDataSource, String assetBaseName, SimpleChartDataSource detailsDataSource) {
        chartGroups.add(detailsChartGroup);
        detailsChartGroup.setHeaderText(name);
        detailsChartGroup.setChartDataSource(mainDataSource, assetBaseName, detailsDataSource);
        linearLayout.addView(detailsChartGroup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addSpacer(linearLayout);
    }

    private void addSetting(String title, CompoundButton.OnCheckedChangeListener listener) {
        if (!settingCheckBoxes.isEmpty()) {
            View separator = new View(this);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2);
            layoutParams.leftMargin = GeneralUtils.dp2px(this, 40);
            settingsLayout.addView(separator, layoutParams);
            separator.setBackgroundColor(0xfff0f0f0);
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

    private FrameLayout addSpacer(LinearLayout linearLayout) {
        FrameLayout spacer = new FrameLayout(this);
        View shadowView = new View(this);
        shadowView.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[] {0x10000000, 0x00000000}));
        spacer.addView(shadowView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, GeneralUtils.dp2px(this, 2)));
        spacer.setBackgroundColor(0xfff0f0f0);
        linearLayout.addView(spacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, GeneralUtils.dp2px(this, 24)));
        spacers.add(spacer);
        return spacer;
    }

    private Bitmap getNightBitmap() {
        return ((BitmapDrawable) getResources().getDrawable(darkMode ? R.drawable.night_white : R.drawable.night)).getBitmap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 100, 0, ("Toggle dark mode"))
            .setIcon(new BitmapDrawable(getResources(), getNightBitmap()))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 100) {
            setDarkMode(!darkMode);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        getWindow().setBackgroundDrawable(new ColorDrawable(darkMode ? 0xff242f3e : 0xffffffff));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int flags = getWindow().getDecorView().getSystemUiVisibility();
                if (darkMode) {
                    flags ^= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                getWindow().getDecorView().setSystemUiVisibility(flags);
                getWindow().setStatusBarColor(darkMode ? 0xff242f3e : 0xffffffff);
            } else {
                getWindow().setStatusBarColor(darkMode ? 0xff242f3e : 0xffc0c0c0);
            }
        }
        if (getActionBar() != null) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(darkMode ? 0xff242f3e : 0xffffffff));
            getActionBar().setTitle(Html.fromHtml("<font color='#" + (darkMode ? "FFFFFF" : "000000") + "'>Statistics</font>"));
        }
        invalidateOptionsMenu();

        for (CheckBox checkBox: settingCheckBoxes) {
            checkBox.setTextColor(darkMode ? Color.WHITE : Color.BLACK);
        }
        for (View separator: separators) {
            separator.setBackgroundColor(darkMode ? 0xff1b2433 : 0xfff0f0f0);
        }

        for (FrameLayout spacer: spacers) {
            spacer.setBackgroundColor(darkMode ? 0xff1b2433 : 0xfff0f0f0);
        }

        for (BaseDetailsChartGroup chartGroup: chartGroups) {
            chartGroup.setHeaderColor(darkMode ? 0xffffffff : 0xff000000);
            chartGroup.setChartBackgroundColor(darkMode ? 0xff242f3e : 0xffffffff);
            chartGroup.setHintTitleTextColor(darkMode ? 0xffffffff : 0xff000000);
            chartGroup.setGridLineColor(darkMode ? 0x19ffffff : 0x12182d3b); // TODO
            chartGroup.setGridTextColor(darkMode ? 0xffa3b1c2 : 0x99a3b1c2); // TODO
            chartGroup.setSelectedCircleFillColor(darkMode ? 0xff1b2433 : 0xfff0f0f0);
            chartGroup.setHintBackgroundColor(darkMode ? 0xff1c2533 : 0xffffffff);
            chartGroup.setNavigationBackgroundColor(darkMode ? 0xa00f1e30 : 0xa0f5f6f9);
            chartGroup.setNavigationWindowColor(darkMode ? 0x606b869b : 0x60a5bed1);
        }
    }
}
