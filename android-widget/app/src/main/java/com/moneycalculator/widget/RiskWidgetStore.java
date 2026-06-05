package com.moneycalculator.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

final class RiskWidgetStore {
    static final float MARGIN_RATE = 0.02f;
    static final float RISK_RATE = 0.01f;
    static final float STOP_MIN = 0.5f;
    static final float STOP_MAX = 15.0f;
    static final float STOP_STEP = 0.1f;
    static final float STOP_FAST_STEP = 1.0f;

    private static final String PREFS_NAME = "risk_widget_prefs";
    private static final String DEFAULT_ACCOUNT = "default_account";
    private static final String DEFAULT_STOP = "default_stop";
    private static final float FALLBACK_ACCOUNT = 100.0f;
    private static final float FALLBACK_STOP = 4.8f;

    private RiskWidgetStore() {
    }

    static WidgetState getState(Context context, int appWidgetId) {
        SharedPreferences prefs = prefs(context);
        float account = prefs.getFloat(accountKey(appWidgetId), prefs.getFloat(DEFAULT_ACCOUNT, FALLBACK_ACCOUNT));
        float stop = prefs.getFloat(stopKey(appWidgetId), prefs.getFloat(DEFAULT_STOP, FALLBACK_STOP));
        return new WidgetState(account, normalizeStop(stop));
    }

    static void saveState(Context context, int appWidgetId, float account, float stop) {
        float normalizedAccount = account > 0 ? account : FALLBACK_ACCOUNT;
        float normalizedStop = normalizeStop(stop);
        SharedPreferences.Editor editor = prefs(context).edit()
                .putFloat(DEFAULT_ACCOUNT, normalizedAccount)
                .putFloat(DEFAULT_STOP, normalizedStop);

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            editor.putFloat(accountKey(appWidgetId), normalizedAccount)
                    .putFloat(stopKey(appWidgetId), normalizedStop);
        }

        editor.apply();
    }

    static WidgetState adjustStop(Context context, int appWidgetId, float delta) {
        WidgetState state = getState(context, appWidgetId);
        WidgetState nextState = new WidgetState(state.account, normalizeStop(state.stop + delta));
        saveState(context, appWidgetId, nextState.account, nextState.stop);
        return nextState;
    }

    static void deleteState(Context context, int appWidgetId) {
        prefs(context).edit()
                .remove(accountKey(appWidgetId))
                .remove(stopKey(appWidgetId))
                .apply();
    }

    static Calculation calculate(WidgetState state) {
        float margin = state.account * MARGIN_RATE;
        float targetLoss = state.account * RISK_RATE;
        float theoreticalLeverage = targetLoss / (margin * (state.stop / 100f));
        int suggestedLeverage = (int) Math.floor(theoreticalLeverage);
        return new Calculation(margin, theoreticalLeverage, Math.max(suggestedLeverage, 1));
    }

    static String formatMoney(float value) {
        return String.format(Locale.US, "%.2fU", value);
    }

    static String formatStop(float value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    static String formatAccountLine(WidgetState state) {
        return "總資金 " + formatMoney(state.account) + " / 保證金 2%";
    }

    static float normalizeStop(float value) {
        float rounded = Math.round(value * 10f) / 10f;
        return Math.min(Math.max(rounded, STOP_MIN), STOP_MAX);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String accountKey(int appWidgetId) {
        return "account_" + appWidgetId;
    }

    private static String stopKey(int appWidgetId) {
        return "stop_" + appWidgetId;
    }

    static final class WidgetState {
        final float account;
        final float stop;

        WidgetState(float account, float stop) {
            this.account = account;
            this.stop = stop;
        }
    }

    static final class Calculation {
        final float margin;
        final float theoreticalLeverage;
        final int suggestedLeverage;

        Calculation(float margin, float theoreticalLeverage, int suggestedLeverage) {
            this.margin = margin;
            this.theoreticalLeverage = theoreticalLeverage;
            this.suggestedLeverage = suggestedLeverage;
        }
    }
}
