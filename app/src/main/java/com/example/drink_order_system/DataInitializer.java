package com.example.drink_order_system;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class DataInitializer {
    private static final String IMAGE_DIR = "drink_images";
    private static final String DEFAULT_MERCHANT_USERNAME = "root";
    private static final String DEFAULT_MERCHANT_PASSWORD = "12345678";
    private static final String DEFAULT_SHOP_NAME = "食客行";

    public static void initializeDrinks(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // 检查默认商家是否存在
            long shopId = getDefaultShopId(db);

            if (shopId == -1) {
                // 插入默认商户用户
                ContentValues userValues = new ContentValues();
                userValues.put(DatabaseHelper.COLUMN_USERNAME, DEFAULT_MERCHANT_USERNAME);
                // 修改后（使用BCrypt加密）
                String hashedPassword = BCrypt.withDefaults().hashToString(12, DEFAULT_MERCHANT_PASSWORD.toCharArray());
                userValues.put(DatabaseHelper.COLUMN_PASSWORD, hashedPassword);
                userValues.put("role", "merchant");
                db.insert(DatabaseHelper.TABLE_USERS, null, userValues);

                // 插入默认商店
                ContentValues shopValues = new ContentValues();
                shopValues.put(DatabaseHelper.COLUMN_USERNAME, DEFAULT_MERCHANT_USERNAME);
                shopValues.put(DatabaseHelper.COLUMN_SHOP_NAME, DEFAULT_SHOP_NAME);
                shopId = db.insert(DatabaseHelper.TABLE_SHOPS, null, shopValues);
            }

            // 检查饮品数据是否已存在
            if (!hasDrinkData(db)) {
                createImageDir(context);
                insertDefaultDrinks(db, context, shopId);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    private static long getDefaultShopId(SQLiteDatabase db) {
        String query = "SELECT " + DatabaseHelper.COLUMN_SHOP_ID + " FROM " +
                DatabaseHelper.TABLE_SHOPS + " WHERE " +
                DatabaseHelper.COLUMN_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{DEFAULT_MERCHANT_USERNAME});

        long shopId = -1;
        if (cursor.moveToFirst()) {
            shopId = cursor.getLong(0);
        }
        cursor.close();
        return shopId;
    }

    private static boolean hasDrinkData(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " +
                DatabaseHelper.TABLE_DRINKS, null);
        cursor.moveToFirst();
        boolean hasData = cursor.getInt(0) > 0;
        cursor.close();
        return hasData;
    }

    private static void insertDefaultDrinks(SQLiteDatabase db, Context context, long shopId) {
        insertDrink(db, context, "牧场酸酪牛油果", "灵感上新", 23f,
                "定制牧场奶源酸酪...", R.drawable.avocado_square, shopId);

        insertDrink(db, context, "经典美式咖啡", "咖啡", 18f,
                "精选咖啡豆现磨...", R.drawable.black_sq, shopId);
        // 添加其他饮品...
    }

    private static void insertDrink(SQLiteDatabase db, Context context,
                                    String name, String type, float price,
                                    String desc, int imageResId, long shopId) {
        String imagePath = saveImageToStorage(context, imageResId, name);

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_NAME, name);
        values.put(DatabaseHelper.COLUMN_TYPE, type);
        values.put(DatabaseHelper.COLUMN_PRICE, price);
        values.put(DatabaseHelper.COLUMN_DESCRIPTION, desc);
        values.put("image_path", imagePath);
        values.put(DatabaseHelper.COLUMN_SHOP_ID, shopId); // 关联店铺ID

        db.insert(DatabaseHelper.TABLE_DRINKS, null, values);
    }

    private static void createImageDir(Context context) {
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private static String saveImageToStorage(Context context, int resId, String fileName) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
            File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), IMAGE_DIR);
            File imageFile = new File(directory, fileName + ".png");

            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}