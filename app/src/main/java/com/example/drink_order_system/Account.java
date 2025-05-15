package com.example.drink_order_system;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import at.favre.lib.crypto.bcrypt.BCrypt;

public class Account {
    private final String username;
    private final String password;
    private final Context mContext;
    private DatabaseHelper dbHelper;

    // 构造方法（用于注册/登录）
    public Account(String username, String password, Context context) {
        this.username = username;
        this.password = password;
        this.mContext = context;
        this.dbHelper = new DatabaseHelper(context);
    }
    // 添加此构造方法到 Account.java
    public Account(String username, Context context) {
        this.username = username;
        this.password = null;  // 无需密码的操作（如下单）
        this.mContext = context;
        this.dbHelper = new DatabaseHelper(context);
    }
    // 检查用户是否存在（登录验证）
    public boolean exist() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT " + DatabaseHelper.COLUMN_PASSWORD +
                    " FROM " + DatabaseHelper.TABLE_USERS +
                    " WHERE " + DatabaseHelper.COLUMN_USERNAME + " = ?";
            cursor = db.rawQuery(query, new String[]{username});
            if (cursor.moveToFirst()) {
                String storedHash = cursor.getString(0);
                // 验证密码哈希
                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);
                return result.verified;
            }
            return false;
        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "登录查询失败: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    // 保存用户到数据库（注册）
    public boolean saveAccount(String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("role", role); // 新增角色字段的保存
        // 生成密码哈希
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_PASSWORD, hashedPassword);

        try {
            db.insertOrThrow(DatabaseHelper.TABLE_USERS, null, values);
            return true;
        } catch (SQLiteException e) {
            if (e.getMessage().contains("UNIQUE constraint")) {
                Log.d("REGISTER", "用户名已存在: " + username);
            } else {
                Log.e("DB_ERROR", "注册失败: " + e.getMessage());
            }
            return false;
        } finally {
            db.close();
        }
    }

    // 保存订单到数据库
    public void saveBill(String takeAway, double cost) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        try {
            db.beginTransaction();
            String orderId = generateOrderId();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            values.put(DatabaseHelper.COLUMN_ORDER_ID, orderId);
            values.put(DatabaseHelper.COLUMN_USERNAME, username);
            values.put(DatabaseHelper.COLUMN_ORDER_DATE, dateFormat.format(new Date()));
            values.put(DatabaseHelper.COLUMN_TAKE_AWAY, takeAway);
            values.put(DatabaseHelper.COLUMN_TOTAL, cost);

            db.insert(DatabaseHelper.TABLE_ORDERS, null, values);
            db.setTransactionSuccessful();
            Log.d("ORDER", "订单保存成功: " + orderId);
        } catch (SQLiteException e) {
            Log.e("DB_ERROR", "订单保存失败: " + e.getMessage());
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    // 新增角色获取方法
    public String getRoleFromDB() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT role FROM users WHERE username=?",
                new String[]{username});
        return cursor.moveToFirst() ? cursor.getString(0) : "customer";
    }
    // 新增角色验证
    public boolean isMerchant() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT role FROM users WHERE username=?",
                new String[]{username});
        if (cursor.moveToFirst()) {
            return "merchant".equals(cursor.getString(0));
        }
        return false;
    }
    // 生成唯一订单ID（时间戳 + UUID）
    private String generateOrderId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + "-" + uuid;
    }

}