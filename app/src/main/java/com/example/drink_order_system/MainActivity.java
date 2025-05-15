package com.example.drink_order_system;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final Context mContext = this;
    private EditText ET_username;
    private EditText ET_password;
    private RadioGroup rgRole;

    public void handlePermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission has been allowed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "ask for permission", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DataInitializer.initializeDrinks(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handlePermission();
        ET_username = findViewById(R.id.et_username);
        ET_password = findViewById(R.id.et_password);
        rgRole = findViewById(R.id.rg_role);
    }

    public void BT_signUp_onClick(View v) {
        String username = ET_username.getText().toString();
        String password = ET_password.getText().toString();

        if (username.equals("") || password.equals("")) {
            Toast.makeText(this, "用户名或密码不能为空 (!_!)", Toast.LENGTH_SHORT).show();
        } else if (password.length() < 8) {
            Toast.makeText(this, "为安全考虑，密码至少8位(!_!)", Toast.LENGTH_SHORT).show();
        } else if (username.contains("\\") || username.contains("/") || username.contains(":") || username.contains("*")
                || username.contains("?") || username.contains("\"") || username.contains("<") || username.contains(">")
                || username.contains("|")) {
            Toast.makeText(this, "用户名中请勿包含\n \\ / : * ? \" < > |等特殊字符(!_!)", Toast.LENGTH_SHORT).show();
        } else {
            String role = rgRole.getCheckedRadioButtonId() == R.id.rb_merchant
                    ? "merchant" : "customer";
            Account temp = new Account(username, password, mContext);

            if (temp.saveAccount(role)) {
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                prefs.edit()
                        .putString("current_username", username)
                        .putString("role", role)
                        .apply();

                Toast.makeText(this, "注册成功 (^_^)", Toast.LENGTH_SHORT).show();

                // 如果是商家角色，显示添加店铺对话框
                if ("merchant".equals(role)) {
                    showAddShopDialog(username);
                } else {
                    // 普通用户直接跳转
                    Intent intent = new Intent(MainActivity.this, RootActivity.class);
                    intent.putExtra("userName", username);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(this, "注册失败 (@_@)\n该用户名已存在，请尝试其他用户名", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void BT_logIn_onClick(View v) {
        String username = ET_username.getText().toString().trim();
        String password = ET_password.getText().toString().trim();

        // 获取用户选择的角色
        int selectedId = rgRole.getCheckedRadioButtonId();
        String selectedRole = (selectedId == R.id.rb_merchant) ? "merchant" : "customer";

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "用户名或密码不能为空(!_!)", Toast.LENGTH_SHORT).show();
            return;
        }

        Account temp = new Account(username, password, mContext);
        if (temp.exist()) {
            // 获取数据库中的实际角色
            String actualRole = temp.getRoleFromDB();

            if (selectedRole.equals(actualRole)) {
                // 保存登录信息
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                prefs.edit()
                        .putString("current_username", username)
                        .putString("role", selectedRole)
                        .apply();

                // 如果是商家角色，检查是否已有店铺
                if ("merchant".equals(actualRole)) {
                    if (hasShop(username)) {
                        // 已有店铺，直接跳转
                        redirectUser(actualRole);
                    } else {
                        // 没有店铺，显示添加店铺对话框
                        showAddShopDialog(username);
                    }
                } else {
                    // 普通用户直接跳转
                    redirectUser(actualRole);
                }
            } else {
                // 角色不匹配
                Toast.makeText(this, "登录失败 (@_@)\n角色类型错误", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 用户名或密码错误
            Toast.makeText(this, "登录失败 (@_@)\n用户名或密码错误", Toast.LENGTH_SHORT).show();
        }
    }

    // 新增跳转逻辑
    private void redirectUser(String role) {
        Intent intent;
        if ("merchant".equals(role)) {
            intent = new Intent(this, MerchantRootActivity.class);
        } else {
            intent = new Intent(this, RootActivity.class);
        }
        intent.putExtra("userName", ET_username.getText().toString());
        startActivity(intent);
    }

    private void showAddShopDialog(String username) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_shop, null);
        builder.setView(dialogView);
        builder.setCancelable(false); // 防止用户通过点击外部关闭对话框

        final EditText etShopName = dialogView.findViewById(R.id.et_shop_name);
        final EditText etShopDescription = dialogView.findViewById(R.id.et_shop_description);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        final AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> {
            // 用户取消添加店铺，可以提示或直接关闭
            Toast.makeText(MainActivity.this, "您需要添加店铺才能使用商家功能", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            String shopName = etShopName.getText().toString().trim();
            String shopDescription = etShopDescription.getText().toString().trim();

            if (shopName.isEmpty()) {
                Toast.makeText(MainActivity.this, "店铺名称不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            // 保存店铺信息到数据库
            if (addShopToDatabase(username, shopName, shopDescription)) {
                Toast.makeText(MainActivity.this, "店铺添加成功", Toast.LENGTH_SHORT).show();
                dialog.dismiss();

                // 添加成功后跳转到商家主界面
                Intent intent = new Intent(MainActivity.this, MerchantRootActivity.class);
                intent.putExtra("userName", username);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "店铺添加失败，请重试", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean addShopToDatabase(String username, String shopName, String shopDescription) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_SHOP_NAME, shopName);

        // 如果数据库表中有店铺描述字段，也可以添加
        // values.put("shop_description", shopDescription);

        long result = db.insert(DatabaseHelper.TABLE_SHOPS, null, values);
        db.close();

        return result != -1;
    }

    // 检查用户是否已有店铺
    private boolean hasShop(String username) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_SHOPS,
                null,
                DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );

        boolean hasShop = cursor.getCount() > 0;
        cursor.close();
        db.close();

        return hasShop;
    }
}
