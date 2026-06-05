package com.moneycalculator.widget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class WidgetConfigActivity extends Activity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private EditText accountInput;
    private TextView stopValue;
    private TextView previewValue;
    private float stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
        );

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        RiskWidgetStore.WidgetState state = RiskWidgetStore.getState(this, appWidgetId);
        stop = state.stop;

        setContentView(createContentView(state));
        refreshPreview();
    }

    private View createContentView(RiskWidgetStore.WidgetState state) {
        int padding = dp(20);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(Color.rgb(9, 11, 16));

        TextView title = label("倉位桌面小工具", 24, Color.WHITE);
        title.setGravity(Gravity.START);
        root.addView(title, matchWrap());

        TextView subtitle = label("保證金固定為總資金 2%，止損距離用 +/- 每次調整 0.1%。", 14, Color.rgb(168, 176, 189));
        subtitle.setPadding(0, dp(8), 0, dp(18));
        root.addView(subtitle, matchWrap());

        TextView accountLabel = label("總資金 USDT", 14, Color.rgb(215, 221, 231));
        root.addView(accountLabel, matchWrap());

        accountInput = new EditText(this);
        accountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        accountInput.setSingleLine(true);
        accountInput.setText(String.format(Locale.US, "%.2f", state.account));
        accountInput.setTextColor(Color.WHITE);
        accountInput.setTextSize(22);
        accountInput.setSelectAllOnFocus(true);
        accountInput.setHint("100");
        accountInput.setHintTextColor(Color.rgb(126, 135, 150));
        accountInput.setPadding(dp(12), 0, dp(12), 0);
        accountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                refreshPreview();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        root.addView(accountInput, fixedHeight(56));

        LinearLayout stopRow = new LinearLayout(this);
        stopRow.setOrientation(LinearLayout.HORIZONTAL);
        stopRow.setGravity(Gravity.CENTER_VERTICAL);
        stopRow.setPadding(0, dp(18), 0, dp(8));

        Button decrease = stepButton("-");
        decrease.setOnClickListener(v -> adjustStop(-RiskWidgetStore.STOP_STEP));
        stopRow.addView(decrease, squareButton());

        stopValue = label("", 34, Color.rgb(72, 213, 255));
        stopValue.setGravity(Gravity.CENTER);
        stopRow.addView(stopValue, new LinearLayout.LayoutParams(0, dp(72), 1));

        Button increase = stepButton("+");
        increase.setOnClickListener(v -> adjustStop(RiskWidgetStore.STOP_STEP));
        stopRow.addView(increase, squareButton());

        root.addView(stopRow, matchWrap());

        previewValue = label("", 18, Color.rgb(62, 224, 139));
        previewValue.setGravity(Gravity.CENTER);
        previewValue.setPadding(0, dp(10), 0, dp(18));
        root.addView(previewValue, matchWrap());

        Button saveButton = new Button(this);
        saveButton.setText("儲存到桌面小工具");
        saveButton.setTextSize(18);
        saveButton.setAllCaps(false);
        saveButton.setOnClickListener(v -> saveAndClose());
        root.addView(saveButton, fixedHeight(56));

        return root;
    }

    private void adjustStop(float delta) {
        stop = RiskWidgetStore.normalizeStop(stop + delta);
        refreshPreview();
    }

    private void refreshPreview() {
        float account = parseAccount();
        RiskWidgetStore.WidgetState state = new RiskWidgetStore.WidgetState(account, stop);
        RiskWidgetStore.Calculation calc = RiskWidgetStore.calculate(state);
        stopValue.setText(RiskWidgetStore.formatStop(stop));
        previewValue.setText("槓桿 " + calc.suggestedLeverage + "x / 保證金 " + RiskWidgetStore.formatMoney(calc.margin));
    }

    private void saveAndClose() {
        float account = parseAccount();
        RiskWidgetStore.saveState(this, appWidgetId, account, stop);

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            RiskWidgetProvider.updateWidget(this, manager, appWidgetId);
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
        } else {
            RiskWidgetProvider.updateAllWidgets(this);
            setResult(RESULT_OK);
        }

        finish();
    }

    private float parseAccount() {
        try {
            float parsed = Float.parseFloat(accountInput.getText().toString().trim());
            return parsed > 0 ? parsed : 100f;
        } catch (NumberFormatException error) {
            return 100f;
        }
    }

    private TextView label(String text, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private Button stepButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(28);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams fixedHeight(int dp) {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(dp)
        );
    }

    private LinearLayout.LayoutParams squareButton() {
        return new LinearLayout.LayoutParams(dp(72), dp(72));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
