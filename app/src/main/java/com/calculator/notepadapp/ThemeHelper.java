package com.calculator.notepadapp;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * 主题管理帮助类
 * 管理日间模式、夜间模式、跟随系统三种主题
 */
public class ThemeHelper {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // 主题模式常量
    public static final int MODE_LIGHT = 0;   // 日间模式
    public static final int MODE_DARK = 1;    // 夜间模式
    public static final int MODE_SYSTEM = 2;  // 跟随系统

    /**
     * 应用保存的主题设置
     * 应在 Application 或 Activity 启动时调用
     */
    public static void applyTheme(Context context) {
        int mode = getThemeMode(context);
        applyThemeMode(mode);
    }

    /**
     * 设置并应用主题模式
     */
    public static void setThemeMode(Context context, int mode) {
        // 保存设置
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        
        // 应用主题
        applyThemeMode(mode);
    }

    /**
     * 获取当前保存的主题模式
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM); // 默认跟随系统
    }

    /**
     * 应用主题模式
     */
    private static void applyThemeMode(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case MODE_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * 获取主题模式的显示名称
     */
    public static String getThemeModeName(int mode) {
        switch (mode) {
            case MODE_LIGHT:
                return "☀️ 日间模式";
            case MODE_DARK:
                return "🌙 夜间模式";
            case MODE_SYSTEM:
            default:
                return "🌓 跟随系统";
        }
    }
}

