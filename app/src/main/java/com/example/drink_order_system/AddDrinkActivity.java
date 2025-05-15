package com.example.drink_order_system;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class AddDrinkActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText etName, etType, etPrice, etDesc, etImageRes;
    private Button btnSave, btnBack;
    private ImageView ivPreview;
    private Button btnUpload;
    private String currentImagePath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_drink);

        // 初始化视图
        etName = findViewById(R.id.et_name);
        etType = findViewById(R.id.et_type);
        etPrice = findViewById(R.id.et_price);
        etDesc = findViewById(R.id.et_description);
        btnSave = findViewById(R.id.btn_save);  // 关键初始化行
        // 初始化视图
        ivPreview = findViewById(R.id.iv_preview);
        btnUpload = findViewById(R.id.btn_upload);
        // 图片上传按钮点击事件
        btnUpload.setOnClickListener(v -> openImageChooser());
        btnBack = findViewById(R.id.btn_back); // 绑定返回按钮
        // 设置返回按钮点击事件
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveDrink());
    }
    // 处理选择结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                // 获取图片路径并显示预览
                currentImagePath = getRealPathFromUri(imageUri);
                Glide.with(this) // 使用Glide加载图片
                        .load(imageUri)
                        .into(ivPreview);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private long getCurrentShopId() {
        // 获取当前登录用户名
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String username = prefs.getString("current_username", "");

        // 查询数据库获取shop_id
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_SHOP_ID +
                        " FROM " + DatabaseHelper.TABLE_SHOPS +
                        " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username}
        );

        long shopId = -1;
        if (cursor.moveToFirst()) {
            shopId = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return shopId;
    }
    // 获取真实文件路径
    private String getRealPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(columnIndex);
            cursor.close();
            return path;
        }
        return uri.getPath();
    }
    private void saveDrink() {

        // 验证输入
        if (TextUtils.isEmpty(etName.getText())) {
            showError("请输入饮品名称");
            return;
        }
        // 获取当前商家的shop_id
        long shopId = getCurrentShopId();
        if (shopId == -1) {
            Toast.makeText(this, "无法获取店铺信息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取图片路径（保持原有逻辑）
        String imagePath = currentImagePath != null ? currentImagePath : "";
        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // 插入数据库
        // 调用修改后的插入方法
        long result = dbHelper.insertDrink(
                etName.getText().toString(),
                etType.getText().toString(),
                Float.parseFloat(etPrice.getText().toString()),
                etDesc.getText().toString(),
                currentImagePath,// 使用图片路径
                shopId // 传入正确的shop_id
        );

        if (result != -1) {
            Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK); // 设置成功结果
            finish(); // 关闭当前界面
        } else {
            Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show();
        }
    }
    // 打开图片选择器
    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    private void showError(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("输入错误")
                .setMessage(msg)
                .setPositiveButton("确定", null)
                .show();
    }
}