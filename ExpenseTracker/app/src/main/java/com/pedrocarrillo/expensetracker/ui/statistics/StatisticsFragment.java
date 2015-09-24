package com.pedrocarrillo.expensetracker.ui.statistics;

import android.animation.PropertyValuesHolder;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TextView;

import com.db.chart.Tools;
import com.db.chart.model.Bar;
import com.db.chart.model.BarSet;
import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.BarChartView;
import com.db.chart.view.LineChartView;
import com.db.chart.view.Tooltip;
import com.db.chart.view.XController;
import com.db.chart.view.YController;
import com.db.chart.view.animation.Animation;
import com.pedrocarrillo.expensetracker.R;
import com.pedrocarrillo.expensetracker.entities.Category;
import com.pedrocarrillo.expensetracker.entities.Expense;
import com.pedrocarrillo.expensetracker.interfaces.IExpensesType;
import com.pedrocarrillo.expensetracker.ui.MainActivity;
import com.pedrocarrillo.expensetracker.ui.MainFragment;
import com.pedrocarrillo.expensetracker.utils.DateUtils;
import com.pedrocarrillo.expensetracker.utils.DialogManager;
import com.pedrocarrillo.expensetracker.utils.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by pcarrillo on 17/09/2015.
 */
public class StatisticsFragment extends MainFragment implements View.OnClickListener {

    private TextView tvDateFrom;
    private TextView tvDateTo;
    private BarChartView bcvCategories;
    private LineChartView lcvExpenses;

    private Date mDateFrom;
    private Date mDateTo;
    private List<Float> valuesPerCategory;

    public static StatisticsFragment newInstance() {
        return new StatisticsFragment();
    }

    public StatisticsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainActivityListener.setMode(MainActivity.NAVIGATION_MODE_STANDARD);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_statistics, container, false);
        tvDateFrom = (TextView)rootView.findViewById(R.id.tv_date_from);
        tvDateTo = (TextView)rootView.findViewById(R.id.tv_date_to);
        bcvCategories = (BarChartView) rootView.findViewById(R.id.chartCategories);
        lcvExpenses = (LineChartView) rootView.findViewById(R.id.chartExpenses);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tvDateFrom.setOnClickListener(this);
        tvDateTo.setOnClickListener(this);
        mDateFrom = DateUtils.getFirstDateOfCurrentWeek();
        mDateTo = DateUtils.getTomorrowDate();
        updateDate(tvDateFrom, mDateFrom);
        updateDate(tvDateTo, mDateTo);
        setCategoryChart();
//        setExpensesChart();
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.tv_date_from || v.getId() == R.id.tv_date_to) {
            showDateDialog(v.getId());
        }
    }

    private void showDateDialog(final int id) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        DialogManager.getInstance()
                .showDatePicker(
                        getActivity(),
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                                calendar.set(year, month, day);
                                if (id == R.id.tv_date_from) {
                                    mDateFrom = calendar.getTime();
                                    updateDate(tvDateFrom, mDateFrom);
                                } else {
                                    mDateTo = calendar.getTime();
                                    updateDate(tvDateTo, mDateTo);
                                }
                                bcvCategories.dismissAllTooltips();
                                bcvCategories.reset();
                                setCategoryChart();
                            }
                        },
                        calendar,
                        (R.id.tv_date_from == id) ? null : mDateTo,
                        (R.id.tv_date_from == id) ? mDateTo : null);
    }

    private void updateDate(TextView tv, Date date) {
        tv.setText(Util.formatDateToString(date, "MM/dd/yyyy"));
    }

    private void setCategoryChart() {
        List<Category> categoryList = Category.getCategoriesExpense();

        Runnable action =  new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        if( getActivity() != null) showTooltipCategoriesChart();
                    }
                }, 500);
            }
        };

        Tooltip tip = new Tooltip(getActivity(), R.layout.tooltip_bar_chart);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            tip.setEnterAnimation(PropertyValuesHolder.ofFloat(View.ALPHA, 1));
            tip.setExitAnimation(PropertyValuesHolder.ofFloat(View.ALPHA,0));
        }

        bcvCategories.setTooltips(tip);
        BarSet barSet = new BarSet();
        valuesPerCategory = new ArrayList<>();

        int[] order = new int[categoryList.size()];
        int pos = 0;
        for (Category category : categoryList) {
            float value = Expense.getCategoryTotalByDate(mDateFrom, mDateTo, category);
            valuesPerCategory.add(value);
            order[pos] = pos++;
            barSet.addBar(new Bar(category.getName(), value));
        }
        barSet.setColor(getResources().getColor(R.color.colorPrimaryLight));
        bcvCategories.setSetSpacing(Tools.fromDpToPx(-15));
        bcvCategories.setRoundCorners(Tools.fromDpToPx(2));
        bcvCategories.addData(barSet);
        bcvCategories.setBarSpacing(Tools.fromDpToPx(35));
        int maxValue = Math.round(Collections.max(valuesPerCategory));
        bcvCategories.setBorderSpacing(0)
                .setAxisBorderValues(0, maxValue, 10)
                .setAxisColor(getResources().getColor(R.color.grey))
                .setLabelsColor(getResources().getColor(R.color.colorPrimaryDark))
                .setYAxis(false)
                .setYLabels(YController.LabelPosition.NONE)
                .setXLabels(XController.LabelPosition.OUTSIDE);
        bcvCategories.setHorizontalScrollBarEnabled(true);
        Collections.shuffle(Arrays.asList(order));
        bcvCategories.show(new Animation()
                .setOverlap(.3f, order)
                .setEndAction(action));
    }

    private void showTooltipCategoriesChart(){
        ArrayList<ArrayList<Rect>> areas = new ArrayList<>();
        areas.add(bcvCategories.getEntriesArea(0));

        for(int i = 0; i < areas.size(); i++) {
            for (int j = 0; j < areas.get(i).size(); j++) {
                Tooltip tooltip = new Tooltip(getActivity(), R.layout.tooltip_bar_chart, R.id.value);
                tooltip.prepare(areas.get(i).get(j), valuesPerCategory.get(j));
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    tooltip.setEnterAnimation(PropertyValuesHolder.ofFloat(View.ALPHA, 1));
                    tooltip.setExitAnimation(PropertyValuesHolder.ofFloat(View.ALPHA, 0));
                }
                bcvCategories.showTooltip(tooltip, true);
            }
        }

    }

    private void setExpensesChart() {
        List<Category> categoryList = Category.getCategoriesExpense();
        Runnable action =  new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
//                        if( getActivity() != null) showTooltipCategoriesChart();
                    }
                }, 500);
            }
        };

        Tooltip tip = new Tooltip(getActivity(), R.layout.tooltip_bar_chart);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            tip.setEnterAnimation(PropertyValuesHolder.ofFloat(View.ALPHA, 1));
            tip.setExitAnimation(PropertyValuesHolder.ofFloat(View.ALPHA,0));
        }

        lcvExpenses.setTooltips(tip);
        String[] categoriesNames = new String[categoryList.size()];
        for (int pos=0; pos<categoryList.size(); pos++) {
            categoriesNames[pos] = categoryList.get(pos).getName();
        }

        for (Category category : categoryList) {
            List<Expense> expenseList = Expense.getExpensesList(mDateFrom, DateUtils.addDaysToDate(mDateTo, 1), null, category);
            float[] expenseValues = new float[expenseList.size()];
            for(int i=0; i<expenseList.size(); i++) {
                Expense expense = expenseList.get(i);
                expenseValues[i] = expense.getType() == IExpensesType.MODE_INCOME ? -expense.getTotal() : expense.getTotal();
            }
            if( expenseValues.length > 0) {
                LineSet dataset = new LineSet(categoriesNames, expenseValues);
                dataset
//                    .setColor(Color.parseColor("#a34545"))
//                    .setFill(Color.parseColor("#a34545"))
                        .setSmooth(true);
                lcvExpenses.addData(dataset);
            }
        }

        lcvExpenses.setTopSpacing(Tools.fromDpToPx(15))
                .setBorderSpacing(Tools.fromDpToPx(0))
                .setAxisBorderValues(0, 10, 1)
                .setXLabels(AxisController.LabelPosition.INSIDE)
                .setYLabels(AxisController.LabelPosition.NONE)
                .setLabelsColor(Color.parseColor("#e08b36"))
                .setXAxis(false)
                .setYAxis(false);
        Animation anim = new Animation().setStartPoint(-1, 1).setEndAction(action);

        lcvExpenses.show(anim);

    }
}