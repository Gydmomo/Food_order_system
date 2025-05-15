package com.example.drink_order_system;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MerchantActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private EditText etName, etType, etPrice, etDesc;
    private Spinner spinnerImage;
    private long currentShopId;
    // 图片资源映射表
    private final Map<String, Integer> imageResources = new HashMap<String, Integer>() {{
        put("牛油果", R.drawable.avocado_square);
        put("黑咖啡", R.drawable.black_sq);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant);

        dbHelper = new DatabaseHelper(this);
        initViews();
        // 获取当前登录商户的shopId（这里假设已经保存了登录信息）
        String username = getSharedPreferences("user", MODE_PRIVATE).getString("username", "");
        currentShopId = getShopIdByUsername(username);
    }
    // 查询店铺ID的方法
    private long getShopIdByUsername(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + DatabaseHelper.COLUMN_SHOP_ID +
                        " FROM " + DatabaseHelper.TABLE_SHOPS +
                        " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username});

        long shopId = -1;
        if (cursor.moveToFirst()) {
            shopId = cursor.getLong(0);
        }
        cursor.close();
        return shopId;
    }
    private void initViews() {
        etName = findViewById(R.id.etDrinkName);
        etType = findViewById(R.id.etDrinkType);
        etPrice = findViewById(R.id.etPrice);
        etDesc = findViewById(R.id.etDescription);
        spinnerImage = findViewById(R.id.spinnerImage);

        findViewById(R.id.btnAddDrink).setOnClickListener(v -> addDrink());
    }

    private void addDrink() {
        String name = etName.getText().toString().trim();
        String type = etType.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String imageName = spinnerImage.getSelectedItem().toString();

        if (validateInput(name, type, priceStr, imageName)) {
            float price = Float.parseFloat(priceStr);
            Integer imageRes = imageResources.get(imageName);

            // 新增图片保存
            if (imageRes == null) {
                Toast.makeText(this, "无效的图片选择", Toast.LENGTH_SHORT).show();
                return;
            }

            String imagePath = saveImageToStorage(imageRes, name);
            if (imagePath.isEmpty()) {
                Toast.makeText(this, "图片保存失败", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                // 使用正确的参数调用
                long result = dbHelper.insertDrink(name, type, price, desc, imagePath, currentShopId);
                runOnUiThread(() -> {
                    if (result != -1) {
                        Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                        clearInputs();
                    } else {
                        Toast.makeText(this, "添加失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        }
    }
    private String saveImageToStorage(int resId, String drinkName) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
            File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "merchant_uploads");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 生成唯一文件名
            String fileName = drinkName + "_" + System.currentTimeMillis() + ".png";
            File imageFile = new File(directory, fileName);

            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
    private boolean validateInput(String name, String type, String price, String image) {
        if (name.isEmpty() || type.isEmpty() || price.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!price.matches("\\d+(\\.\\d+)?")) {
            Toast.makeText(this, "价格格式不正确", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!imageResources.containsKey(image)) {
            Toast.makeText(this, "请选择有效图片", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void clearInputs() {
        etName.setText("");
        etType.setText("");
        etPrice.setText("");
        etDesc.setText("");
        spinnerImage.setSelection(0);
    }

    @Override
    protected void onDestroy() {
        dbHelper.close();
        super.onDestroy();
    }
}