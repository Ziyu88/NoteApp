package com.calculator.notepadapp;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.calculator.notepadapp.adapter.CategoryAdapter;
import com.calculator.notepadapp.dao.CategoryDao;
import com.calculator.notepadapp.dao.NoteDao;
import com.calculator.notepadapp.model.Category;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CategoryActivity extends AppCompatActivity {

    private static final String TAG = "CategoryActivity";

    private RecyclerView recyclerView;
    private CategoryAdapter adapter;
    private FloatingActionButton fabAddCategory;
    private ImageButton buttonBack;
    private CategoryDao categoryDao;
    private NoteDao noteDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 应用主题设置
        ThemeHelper.applyTheme(this);

        // 设置状态栏颜色为白色
        setStatusBarColor();

        setContentView(R.layout.activity_category);

        // 初始化视图
        recyclerView = findViewById(R.id.recyclerViewCategories);
        fabAddCategory = findViewById(R.id.fabAddCategory);
        buttonBack = findViewById(R.id.buttonBack);

        // 初始化数据库
        NoteDatabase database = NoteDatabase.getInstance(this);
        categoryDao = database.categoryDao();
        noteDao = database.noteDao();

        // 设置RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter();
        recyclerView.setAdapter(adapter);

        // 加载分类列表
        loadCategories();

        // 返回按钮
        buttonBack.setOnClickListener(v -> finish());

        // 添加分类按钮
        fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // 分类点击事件
        adapter.setOnCategoryClickListener(category -> {
            // 返回选中的分类
            Intent intent = new Intent();
            intent.putExtra("CATEGORY_ID", category.id);
            intent.putExtra("CATEGORY_NAME", category.name);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    /**
     * 加载分类列表
     */
    private void loadCategories() {
        categoryDao.getAllCategories().observe(this, new Observer<List<Category>>() {
            @Override
            public void onChanged(List<Category> categories) {
                if (categories != null) {
                    Log.d(TAG, "加载分类数量: " + categories.size());
                    
                    // 添加"全部"和"未分类"选项
                    List<Category> allCategories = new ArrayList<>();
                    
                    // 全部
                    Category allCategory = new Category("全部", 0);
                    allCategory.id = -1;
                    allCategories.add(allCategory);
                    
                    // 用户创建的分类
                    allCategories.addAll(categories);
                    
                    // 未分类
                    Category uncategorized = new Category("未分类", 0);
                    uncategorized.id = 0;
                    allCategories.add(uncategorized);
                    
                    // 计算每个分类的笔记数量
                    new Thread(() -> {
                        List<Long> counts = new ArrayList<>();
                        for (Category category : allCategories) {
                            Long count;
                            if (category.id == -1) {
                                // 全部笔记
                                count = noteDao.countAllNotes();
                            } else {
                                count = Long.valueOf(noteDao.countNotesByCategory(category.id));
                            }
                            counts.add(count);
                        }

                        runOnUiThread(() -> {
                            adapter.setCategoryList(allCategories, counts);
                        });
                    }).start();
                }
            }
        });
    }

    /**
     * 显示添加分类对话框
     */
    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新建文件夹");

        // 创建输入框
        final EditText input = new EditText(this);
        input.setHint("请输入文件夹名称");
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            if (TextUtils.isEmpty(categoryName)) {
                Toast.makeText(this, "文件夹名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查是否重名
            new Thread(() -> {
                int count = categoryDao.countCategoriesByName(categoryName);
                if (count > 0) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "文件夹名称已存在", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // 创建分类
                Category category = new Category(categoryName, System.currentTimeMillis());
                categoryDao.insert(category);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "创建成功", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "创建分类: " + categoryName);
                });
            }).start();
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 设置状态栏为浅色背景，深色图标
     */
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }
}

