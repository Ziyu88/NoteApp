package com.calculator.notepadapp;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.calculator.notepadapp.dao.CategoryDao;
import com.calculator.notepadapp.dao.NoteDao;
import com.calculator.notepadapp.model.Category;
import com.calculator.notepadapp.model.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EditNoteActivity extends AppCompatActivity {

    private static final String TAG = "EditNoteActivity";

    private EditText editTitle, editContent;
    private Button buttonSave, buttonDelete;
    private ImageButton buttonBack, buttonUndo, buttonRedo, buttonConfirm;
    private Spinner spinnerCategory;
    private NoteDao noteDao;
    private CategoryDao categoryDao;
    private DatabaseViewModel dbViewModel;
    private Note currentNote;
    private int noteId;
    private String originalTitle; // 保存原始标题，用于检查是否修改
    private List<Category> categoryList = new ArrayList<>();

    // 撤销/重做功能 - 标题
    private Stack<String> titleUndoStack = new Stack<>();
    private Stack<String> titleRedoStack = new Stack<>();
    private boolean isTitleUndoRedoOperation = false;
    private String initialTitle = ""; // 初始标题

    // 撤销/重做功能 - 内容
    private Stack<String> contentUndoStack = new Stack<>();
    private Stack<String> contentRedoStack = new Stack<>();
    private boolean isContentUndoRedoOperation = false;
    private String initialContent = ""; // 初始内容

    // 当前焦点在哪个输入框
    private EditText currentFocusedEditText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 应用主题设置
        ThemeHelper.applyTheme(this);

        // 设置状态栏颜色为白色
        setStatusBarColor();

        setContentView(R.layout.activity_edit_note);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonBack = findViewById(R.id.buttonBack);
        buttonUndo = findViewById(R.id.buttonUndo);
        buttonRedo = findViewById(R.id.buttonRedo);
        buttonConfirm = findViewById(R.id.buttonConfirm);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        // 获取数据库的 DAO
        NoteDatabase database = NoteDatabase.getInstance(this);
        noteDao = database.noteDao();
        categoryDao = database.categoryDao();
        dbViewModel = new ViewModelProvider(this).get(DatabaseViewModel.class);

        // 获取传递过来的笔记ID
        noteId = getIntent().getIntExtra("NOTE_ID", -1);

        if (noteId == -1) {
            Toast.makeText(this, "笔记不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化按钮状态
        updateUndoRedoButtons();

        // 设置按钮点击事件
        setupButtonClickListeners();

        // 加载分类列表
        loadCategories();

        // 加载笔记数据（会在加载完成后设置TextWatcher）
        loadNoteData();
    }

    private void setupButtonClickListeners() {
        // 返回按钮点击事件
        buttonBack.setOnClickListener(v -> finish());

        // 撤销按钮点击事件
        buttonUndo.setOnClickListener(v -> undo());

        // 重做按钮点击事件
        buttonRedo.setOnClickListener(v -> redo());

        // 确认按钮和保存按钮点击事件
        View.OnClickListener saveClickListener = v -> saveNote();
        buttonConfirm.setOnClickListener(saveClickListener);
        buttonSave.setOnClickListener(saveClickListener);

        // 删除按钮点击事件
        buttonDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    // 声明TextWatcher为成员变量
    private TextWatcher titleTextWatcher;
    private TextWatcher contentTextWatcher;
    private View.OnFocusChangeListener titleFocusChangeListener;
    private View.OnFocusChangeListener contentFocusChangeListener;

    /**
     * 设置文本监听器，用于撤销/重做功能
     */
    private void setupTextWatcher() {
        // 先移除已有的监听器，避免重复添加
        if (editTitle != null) {
            if (titleFocusChangeListener != null) {
                editTitle.setOnFocusChangeListener(null);
            }
            if (titleTextWatcher != null) {
                editTitle.removeTextChangedListener(titleTextWatcher);
            }
        }
        
        if (editContent != null) {
            if (contentFocusChangeListener != null) {
                editContent.setOnFocusChangeListener(null);
            }
            if (contentTextWatcher != null) {
                editContent.removeTextChangedListener(contentTextWatcher);
            }
        }

        // 标题输入框的焦点监听
        titleFocusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                currentFocusedEditText = editTitle;
                updateUndoRedoButtons();
            }
        };
        
        // 内容输入框的焦点监听
        contentFocusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                currentFocusedEditText = editContent;
                updateUndoRedoButtons();
            }
        };

        // 标题输入框的文本监听器
        titleTextWatcher = new TextWatcher() {
            private String beforeText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s != null) {
                    beforeText = s.toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isTitleUndoRedoOperation && s != null) {
                    String currentText = s.toString();
                    if (!currentText.equals(beforeText)) {
                        runOnUiThread(() -> {
                            // 将之前的文本压入撤销栈
                            if (titleUndoStack != null) {
                                titleUndoStack.push(beforeText);
                            }
                            if (titleRedoStack != null) {
                                titleRedoStack.clear(); // 新的编辑操作会清空重做栈
                            }
                            updateUndoRedoButtons();
                        });
                    }
                }
            }
        };

        // 内容输入框的文本监听器
        contentTextWatcher = new TextWatcher() {
            private String beforeText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (s != null) {
                    beforeText = s.toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isContentUndoRedoOperation && s != null) {
                    String currentText = s.toString();
                    if (!currentText.equals(beforeText)) {
                        runOnUiThread(() -> {
                            // 将之前的文本压入撤销栈
                            if (contentUndoStack != null) {
                                contentUndoStack.push(beforeText);
                            }
                            if (contentRedoStack != null) {
                                contentRedoStack.clear(); // 新的编辑操作会清空重做栈
                            }
                            updateUndoRedoButtons();
                        });
                    }
                }
            }
        };
        
        // 设置监听器
        if (editTitle != null) {
            editTitle.setOnFocusChangeListener(titleFocusChangeListener);
            editTitle.addTextChangedListener(titleTextWatcher);
        }
        
        if (editContent != null) {
            editContent.setOnFocusChangeListener(contentFocusChangeListener);
            editContent.addTextChangedListener(contentTextWatcher);
        }
    }


    /**
     * 撤销操作
     */
    private void undo() {
        if (currentFocusedEditText == editTitle && !titleUndoStack.isEmpty()) {
            // 撤销标题
            isTitleUndoRedoOperation = true;
            String currentText = editTitle.getText().toString();
            titleRedoStack.push(currentText);
            String previousText = titleUndoStack.pop();

            editTitle.setText(previousText);
            editTitle.setSelection(previousText.length()); // 光标移到末尾
            isTitleUndoRedoOperation = false;
            updateUndoRedoButtons();
        } else if (currentFocusedEditText == editContent && !contentUndoStack.isEmpty()) {
            // 撤销内容
            isContentUndoRedoOperation = true;
            String currentText = editContent.getText().toString();
            contentRedoStack.push(currentText);
            String previousText = contentUndoStack.pop();

            editContent.setText(previousText);
            editContent.setSelection(previousText.length()); // 光标移到末尾
            isContentUndoRedoOperation = false;
            updateUndoRedoButtons();
        }
    }

    /**
     * 重做操作
     */
    private void redo() {
        if (currentFocusedEditText == editTitle && !titleRedoStack.isEmpty()) {
            // 重做标题
            isTitleUndoRedoOperation = true;
            String currentText = editTitle.getText().toString();
            titleUndoStack.push(currentText);
            String nextText = titleRedoStack.pop();
            editTitle.setText(nextText);
            editTitle.setSelection(nextText.length()); // 光标移到末尾
            isTitleUndoRedoOperation = false;
            updateUndoRedoButtons();
        } else if (currentFocusedEditText == editContent && !contentRedoStack.isEmpty()) {
            // 重做内容
            isContentUndoRedoOperation = true;
            String currentText = editContent.getText().toString();
            contentUndoStack.push(currentText);
            String nextText = contentRedoStack.pop();
            editContent.setText(nextText);
            editContent.setSelection(nextText.length()); // 光标移到末尾
            isContentUndoRedoOperation = false;
            updateUndoRedoButtons();
        }
    }

    /**
     * 更新撤销/重做按钮的状态
     */
    private void updateUndoRedoButtons() {
        runOnUiThread(() -> {
            boolean canUndo = false;
            boolean canRedo = false;

            if (currentFocusedEditText == editTitle) {
                // 标题输入框有焦点
                canUndo = !titleUndoStack.isEmpty();
                canRedo = !titleRedoStack.isEmpty();
            } else if (currentFocusedEditText == editContent) {
                // 内容输入框有焦点
                canUndo = !contentUndoStack.isEmpty();
                canRedo = !contentRedoStack.isEmpty();
            }

            if (buttonUndo != null) {
                buttonUndo.setEnabled(canUndo);
                buttonUndo.setAlpha(canUndo ? 1.0f : 0.3f);
            }

            if (buttonRedo != null) {
                buttonRedo.setEnabled(canRedo);
                buttonRedo.setAlpha(canRedo ? 1.0f : 0.3f);
            }
        });
    }

    /**
     * 加载分类列表
     */
    private void loadCategories() {
        if (spinnerCategory == null) {
            Log.e(TAG, "spinnerCategory is null, cannot load categories");
            return;
        }
        
        categoryDao.getAllCategories().observe(this, categories -> {
            try {
                categoryList.clear();

                // 添加"未分类"选项
                Category uncategorized = new Category("未分类", 0);
                uncategorized.id = 0;
                categoryList.add(uncategorized);

                // 添加用户创建的分类
                if (categories != null) {
                    categoryList.addAll(categories);
                }

                // 创建适配器
                List<String> categoryNames = new ArrayList<>();
                for (Category category : categoryList) {
                    if (category != null && category.name != null) {
                        categoryNames.add(category.name);
                    }
                }

                runOnUiThread(() -> {
                    try {
                        if (spinnerCategory != null) {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    this,
                                    android.R.layout.simple_spinner_item,
                                    categoryNames
                            );
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spinnerCategory.setAdapter(adapter);

                            // 设置当前笔记的分类
                            if (currentNote != null) {
                                setCurrentCategory();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting up spinner adapter", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading categories", e);
            }
        });
    }

    /**
     * 设置当前笔记的分类
     */
    private void setCurrentCategory() {
        if (spinnerCategory == null || categoryList == null || currentNote == null) {
            Log.w(TAG, "setCurrentCategory: 必要的组件或数据为空");
            return;
        }
        
        try {
            for (int i = 0; i < categoryList.size(); i++) {
                if (categoryList.get(i).id == currentNote.categoryId) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "设置分类时出错", e);
        }
    }

    /**
     * 加载笔记数据
     */
    private void loadNoteData() {
        dbViewModel.execute(() -> {
            try {
                currentNote = noteDao.getNoteById(noteId);

                if (currentNote == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(EditNoteActivity.this, "笔记不存在", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                originalTitle = currentNote.title; // 保存原始标题
                initialTitle = currentNote.title != null ? currentNote.title : ""; // 保存初始标题
                initialContent = currentNote.content != null ? currentNote.content : ""; // 保存初始内容

                runOnUiThread(() -> {
                    try {
                        editTitle.setText(currentNote.title);
                        editContent.setText(currentNote.content);
                        
                        // 设置分类（确保在UI线程中执行）
                        setCurrentCategory();

                        // 数据加载完成后，再设置TextWatcher
                        setupTextWatcher();

                        // 默认焦点在内容输入框
                        currentFocusedEditText = editContent;
                        editContent.requestFocus();

                        // 初始化时撤销和重做按钮都应该禁用
                        updateUndoRedoButtons();

                        Log.d(TAG, "加载笔记成功: " + currentNote.title);
                    } catch (Exception e) {
                        Log.e(TAG, "UI更新失败", e);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "加载笔记失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(EditNoteActivity.this, "加载笔记失败: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    /**
     * 保存笔记
     */
    private void saveNote() {
        String title = editTitle.getText().toString().trim();
        String content = editContent.getText().toString().trim();

        // 允许标题或内容为空，但不能两者都为空
        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(content)) {
            Toast.makeText(this, "标题和内容不能同时为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 禁用保存按钮，防止重复点击
        buttonSave.setEnabled(false);

        Log.d(TAG, "开始保存笔记，标题: " + title);

        dbViewModel.execute(() -> {
            try {
                // 只有当用户输入了标题且标题被修改时才检查同名
                if (!TextUtils.isEmpty(title) && !title.equals(originalTitle)) {
                    // 标题被修改了，需要检查是否与其他笔记重名
                    int count = noteDao.countNotesByTitle(title);
                    Log.d(TAG, "同名笔记数量: " + count);

                    if (count > 0) {
                        // 存在同名笔记，提示用户
                        Log.d(TAG, "存在同名笔记，取消保存");
                        runOnUiThread(() -> {
                            Toast.makeText(this, "已存在同名笔记，请修改标题", Toast.LENGTH_LONG).show();
                            // 重新启用保存按钮
                            buttonSave.setEnabled(true);
                        });
                        return;
                    }
                }

                // 更新笔记
                currentNote.title = title;
                currentNote.content = content;
                currentNote.updatedAt = System.currentTimeMillis();

                // 更新分类（健壮性处理）
                int selectedPosition = -1;
                if (spinnerCategory != null) {
                    try {
                        selectedPosition = spinnerCategory.getSelectedItemPosition();
                    } catch (Exception ex) {
                        Log.w(TAG, "获取分类选中位置失败", ex);
                    }
                } else {
                    Log.w(TAG, "保存时spinnerCategory为null，使用默认分类");
                }

                if (selectedPosition >= 0 && selectedPosition < categoryList.size()) {
                    currentNote.categoryId = categoryList.get(selectedPosition).id;
                } else {
                    // 兜底：若无选中项或列表为空，使用0（未分类）或列表首项
                    currentNote.categoryId = categoryList.isEmpty() ? 0 : categoryList.get(0).id;
                }

                Log.d(TAG, "准备更新笔记到数据库，分类ID: " + currentNote.categoryId);
                noteDao.update(currentNote);
                Log.d(TAG, "笔记更新成功");

                // 等待一小段时间确保数据库写入完成
                Thread.sleep(200);

                runOnUiThread(() -> {
                    Log.d(TAG, "显示保存成功提示");
                    Toast.makeText(this, "保存成功！", Toast.LENGTH_LONG).show();
                    // 设置结果码，通知MainActivity刷新
                    setResult(RESULT_OK);

                    // 延迟500ms后返回，确保Toast能显示
                    editTitle.postDelayed(() -> {
                        finish(); // 返回主页面
                    }, 500);
                });

            } catch (Exception e) {
                Log.e(TAG, "保存笔记失败", e);
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // 重新启用保存按钮
                    buttonSave.setEnabled(true);
                });
            }
        });
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定要删除笔记 \"" + currentNote.title + "\" 吗？")
                .setPositiveButton("确认删除", (dialogInterface, which) -> {
                    deleteNote();
                })
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 设置红色删除按钮
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.RED);
        }
    }

    /**
     * 删除笔记
     */
    private void deleteNote() {
        dbViewModel.execute(() -> {
            try {
                currentNote.isDeleted = true;
                currentNote.deletedAt = System.currentTimeMillis();
                noteDao.update(currentNote);
                Log.d(TAG, "笔记删除成功");

                runOnUiThread(() -> {
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    // 设置结果码，通知MainActivity刷新
                    setResult(RESULT_OK);
                    finish(); // 返回主页面
                });

            } catch (Exception e) {
                Log.e(TAG, "删除笔记失败", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 根据当前日夜模式设置状态栏颜色和图标颜色
     */
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            // 清除全屏标志，确保状态栏可见
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // 添加绘制系统栏背景的标志
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // 使用主题背景色作为状态栏颜色
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.background_primary));

            // 根据当前是否为夜间模式来决定图标深浅
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                boolean isNight = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);

                int visibility = window.getDecorView().getSystemUiVisibility();
                if (!isNight) {
                    // 日间模式：浅色背景 + 深色图标
                    visibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    // 夜间模式：移除浅色状态栏标志，使用亮色图标
                    visibility &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                window.getDecorView().setSystemUiVisibility(visibility);
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        // 移除所有监听器
        if (editTitle != null) {
            editTitle.setOnFocusChangeListener(null);
            if (titleTextWatcher != null) {
                editTitle.removeTextChangedListener(titleTextWatcher);
            }
            titleTextWatcher = null;
        }
        
        if (editContent != null) {
            editContent.setOnFocusChangeListener(null);
            if (contentTextWatcher != null) {
                editContent.removeTextChangedListener(contentTextWatcher);
            }
            contentTextWatcher = null;
        }
        
        // 清空监听器引用
        titleFocusChangeListener = null;
        contentFocusChangeListener = null;
        
        // 清空栈
        if (titleUndoStack != null) {
            titleUndoStack.clear();
        }
        if (titleRedoStack != null) {
            titleRedoStack.clear();
        }
        if (contentUndoStack != null) {
            contentUndoStack.clear();
        }
        if (contentRedoStack != null) {
            contentRedoStack.clear();
        }
        
        // 清空视图引用
        editTitle = null;
        editContent = null;
        buttonUndo = null;
        buttonRedo = null;
        spinnerCategory = null;
        
        super.onDestroy();
    }
}

