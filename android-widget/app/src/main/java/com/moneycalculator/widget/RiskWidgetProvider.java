package com.moneycalculator.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class RiskWidgetProvider extends AppWidgetProvider {
    static final String ACTION_DECREASE_STOP = "com.moneycalculator.widget.ACTION_DECREASE_STOP";
    static final String ACTION_INCREASE_STOP = "com.moneycalculator.widget.ACTION_INCREASE_STOP";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (!ACTION_DECREASE_STOP.equals(action) && !ACTION_INCREASE_STOP.equals(action)) {
            return;
        }

        int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return;
        }

        float delta = ACTION_INCREASE_STOP.equals(action) ? RiskWidgetStore.STOP_STEP : -RiskWidgetStore.STOP_STEP;
        RiskWidgetStore.adjustStop(context, appWidgetId, delta);
        updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RiskWidgetStore.deleteState(context, appWidgetId);
        }
    }

    static void updateAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, RiskWidgetProvider.class);
        int[] ids = manager.getAppWidgetIds(componentName);
        for (int appWidgetId : ids) {
            updateWidget(context, manager, appWidgetId);
        }
    }

    static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RiskWidgetStore.WidgetState state = RiskWidgetStore.getState(context, appWidgetId);
        RiskWidgetStore.Calculation calc = RiskWidgetStore.calculate(state);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_risk_calculator);
        views.setTextViewText(R.id.stopValue, RiskWidgetStore.formatStop(state.stop));
        views.setTextViewText(R.id.leverageValue, calc.suggestedLeverage + "x");
        views.setTextViewText(R.id.marginValue, "保證金 " + RiskWidgetStore.formatMoney(calc.margin));
        views.setTextViewText(R.id.accountValue, RiskWidgetStore.formatAccountLine(state));

        views.setOnClickPendingIntent(
                R.id.decreaseStop,
                stopPendingIntent(context, appWidgetId, ACTION_DECREASE_STOP, 10_000 + appWidgetId)
        );
        views.setOnClickPendingIntent(
                R.id.increaseStop,
                stopPendingIntent(context, appWidgetId, ACTION_INCREASE_STOP, 20_000 + appWidgetId)
        );
        views.setOnClickPendingIntent(
                R.id.widgetTitle,
                configPendingIntent(context, appWidgetId, 30_000 + appWidgetId)
        );
        views.setOnClickPendingIntent(
                R.id.accountValue,
                configPendingIntent(context, appWidgetId, 40_000 + appWidgetId)
        );

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent stopPendingIntent(Context context, int appWidgetId, String action, int requestCode) {
        Intent intent = new Intent(context, RiskWidgetProvider.class)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, requestCode, intent, flags());
    }

    private static PendingIntent configPendingIntent(Context context, int appWidgetId, int requestCode) {
        Intent intent = new Intent(context, WidgetConfigActivity.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getActivity(context, requestCode, intent, flags());
    }

    private static int flags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }
}
