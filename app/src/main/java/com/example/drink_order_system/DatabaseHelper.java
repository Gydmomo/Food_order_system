package com.example.drink_order_system;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    // 数据库基本信息
    private static final String DATABASE_NAME = "DrinkOrderSystem.db";
    private static final int DATABASE_VERSION = 13;  // 版本号升级

    // 用户表
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    // 饮品表
    public static final String TABLE_DRINKS = "drinks";
    public static final String COLUMN_DRINK_ID = "drink_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_IMAGE_RES = "image_res";

    // 订单表
    public static final String TABLE_ORDERS = "orders";
    public static final String COLUMN_ORDER_ID = "order_id";
    public static final String COLUMN_ORDER_DATE = "order_date";
    public static final String COLUMN_TAKE_AWAY = "take_away";
    public static final String COLUMN_TOTAL = "total";

    // 订单明细表
    public static final String TABLE_ORDER_ITEMS = "order_items";
    public static final String COLUMN_ITEM_ID = "item_id";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_SUGAR = "sugar";
    public static final String COLUMN_QUANTITY = "quantity";

    // 新增购物车表
    public static final String TABLE_CART_ITEMS = "cart_items";
    public static final String COLUMN_CART_ID = "cart_id";
    // 商店表
    public static final String TABLE_SHOPS = "shops";
    public static final String COLUMN_SHOP_ID = "shop_id";
    public static final String COLUMN_SHOP_NAME = "shop_name";

    // 创建商店表
    // 创建商店表
    private static final String CREATE_TABLE_SHOPS =
            "CREATE TABLE " + TABLE_SHOPS + " (" +
                    COLUMN_SHOP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_SHOP_NAME + " TEXT NOT NULL, " +
                    "shop_description TEXT, " + // 新增店铺描述字段
                    "FOREIGN KEY(" + COLUMN_USERNAME + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USERNAME + ") ON DELETE CASCADE);";

    // 创建用户表
    // 用户表添加role字段和约束条件
    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USERNAME + " TEXT PRIMARY KEY, " +
                    COLUMN_PASSWORD + " TEXT NOT NULL, " +
                    "role TEXT NOT NULL DEFAULT 'customer' CHECK(role IN ('customer','merchant')));";

    // 创建饮品表
    // 修改后的饮品表结构
    private static final String CREATE_TABLE_DRINKS =
            "CREATE TABLE " + TABLE_DRINKS + " (" +
                    COLUMN_DRINK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_PRICE + " REAL NOT NULL, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    "image_path TEXT,"+ // 新增图片路径字段
                    COLUMN_SHOP_ID + " INTEGER NOT NULL, " + // 新增字段
                            "FOREIGN KEY (" + COLUMN_SHOP_ID + ") REFERENCES " +
                    TABLE_SHOPS + "(" + COLUMN_SHOP_ID + ") ON DELETE CASCADE);"; // 外键约束

    // 创建订单表
    // 修改DatabaseHelper类中的订单表创建语句
    private static final String CREATE_TABLE_ORDERS =
            "CREATE TABLE " + TABLE_ORDERS + " (" +
                    COLUMN_ORDER_ID + " TEXT PRIMARY KEY, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_ORDER_DATE + " TEXT NOT NULL, " +
                    COLUMN_TAKE_AWAY + " INTEGER DEFAULT 0, " +
                    COLUMN_TOTAL + " REAL NOT NULL, " +
                    "status TEXT DEFAULT '备餐', " + // 新增状态字段
                    "FOREIGN KEY(" + COLUMN_USERNAME + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USERNAME + ") ON DELETE CASCADE);";
    // 创建订单明细表
    private static final String CREATE_TABLE_ORDER_ITEMS =
            "CREATE TABLE " + TABLE_ORDER_ITEMS + " (" +
                    COLUMN_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ORDER_ID + " TEXT NOT NULL, " +
                    COLUMN_DRINK_ID + " INTEGER NOT NULL, " +
                    COLUMN_SIZE + " TEXT NOT NULL, " +
                    COLUMN_TEMPERATURE + " TEXT NOT NULL, " +
                    COLUMN_SUGAR + " TEXT NOT NULL, " +
                    COLUMN_QUANTITY + " INTEGER NOT NULL, " +
                    "FOREIGN KEY(" + COLUMN_ORDER_ID + ") REFERENCES " +
                    TABLE_ORDERS + "(" + COLUMN_ORDER_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY(" + COLUMN_DRINK_ID + ") REFERENCES " +
                    TABLE_DRINKS + "(" + COLUMN_DRINK_ID + ") ON DELETE CASCADE);";

    // 新增购物车表结构
    private static final String CREATE_TABLE_CART_ITEMS =
            "CREATE TABLE " + TABLE_CART_ITEMS + " (" +
                    COLUMN_CART_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_DRINK_ID + " INTEGER NOT NULL, " +
                    COLUMN_SIZE + " TEXT NOT NULL, " +
                    COLUMN_TEMPERATURE + " TEXT NOT NULL, " +
                    COLUMN_SUGAR + " TEXT NOT NULL, " +
                    COLUMN_QUANTITY + " INTEGER NOT NULL, " +
                    "FOREIGN KEY(" + COLUMN_USERNAME + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USERNAME + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY(" + COLUMN_DRINK_ID + ") REFERENCES " +
                    TABLE_DRINKS + "(" + COLUMN_DRINK_ID + ") ON DELETE CASCADE);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys = ON;");  // 启用外键
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_DRINKS);
        db.execSQL(CREATE_TABLE_ORDERS);
        db.execSQL(CREATE_TABLE_ORDER_ITEMS);
        db.execSQL(CREATE_TABLE_CART_ITEMS);  // 创建购物车表
        db.execSQL(CREATE_TABLE_SHOPS); // 新增商店表
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER_ITEMS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CART_ITEMS);  // 删除旧购物车表
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DRINKS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHOPS); // 可选，确保清理
            onCreate(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }
    // 在DatabaseHelper类中添加
    public long insertDrink(String name, String type, float price,
                            String description, String imagePath, long shopId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_PRICE, price);
        values.put(COLUMN_DESCRIPTION, description);
        values.put("image_path", imagePath);
        values.put(COLUMN_SHOP_ID, shopId); // 必须传入有效的店铺ID
        return db.insert(TABLE_DRINKS, null, values);
    }
    // 修改后的插入方法
    public long insertDrink(String name, String type, float price,
                            String description, String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_PRICE, price);
        values.put(COLUMN_DESCRIPTION, description);
        values.put("image_path", imagePath); // 存储图片路径
        return db.insert(TABLE_DRINKS, null, values);
    }
    // 在DatabaseHelper中添加
    public List<Shop> getAllShops() {
        List<Shop> shops = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHOPS,
                new String[]{COLUMN_SHOP_ID, COLUMN_SHOP_NAME, COLUMN_USERNAME},
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            shops.add(new Shop(
                    cursor.getInt(0),
                    cursor.getString(2),  // COLUMN_SHOP_NAME
                    cursor.getString(1)   // COLUMN_USERNAME
            ));
        }
        cursor.close();
        return shops;
    }

    public Cursor getDrinksByShop(long shopId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_DRINKS,
                null,
                COLUMN_SHOP_ID + "=?",
                new String[]{String.valueOf(shopId)},
                null, null, null);
    }



    public Shop getShopByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SHOPS,
                new String[]{COLUMN_SHOP_ID, COLUMN_SHOP_NAME},
                COLUMN_USERNAME + " = ?",
                new String[]{username}, null, null, null);

        if (cursor.moveToFirst()) {
            return new Shop(
                    cursor.getInt(0),
                    cursor.getString(1),
                    username
            );
        }
        return null;
    }

    public List<Drinks> getDrinksByShopId(int shopId) {
        List<Drinks> drinks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DRINKS,
                null,
                COLUMN_SHOP_ID + " = ?",
                new String[]{String.valueOf(shopId)},
                null, null, null);

        while (cursor.moveToNext()) {
            drinks.add(new Drinks(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getFloat(3),
                    cursor.getString(4),
                    cursor.getString(5)
            ));
        }
        cursor.close();
        return drinks;
    }
}