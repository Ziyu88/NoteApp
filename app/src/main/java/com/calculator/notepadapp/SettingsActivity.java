package com.calculator.notepadapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 设置页面
 * 包含主题切换等设置项
 */
public class SettingsActivity extends AppCompatActivity {

    private RadioButton radioLightMode;
    private RadioButton radioDarkMode;
    private RadioButton radioSystemMode;
    private TextView textCurrentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 初始化控件
        initViews();
        
        // 加载当前主题设置
        loadCurrentTheme();
        
        // 设置点击事件
        setupClickListeners();
    }

    private void initViews() {
        // 返回按钮
        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish());

        // 主题选项
        radioLightMode = findViewById(R.id.radioLightMode);
        radioDarkMode = findViewById(R.id.radioDarkMode);
        radioSystemMode = findViewById(R.id.radioSystemMode);
        textCurrentTheme = findViewById(R.id.textCurrentTheme);
    }

    private void loadCurrentTheme() {
        int currentMode = ThemeHelper.getThemeMode(this);
        updateRadioButtons(currentMode);
        updateCurrentThemeText(currentMode);
    }

    private void setupClickListeners() {
        // 日间模式
        LinearLayout optionLightMode = findViewById(R.id.optionLightMode);
        optionLightMode.setOnClickListener(v -> {
            applyThemeMode(ThemeHelper.MODE_LIGHT);
        });

        // 夜间模式
        LinearLayout optionDarkMode = findViewById(R.id.optionDarkMode);
        optionDarkMode.setOnClickListener(v -> {
            applyThemeMode(ThemeHelper.MODE_DARK);
        });

        // 跟随系统
        LinearLayout optionSystemMode = findViewById(R.id.optionSystemMode);
        optionSystemMode.setOnClickListener(v -> {
            applyThemeMode(ThemeHelper.MODE_SYSTEM);
        });
    }

    private void applyThemeMode(int mode) {
        // 更新UI
        updateRadioButtons(mode);
        updateCurrentThemeText(mode);
        
        // 应用主题
        ThemeHelper.setThemeMode(this, mode);
    }

    private void updateRadioButtons(int mode) {
        radioLightMode.setChecked(mode == ThemeHelper.MODE_LIGHT);
        radioDarkMode.setChecked(mode == ThemeHelper.MODE_DARK);
        radioSystemMode.setChecked(mode == ThemeHelper.MODE_SYSTEM);
    }

    private void updateCurrentThemeText(int mode) {
        String themeName = ThemeHelper.getThemeModeName(mode);
        textCurrentTheme.setText("当前主题：" + themeName);
    }
}

