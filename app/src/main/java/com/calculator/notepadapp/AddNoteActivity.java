package com.calculator.notepadapp;

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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.calculator.notepadapp.dao.CategoryDao;
import com.calculator.notepadapp.dao.NoteDao;
import com.calculator.notepadapp.model.Category;
import com.calculator.notepadapp.model.Note;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AddNoteActivity extends AppCompatActivity {

    private static final String TAG = "AddNoteActivity";

    private EditText editTitle, editContent;
    private Button buttonSave;
    private ImageButton buttonBack, buttonUndo, buttonRedo, buttonConfirm;
    private Spinner spinnerCategory;
    private NoteDao noteDao;
    private CategoryDao categoryDao;
    private List<Category> categoryList = new ArrayList<>();
    private int selectedCategoryId = 0; // 默认未分类

    // 撤销/重做功能 - 标题
    private Stack<String> titleUndoStack = new Stack<>();
    private Stack<String> titleRedoStack = new Stack<>();
    private boolean isTitleUndoRedoOperation = false;
    private String initialTitle = ""; // 初始标题为空

    // 撤销/重做功能 - 内容
    private Stack<String> contentUndoStack = new Stack<>();
    private Stack<String> contentRedoStack = new Stack<>();
    private boolean isContentUndoRedoOperation = false;
    private String initialContent = ""; // 初始内容为空

    // 当前焦点在哪个输入框
    private EditText currentFocusedEditText = null;

    // 草稿功能已移除

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 应用主题设置
        ThemeHelper.applyTheme(this);

        // 设置状态栏颜色为紫色
        setStatusBarColor();

        setContentView(R.layout.activity_add_note);

        editTitle = findViewById(R.id.editTitle);
        editContent = findViewById(R.id.editContent);
        buttonSave = findViewById(R.id.buttonSave);
        buttonBack = findViewById(R.id.buttonBack);
        buttonUndo = findViewById(R.id.buttonUndo);
        buttonRedo = findViewById(R.id.buttonRedo);
        buttonConfirm = findViewById(R.id.buttonConfirm);
        spinnerCategory = findViewById(R.id.spinnerCategory);

        // 获取数据库的 DAO
        NoteDatabase database = NoteDatabase.getInstance(this);
        noteDao = database.noteDao();
        categoryDao = database.categoryDao();

        // 加载分类列表
        loadCategories();

        // 设置文本监听器（用于撤销/重做）
        setupTextWatcher();

        // 默认焦点在内容输入框
        currentFocusedEditText = editContent;
        editContent.requestFocus();

        // 返回按钮点击事件
        buttonBack.setOnClickListener(v -> finish());

        // 撤销按钮点击事件
        buttonUndo.setOnClickListener(v -> undo());

        // 重做按钮点击事件
        buttonRedo.setOnClickListener(v -> redo());

        // 确认按钮点击事件（与保存按钮功能相同）
        buttonConfirm.setOnClickListener(v -> saveNote());

        // 保存按钮点击事件
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

    }

    /**
     * 设置文本监听器，用于撤销/重做功能
     */
    private void setupTextWatcher() {
        // 标题输入框的焦点监听
        editTitle.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                currentFocusedEditText = editTitle;
                updateUndoRedoButtons();
            }
        });

        // 内容输入框的焦点监听
        editContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                currentFocusedEditText = editContent;
                updateUndoRedoButtons();
            }
        });

        // 标题输入框的文本监听器
        editTitle.addTextChangedListener(new TextWatcher() {
            private String beforeText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isTitleUndoRedoOperation) {
                    String currentText = s.toString();
                    if (!currentText.equals(beforeText)) {
                        // 将之前的文本压入撤销栈
                        titleUndoStack.push(beforeText);
                        titleRedoStack.clear(); // 新的编辑操作会清空重做栈
                        updateUndoRedoButtons();
                    }
                }
            }
        });

        // 内容输入框的文本监听器
        editContent.addTextChangedListener(new TextWatcher() {
            private String beforeText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                beforeText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 不处理
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isContentUndoRedoOperation) {
                    String currentText = s.toString();
                    if (!currentText.equals(beforeText)) {
                        // 将之前的文本压入撤销栈
                        contentUndoStack.push(beforeText);
                        contentRedoStack.clear(); // 新的编辑操作会清空重做栈
                        updateUndoRedoButtons();
                    }
                }
            }
        });

        // 初始化时更新按钮状态（都应该禁用）
        updateUndoRedoButtons();
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

        buttonUndo.setEnabled(canUndo);
        buttonUndo.setAlpha(canUndo ? 1.0f : 0.3f);

        buttonRedo.setEnabled(canRedo);
        buttonRedo.setAlpha(canRedo ? 1.0f : 0.3f);
    }

    /**
     * 加载分类列表
     */
    private void loadCategories() {
        categoryDao.getAllCategories().observe(this, categories -> {
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
                categoryNames.add(category.name);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    categoryNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCategory.setAdapter(adapter);
        });
    }

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

        // Room 数据库不允许在主线程中进行写操作，需要新线程
        new Thread(() -> {
            try {
                // 只有当用户输入了标题时才检查同名
                if (!TextUtils.isEmpty(title)) {
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

                // 不存在同名笔记，继续保存
                long currentTime = System.currentTimeMillis();

                // 由于 Note 类现在只有无参构造函数，这里使用无参构造并通过 setter 赋值
                Note note = new Note();
                note.setTitle(title);
                note.setContent(content);
                note.setUpdatedAt(currentTime);
                note.setDeleted(false);
                note.setPinned(false);
                note.setDeletedAt(0L);

                // 设置分类（默认未分类为 0）
                int selectedPosition = spinnerCategory.getSelectedItemPosition();
                int categoryId = 0;
                if (selectedPosition >= 0 && selectedPosition < categoryList.size()) {
                    categoryId = categoryList.get(selectedPosition).id;
                }
                note.setCategoryId(categoryId);

                Log.d(TAG, "准备插入笔记到数据库，分类ID: " + categoryId);
                noteDao.insertNote(note);
                Log.d(TAG, "笔记插入成功");

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
        }).start();
    }

    /**
     * 设置状态栏为浅色背景，深色图标
     */
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            // 清除全屏标志，确保状态栏可见
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // 添加绘制系统栏背景的标志
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 设置状态栏颜色为白色
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.white));

            // 设置状态栏图标为深色（黑色），以便在浅色背景上清晰可见
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }
}
