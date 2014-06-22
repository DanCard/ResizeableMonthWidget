package dancar.resizeablemonthwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.DateFormatSymbols;
import java.util.Calendar;

/**
 * Draw the widget
 */
public class ResizeableMonthWidget extends AppWidgetProvider {
    private static final String ACTION_PREVIOUS_MONTH= "dancar.ResizeableMonthWidget.action.PREVIOUS_MONTH";
    private static final String ACTION_NEXT_MONTH    = "dancar.ResizeableMonthWidget.action.NEXT_MONTH";
    private static final String ACTION_RESET_MONTH   = "dancar.ResizeableMonthWidget.action.RESET_MONTH";

    private static final String[] WEEKDAYS = DateFormatSymbols.getInstance().getShortWeekdays();
    private static final String PREF_MONTH = "month";
    private static final String PREF_YEAR = "year";

    private static final int NUM_CELLS_1 =  40;
    private static final int NUM_CELLS_2 = 110;
    private static final int NUM_CELLS_3 = 180;
    private static final int NUM_CELLS_4 = 250;


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    private void redrawWidgets(Context context) {
        int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, ResizeableMonthWidget.class));
        for (int appWidgetId : appWidgetIds) {
            drawWidget(context, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_RESET_MONTH.equals(action)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().remove(PREF_MONTH).remove(PREF_YEAR).apply();
        } else {
            //Log.d(context.toString(), intent.toString());
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Calendar cal = Calendar.getInstance();
            int thisMonth = sp.getInt(PREF_MONTH, cal.get(Calendar.MONTH));
            int thisYear = sp.getInt(PREF_YEAR, cal.get(Calendar.YEAR));
            cal.set(Calendar.MONTH, thisMonth);
            if (ACTION_PREVIOUS_MONTH.equals(action)) {
                cal.add(Calendar.MONTH, -1);
            } else if (ACTION_NEXT_MONTH.equals(action)) {
                cal.add(Calendar.MONTH, 1);
            }
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.YEAR, thisYear);
            sp.edit()
                    .putInt(PREF_MONTH, cal.get(Calendar.MONTH))
                    .putInt(PREF_YEAR, cal.get(Calendar.YEAR))
                    .apply();
        }
        redrawWidgets(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        drawWidget(context, appWidgetId);
    }

    private void drawWidget(Context context, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
        boolean shortMonthName = false;
        boolean width1Col = false;
        boolean oneWeekView = false;
        boolean useCurrentMonth = true;
        boolean oneByOne = false;
        boolean startAtFirstOfMonth = false;
        int numWeeks = 6;
        if (widgetOptions != null) {
            int minWidthDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int minHeightDp = widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            shortMonthName = minWidthDp < NUM_CELLS_2;
            width1Col = minWidthDp < NUM_CELLS_2;
            boolean heightLessThan3 = minHeightDp < NUM_CELLS_3;
            if (heightLessThan3) {
                numWeeks = minHeightDp < NUM_CELLS_2 ? 1 : 2;
            }
            oneWeekView = heightLessThan3;
            oneByOne = width1Col && numWeeks == 1;
            useCurrentMonth = oneByOne || oneWeekView || width1Col;
            startAtFirstOfMonth = numWeeks == 6 && !width1Col;
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);

        Calendar cal = Calendar.getInstance();
        int today = cal.get(Calendar.DAY_OF_YEAR);
        int todayDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int currentYear = cal.get(Calendar.YEAR);
        int thisMonth;
        if (useCurrentMonth) {
            thisMonth = cal.get(Calendar.MONTH);
        } else {
            thisMonth = sp.getInt(PREF_MONTH, cal.get(Calendar.MONTH));
            int thisYear = sp.getInt(PREF_YEAR, cal.get(Calendar.YEAR));
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.MONTH, thisMonth);
            cal.set(Calendar.YEAR, thisYear);
        }
        rv.setTextViewText(R.id.month_label, DateFormat.format(
                shortMonthName ? "MMM yy" : "MMMM yyyy", cal));

        if (!oneByOne) {
            // Set the first day we start displaying the calendar
            if (startAtFirstOfMonth) {
                cal.set(Calendar.DAY_OF_MONTH, 1);
            } else if (oneWeekView) {
                cal.add(Calendar.DAY_OF_MONTH, 1 - todayDayOfWeek);
            } else if (width1Col) {
                cal.set(Calendar.DAY_OF_MONTH, todayDayOfWeek);
            }
            if (startAtFirstOfMonth || width1Col) {
                int monthStartDayOfWeek = todayDayOfWeek;
                cal.add(Calendar.DAY_OF_MONTH, 1 - monthStartDayOfWeek);
            }
        }

        rv.removeAllViews(R.id.calendar);

        RemoteViews headerRowRv = new RemoteViews(context.getPackageName(), R.layout.row_header);
        if (width1Col) {
            RemoteViews remoteViews = setRemoteViewText(context.getPackageName(), headerRowRv, WEEKDAYS[todayDayOfWeek]);
            remoteViews.setTextViewTextSize(android.R.id.text1, TypedValue.COMPLEX_UNIT_SP, (float) 18.0);
            //remoteViews.setText
        } else {
            for (int day = Calendar.SUNDAY; day <= Calendar.SATURDAY; day++) {
                setRemoteViewText(context.getPackageName(), headerRowRv, WEEKDAYS[day]);
            }
        }
        rv.addView(R.id.calendar, headerRowRv);

        for (int week = 0; week < numWeeks; week++) {
            RemoteViews rowRv = new RemoteViews(context.getPackageName(), R.layout.row_week);
            for (int day = 1; day <= 7; day++) {
                boolean inMonth = cal.get(Calendar.MONTH) == thisMonth;
                boolean inYear  = cal.get(Calendar.YEAR) == currentYear;
                boolean isToday = inYear && inMonth && (cal.get(Calendar.DAY_OF_YEAR) == today);
                boolean isFirstOfMonth = cal.get(Calendar.DAY_OF_MONTH) == 1;

                int cellLayoutResId = R.layout.cell_day;
                if (isToday) {
                    cellLayoutResId = R.layout.cell_today;
                } else if (inMonth) {
                    cellLayoutResId = R.layout.cell_day_this_month;
                }
                if (!width1Col || day == todayDayOfWeek) {
                    RemoteViews cellRv = new RemoteViews(context.getPackageName(), cellLayoutResId);
                    cellRv.setTextViewText(android.R.id.text1,
                            Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
                    if (isFirstOfMonth) {
                        cellRv.setTextViewText(R.id.month_label, DateFormat.format("MMM", cal));
                    }
                    rowRv.addView(R.id.row_week, cellRv);
                }
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
            rv.addView(R.id.calendar, rowRv);
        }

        boolean showMonthNavigationButtons = numWeeks > 2 && !width1Col;
        rv.setViewVisibility(R.id.prev_month_button, showMonthNavigationButtons ? View.VISIBLE : View.GONE);
        rv.setOnClickPendingIntent(R.id.prev_month_button, PendingIntent.getBroadcast(context, 0,
            new Intent(context, ResizeableMonthWidget.class).setAction(ACTION_PREVIOUS_MONTH),
                        PendingIntent.FLAG_UPDATE_CURRENT));
        rv.setViewVisibility(R.id.next_month_button, showMonthNavigationButtons ? View.VISIBLE : View.GONE);
        rv.setOnClickPendingIntent(R.id.next_month_button, PendingIntent.getBroadcast(context, 0,
            new Intent(context, ResizeableMonthWidget.class).setAction(ACTION_NEXT_MONTH),
                        PendingIntent.FLAG_UPDATE_CURRENT));
        rv.setOnClickPendingIntent(R.id.month_label, PendingIntent.getBroadcast(context, 0,
            new Intent(context, ResizeableMonthWidget.class).setAction(ACTION_RESET_MONTH),
                        PendingIntent.FLAG_UPDATE_CURRENT));
        rv.setViewVisibility(R.id.month_bar, numWeeks > 1 || width1Col ? View.VISIBLE : View.GONE);
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    private RemoteViews setRemoteViewText(String packageName, RemoteViews headerRowRv, String text) {
        RemoteViews dayRemoteViews = new RemoteViews(packageName, R.layout.cell_header);
        dayRemoteViews.setTextViewText(android.R.id.text1, text);
        headerRowRv.addView(R.id.row_header, dayRemoteViews);
        return dayRemoteViews;
    }

}
