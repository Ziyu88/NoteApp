package com.calculator.notepadapp;

import android.app.Activity;
import android.content.Intent;import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.HorizontalScrollView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.calculator.notepadapp.adapter.NoteAdapter;
import com.calculator.notepadapp.model.Note;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ADD_NOTE = 1;
    private static final int REQUEST_EDIT_NOTE = 2;
    private static final int REQUEST_CATEGORY = 3;
    private static final int REQUEST_MOVE_TO_CATEGORY = 4;

    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private NoteDatabase noteDatabase;
    private NoteDeleteManager deleteManager;
    private FloatingActionButton fabAdd;
    private FloatingActionButton fabDelete;
    private ImageButton buttonCategory;
    private ImageButton buttonSettings;
    private ImageButton buttonExitMultiSelect;
    private ImageButton buttonMultiSelectIcon;
    private TextView textSelectedCount;
    private TextView textAppTitle;
    private LinearLayout bottomActionBar;
    private LinearLayout actionPin;
    private LinearLayout actionMove;
    private LinearLayout actionDelete;
    private boolean isMultiSelectMode = false;
    private int currentCategoryId = -1; // -1表示显示全部，0表示未分类

    // 搜索相关
    private EditText editTextSearch;
    private ImageButton buttonClearSearch;
    private String currentSearchQuery = ""; // 当前搜索关键词
    // 标记当前是否在代码中更新搜索框文本，避免 TextWatcher 形成递归
    private boolean isUpdatingSearchText = false;

    // 分类条容器
    private LinearLayout categoryChipContainer;


    // 顶部显示/隐藏相关
    private View searchContainer;
    private HorizontalScrollView categoryScroll;
    private TextView textLargeTitle;
    private boolean topBarsHidden = false;
    private int hiddenAreaHeight = 0; // 大标题+搜索框的高度

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 应用主题设置
        ThemeHelper.applyTheme(this);

        // 设置状态栏颜色
        setStatusBarColor();

        setContentView(R.layout.activity_main); // 绑定 activity_main.xml

        // 启动回收站自动清理任务（每天执行一次）
        scheduleTrashCleanupWork();

        // 初始化删除管理器
        noteDatabase = NoteDatabase.getInstance(this);
        deleteManager = new NoteDeleteManager(this, noteDatabase.noteDao());

        // 初始化 RecyclerView
        recyclerView = findViewById(R.id.recyclerViewNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter();
        recyclerView.setAdapter(adapter);

        // 设置适配器点击事件
        adapter.setOnItemClickListener((note, position) -> {
            if (isMultiSelectMode) {
                // 多选模式下，点击切换选中状态
                updateDeleteButtonText();
            } else {
                // 普通模式下，点击进入编辑页面
                Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
                intent.putExtra("NOTE_ID", note.id);
                startActivityForResult(intent, REQUEST_EDIT_NOTE);
            }
        });

        // 设置适配器长按事件
        adapter.setOnItemLongClickListener((note, position) -> {
            enterMultiSelectMode();
            adapter.toggleSelection(position);
            updateDeleteButtonText();
        });

        // 设置删除按钮点击事件
        adapter.setOnDeleteClickListener((note, position) -> {
            // 点击删除按钮后弹出确认对话框
            deleteManager.deleteSingleNote(note, new NoteDeleteManager.OnDeleteListener() {
                @Override
                public void onDeleteSuccess() {
                    // 删除成功
                }

                @Override
                public void onDeleteCancelled() {
                    // 取消删除，不需要特殊处理
                }
            });
        });

        // 初始化搜索框
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonClearSearch = findViewById(R.id.buttonClearSearch);
        setupSearchBox();

        // 分类条容器
        categoryChipContainer = findViewById(R.id.categoryChipContainer);
        setupCategoryChips();


        // 获取顶部容器引用
        searchContainer = findViewById(R.id.searchContainer);
        categoryScroll = findViewById(R.id.categoryScroll);
        textLargeTitle = findViewById(R.id.textLargeTitle);
        textAppTitle = findViewById(R.id.textAppTitle);

        // 计算大标题+搜索框的高度（滑动时隐藏的区域）
        textLargeTitle.post(() -> {
            int h1 = textLargeTitle.getHeight() + ((ViewGroup.MarginLayoutParams) textLargeTitle.getLayoutParams()).topMargin;
            int h2 = searchContainer.getHeight();
            hiddenAreaHeight = h1 + h2;
        });
        setupRecyclerScrollToggle();

        // 监听数据库数据并加载（初始加载全部笔记）
        loadAllNotes();

        // 添加按钮
        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddNoteActivity.class);
            startActivityForResult(intent, REQUEST_ADD_NOTE);
        });

        // 删除按钮
        fabDelete = findViewById(R.id.fabDelete);
        fabDelete.setOnClickListener(v -> {
            deleteSelectedNotesWithConfirm();
        });

        // 文件夹按钮
        buttonCategory = findViewById(R.id.buttonCategory);
        buttonCategory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
            startActivityForResult(intent, REQUEST_CATEGORY);
        });

        // 设置按钮
        buttonSettings = findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 多选模式相关控件
        buttonExitMultiSelect = findViewById(R.id.buttonExitMultiSelect);
        buttonMultiSelectIcon = findViewById(R.id.buttonMultiSelectIcon);
        textSelectedCount = findViewById(R.id.textSelectedCount);
        bottomActionBar = findViewById(R.id.bottomActionBar);
        actionPin = findViewById(R.id.actionPin);
        actionMove = findViewById(R.id.actionMove);
        actionDelete = findViewById(R.id.actionDelete);

        // 退出多选按钮
        buttonExitMultiSelect.setOnClickListener(v -> exitMultiSelectMode());

        // 底部操作栏按钮
        actionPin.setOnClickListener(v -> pinSelectedNotes());
        actionMove.setOnClickListener(v -> moveSelectedNotes());
        actionDelete.setOnClickListener(v -> deleteSelectedNotesWithConfirm());
    }

    /**
     * 安排回收站自动清理的后台任务
     */
    private void scheduleTrashCleanupWork() {
        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(TrashCleanupWorker.class,
                        java.time.Duration.ofDays(1))
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "trash_cleanup_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
    }

    /**
     * 进入多选模式
     */
    private void enterMultiSelectMode() {
        isMultiSelectMode = true;
        adapter.enterMultiSelectMode();

        // 隐藏普通模式的控件
        fabAdd.hide();
        buttonCategory.setVisibility(View.GONE);
        textAppTitle.setVisibility(View.GONE);

        // 显示多选模式的控件
        buttonExitMultiSelect.setVisibility(View.VISIBLE);
        buttonMultiSelectIcon.setVisibility(View.VISIBLE);
        textSelectedCount.setVisibility(View.VISIBLE);
        bottomActionBar.setVisibility(View.VISIBLE);

        updateSelectedCount();
    }

    /**
     * 退出多选模式
     */
    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        adapter.exitMultiSelectMode();

        // 显示普通模式的控件
        fabAdd.show();
        buttonCategory.setVisibility(View.VISIBLE);
        textAppTitle.setVisibility(View.VISIBLE);

        // 隐藏多选模式的控件
        buttonExitMultiSelect.setVisibility(View.GONE);
        buttonMultiSelectIcon.setVisibility(View.GONE);
        textSelectedCount.setVisibility(View.GONE);
        bottomActionBar.setVisibility(View.GONE);
    }

    /**
     * 更新选中数量显示
     */
    private void updateSelectedCount() {
        int count = adapter.getSelectedCount();
        textSelectedCount.setText("已选择 " + count + " 项");
    }

    /**
     * 更新删除按钮文字
     */
    private void updateDeleteButtonText() {
        updateSelectedCount();
    }

    /**
     * 置顶选中的笔记
     */
    private void pinSelectedNotes() {
        List<Note> selectedNotes = adapter.getSelectedNotes();
        if (selectedNotes.isEmpty()) {
            Toast.makeText(this, "请先选择笔记", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            for (Note note : selectedNotes) {
                note.isPinned = !note.isPinned; // 切换置顶状态
                noteDatabase.noteDao().update(note);
            }

            runOnUiThread(() -> {
                String message = selectedNotes.get(0).isPinned ? "已置顶" : "已取消置顶";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                exitMultiSelectMode();
            });
        }).start();
    }

    /**
     * 移动选中的笔记到指定分类
     */
    private void moveSelectedNotes() {
        List<Note> selectedNotes = adapter.getSelectedNotes();
        if (selectedNotes.isEmpty()) {
            Toast.makeText(this, "请先选择笔记", Toast.LENGTH_SHORT).show();
            return;
        }

        // 跳转到分类选择页面
        Intent intent = new Intent(MainActivity.this, CategoryActivity.class);
        startActivityForResult(intent, REQUEST_MOVE_TO_CATEGORY);
    }

    /**
     * 删除选中的笔记（带确认对话框）
     */
    private void deleteSelectedNotesWithConfirm() {
        List<Note> selectedNotes = adapter.getSelectedNotes();
        if (selectedNotes.isEmpty()) {
            Toast.makeText(this, "请先选择笔记", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = selectedNotes.size();
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除选中的 " + count + " 条笔记吗？")
                .setPositiveButton("确认删除", (dialogInterface, which) -> {
                    // 直接执行删除，不再弹出第二个对话框
                    new Thread(() -> {
                        for (Note note : selectedNotes) {
                            noteDatabase.noteDao().delete(note);
                        }

                        runOnUiThread(() -> {
                            Toast.makeText(this, "已删除 " + count + " 条笔记", Toast.LENGTH_SHORT).show();
                            exitMultiSelectMode();
                        });
                    }).start();
                })
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 设置"确认删除"按钮为红色
        android.widget.Button positiveButton = dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(android.graphics.Color.RED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == REQUEST_ADD_NOTE && resultCode == RESULT_OK) {
            // 从添加笔记页面返回，强制刷新列表
            Log.d(TAG, "从添加笔记页面返回，准备刷新列表");
            refreshNoteList();
        } else if (requestCode == REQUEST_EDIT_NOTE && resultCode == RESULT_OK) {
            // 从编辑笔记页面返回，强制刷新列表
            Log.d(TAG, "从编辑笔记页面返回，准备刷新列表");
            refreshNoteList();
        } else if (requestCode == REQUEST_CATEGORY && resultCode == RESULT_OK && data != null) {
            // 从分类页面返回，获取选中的分类
            int categoryId = data.getIntExtra("CATEGORY_ID", -1);
            String categoryName = data.getStringExtra("CATEGORY_NAME");
            Log.d(TAG, "选中分类: " + categoryName + " (ID: " + categoryId + ")");

            currentCategoryId = categoryId;
            filterNotesByCategory(categoryId);
        } else if (requestCode == REQUEST_MOVE_TO_CATEGORY && resultCode == RESULT_OK && data != null) {
            // 从移动到分类页面返回
            int categoryId = data.getIntExtra("CATEGORY_ID", -1);
            String categoryName = data.getStringExtra("CATEGORY_NAME");

            // 移动选中的笔记到指定分类
            List<Note> selectedNotes = adapter.getSelectedNotes();
            new Thread(() -> {
                for (Note note : selectedNotes) {
                    note.categoryId = categoryId;
                    noteDatabase.noteDao().update(note);
                }

                runOnUiThread(() -> {
                    Toast.makeText(this, "已成功移动到 \"" + categoryName + "\"", Toast.LENGTH_SHORT).show();
                    // 这里你原来的代码缺失了括号和分号，我已经补全
                    exitMultiSelectMode();
                });
            }).start();
        }
    }

    private void refreshNoteList() {
        // 根据当前分类和搜索关键字刷新列表
        if (currentCategoryId == -1) {
            // 全部
            if (currentSearchQuery == null || currentSearchQuery.isEmpty()) {
                loadAllNotes();
            } else {
                performSearch(currentSearchQuery);
            }
        } else {
            // 指定分类
            filterNotesByCategory(currentCategoryId);
        }
    }

    private void filterNotesByCategory(int categoryId) {
        // 切换分类时需要清空搜索框，同时避免触发 TextWatcher 递归
        isUpdatingSearchText = true;
        editTextSearch.setText("");
        currentSearchQuery = "";
        isUpdatingSearchText = false;

        // 清除高亮
        if (adapter != null) {
            adapter.setHighlightQuery("");
        }

        if (categoryId == -1) {
            // 显示全部笔记
            loadAllNotes();
        } else {
            // 按分类过滤
            noteDatabase.noteDao().getNotesByCategory(categoryId)
                    .observe(this, notes -> {
                        adapter.setNoteList(notes);
                        adapter.setHighlightQuery("");
                    });
        }
    }

    private void loadAllNotes() {
        noteDatabase.noteDao().getAllNotes().observe(this, notes -> {
            Log.d(TAG, "数据库数据更新，笔记数量: " + (notes != null ? notes.size() : 0));
            adapter.setNoteList(notes);
            // 根据当前搜索词决定是否需要高亮
            adapter.setHighlightQuery(currentSearchQuery != null ? currentSearchQuery : "");
        });
    }

    private void setupSearchBox() {
        // 文本变化监听
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdatingSearchText) {
                    // 代码内部更新搜索框时，不触发搜索逻辑，防止递归
                    return;
                }
                String query = s.toString().trim();
                currentSearchQuery = query;

                // 显示或隐藏清除按钮
                if (query.isEmpty()) {
                    buttonClearSearch.setVisibility(View.GONE);
                } else {
                    buttonClearSearch.setVisibility(View.VISIBLE);
                }

                // 执行搜索
                performSearch(query);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // 软键盘搜索键行为：仅收起键盘
        editTextSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                editTextSearch.clearFocus();
                return true;
            }
            return false;
        });

        // 清除搜索按钮
        buttonClearSearch.setOnClickListener(v -> {
            isUpdatingSearchText = true;
            editTextSearch.setText("");
            editTextSearch.clearFocus();
            isUpdatingSearchText = false;
            currentSearchQuery = "";

            // 恢复当前分类下的全部笔记
            if (currentCategoryId == -1) {
                loadAllNotes();
            } else {
                filterNotesByCategory(currentCategoryId);
            }
        });
    }

    /**
     * 执行搜索，并在适配器中设置高亮关键字
     */
    private void performSearch(String query) {
        if (adapter == null) return;

        // 更新高亮关键字
        adapter.setHighlightQuery(query);

        if (query == null || query.isEmpty()) {
            // 搜索为空时，按当前分类显示
            if (currentCategoryId == -1) {
                loadAllNotes();
            } else {
                filterNotesByCategory(currentCategoryId);
            }
            return;
        }

        String pattern = "%" + query + "%";
        noteDatabase.noteDao().searchNotes(pattern).observe(this, notes -> {
            adapter.setNoteList(notes);
            adapter.setHighlightQuery(query);
        });
    }

    private void setupCategoryChips() {
        // 先清空容器
        categoryChipContainer.removeAllViews();

        NoteDatabase database = NoteDatabase.getInstance(this);
        database.categoryDao().getAllCategories().observe(this, categories -> {
            categoryChipContainer.removeAllViews();

            // “全部”
            addCategoryChip("全部", -1);

            // 用户自定义分类
            if (categories != null) {
                for (com.calculator.notepadapp.model.Category c : categories) {
                    addCategoryChip(c.name, c.id);
                }
            }

            // “未分类”
            addCategoryChip("未分类", 0);

            highlightSelectedCategory();
        });
    }

    private void addCategoryChip(String name, int categoryId) {
        TextView chip = new TextView(this);
        chip.setText(name);
        chip.setTextSize(14f);
        chip.setPadding(28, 10, 28, 10);
        chip.setBackgroundResource(R.drawable.bg_chip_selector);
        // 文本颜色用主题色，由 selector 控制选中状态
        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(8, 8, 8, 8);
        chip.setLayoutParams(lp);

        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setTag(categoryId);
        chip.setOnClickListener(v -> {
            currentCategoryId = categoryId;
            highlightSelectedCategory();
            filterNotesByCategory(categoryId);
        });

        categoryChipContainer.addView(chip);
    }

    private void highlightSelectedCategory() {
        for (int i = 0; i < categoryChipContainer.getChildCount(); i++) {
            View child = categoryChipContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                Object tag = tv.getTag();
                boolean selected = (tag instanceof Integer) && ((Integer) tag) == currentCategoryId;
                tv.setSelected(selected);
            }
        }
    }

    private void setupRecyclerScrollToggle() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) {
                    // 向上滑动：隐藏大标题和搜索框
                    hideTopBars();
                } else if (dy < 0) {
                    // 向下滑动：显示大标题和搜索框
                    showTopBars();
                }
            }
        });
    }

    private void showTopBars() {
        if (!topBarsHidden) return;
        topBarsHidden = false;

        // 显示大标题和搜索框
        textLargeTitle.animate().alpha(1f).translationY(0).setDuration(200).start();
        searchContainer.animate().alpha(1f).translationY(0).setDuration(200).start();

        // 分类栏和列表下移回原位
        categoryScroll.animate().translationY(0).setDuration(200).start();
        recyclerView.animate().translationY(0).setDuration(200).start();

        // 隐藏顶部小标题
        textAppTitle.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            textAppTitle.setVisibility(View.GONE);
        }).start();
    }

    private void hideTopBars() {
        if (topBarsHidden) return;
        topBarsHidden = true;

        // 向上移动并淡出大标题和搜索框
        textLargeTitle.animate().alpha(0f)
                .translationY(-textLargeTitle.getHeight())
                .setDuration(200).start();
        searchContainer.animate().alpha(0f)
                .translationY(-searchContainer.getHeight())
                .setDuration(200).start();

        // 分类栏和列表上移
        if (hiddenAreaHeight > 0) {
            categoryScroll.animate().translationY(-hiddenAreaHeight).setDuration(200).start();
            recyclerView.animate().translationY(-hiddenAreaHeight).setDuration(200).start();
        }

        // 显示顶部小标题
        textAppTitle.setVisibility(View.VISIBLE);
        textAppTitle.animate().alpha(1f).setDuration(150).start();
    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            // 确保状态栏可见
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // 使用主题背景色作为状态栏颜色
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.background_primary));

            // 根据当前是否为夜间模式来决定图标深浅
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int nightModeFlags = getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                boolean isNight = (nightModeFlags
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES);

                int visibility = window.getDecorView().getSystemUiVisibility();
                if (!isNight) {
                    // 日间：浅色背景 + 深色图标
                    visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    // 夜间：深色背景 + 浅色图标
                    visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                window.getDecorView().setSystemUiVisibility(visibility);
            }
        }
    }
}
