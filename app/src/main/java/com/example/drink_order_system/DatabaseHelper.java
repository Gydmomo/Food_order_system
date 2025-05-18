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
    private static final int DATABASE_VERSION = 18;  // 版本号升级

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
                    "status TEXT DEFAULT '备餐', " +
                    "shop_id INTEGER, " +
                    "user_info_id INTEGER, " + // 新增个人信息ID列
                    "FOREIGN KEY(" + COLUMN_USERNAME + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USERNAME + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY(user_info_id) REFERENCES user_info(info_id));"; // 新增外键约束    // 新增个人信息表
    // 替换原有user_info表结构
    private static final String CREATE_TABLE_USER_INFO =
            "CREATE TABLE user_info (" +
                    "info_id INTEGER PRIMARY KEY AUTOINCREMENT, " + // 新增唯一ID
                    "username TEXT NOT NULL, " +
                    "info_name TEXT NOT NULL, " + // 信息名称（如：家庭地址、办公地址）
                    "real_name TEXT, " +
                    "gender TEXT, " +
                    "phone TEXT, " +
                    "address TEXT, " +
                    "avatar_path TEXT, " +
                    "FOREIGN KEY(username) REFERENCES users(username) ON DELETE CASCADE);";    // 创建订单明细表
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
    // 修改 CREATE_TABLE_CART_ITEMS 定义
    private static final String CREATE_TABLE_CART_ITEMS =
            "CREATE TABLE " + TABLE_CART_ITEMS + " (" +
                    COLUMN_CART_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_DRINK_ID + " INTEGER NOT NULL, " +
                    COLUMN_SIZE + " TEXT NOT NULL, " +
                    COLUMN_TEMPERATURE + " TEXT NOT NULL, " +
                    COLUMN_SUGAR + " TEXT NOT NULL, " +
                    COLUMN_QUANTITY + " INTEGER NOT NULL, " +
                    COLUMN_SHOP_ID + " INTEGER NOT NULL, " + // 新增字段
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
        db.execSQL(CREATE_TABLE_SHOPS); // 先创建商店表，因为饮品表依赖它
        db.execSQL(CREATE_TABLE_DRINKS);
        db.execSQL(CREATE_TABLE_ORDERS);
        db.execSQL(CREATE_TABLE_ORDER_ITEMS);
        db.execSQL(CREATE_TABLE_CART_ITEMS);
        db.execSQL(CREATE_TABLE_USER_INFO); // 新增的创建语句
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            if (oldVersion < 18) { // 新版本号
                try {
                    db.execSQL("ALTER TABLE " + TABLE_ORDERS + " ADD COLUMN user_info_id INTEGER");
                } catch (Exception e) {
                    // 处理列已存在的情况
                }
            }
            if (oldVersion < 17) {
                // 1. 备份旧表
                db.execSQL("ALTER TABLE user_info RENAME TO temp_user_info");

                // 2. 创建新结构表
                db.execSQL(CREATE_TABLE_USER_INFO);

                // 3. 迁移数据（保留第一条记录）
                db.execSQL("INSERT INTO user_info (username, info_name, real_name, gender, phone, address) " +
                        "SELECT username, '默认信息', real_name, gender, phone, address " +
                        "FROM temp_user_info");

                // 4. 删除临时表
                db.execSQL("DROP TABLE temp_user_info");
            }
            // 版本14到15的升级：给cart_items添加shop_id
            if (oldVersion < 16) {
                db.execSQL(CREATE_TABLE_USER_INFO);
            }

            if (oldVersion < 15) {
                try {
                    // 检查表是否存在
                    Cursor cursor = db.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' AND name='cart_items'",
                            null
                    );
                    boolean tableExists = cursor.getCount() > 0;
                    cursor.close();
                    if (tableExists) {
                        // 检查列是否存在
                        cursor = db.rawQuery("PRAGMA table_info(cart_items)", null);
                        boolean columnExists = false;
                        while (cursor.moveToNext()) {
                            if ("shop_id".equals(cursor.getString(1))) {
                                columnExists = true;
                                break;
                            }
                        }
                        cursor.close();
                        if (!columnExists) {
                            db.execSQL("ALTER TABLE cart_items ADD COLUMN shop_id INTEGER NOT NULL DEFAULT 0");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 版本13到14的升级：添加shop_id列到orders表
            if (oldVersion < 14) {
                try {
                    // 尝试添加shop_id列
                    db.execSQL("ALTER TABLE " + TABLE_ORDERS + " ADD COLUMN shop_id INTEGER");
                } catch (Exception e) {
                    // 如果列已存在或其他错误，记录但继续
                    e.printStackTrace();
                }
            }

            // 未来版本的升级可以在这里添加
            // if (oldVersion < 15) { ... }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
            // 如果出现严重错误，回退到完全重建
            fallbackToRecreateDatabase(db);
        } finally {
            db.endTransaction();
        }
    }
    private void fallbackToRecreateDatabase(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CART_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DRINKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SHOPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
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
    public long insertOrUpdateUserInfo(String username, String realName,
                                       String gender, String phone,
                                       String address, String avatarPath) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("real_name", realName);
        values.put("gender", gender);
        values.put("phone", phone);
        values.put("address", address);
        values.put("avatar_path", avatarPath);

        // 使用INSERT OR REPLACE保证唯一性
        return db.replace("user_info", null, values);
    }
    public Cursor getUserInfo(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query("user_info",
                new String[]{"real_name", "gender", "phone", "address", "avatar_path"},
                "username = ?",
                new String[]{username},
                null, null, null);
    }
    public int deleteUserInfo(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("user_info",
                "username = ?",
                new String[]{username});
    }

    public long saveUserInfo(UserInfo info, String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("info_name", info.getInfoName());
        values.put("real_name", info.getRealName());
        values.put("gender", info.getGender());
        values.put("phone", info.getPhone());
        values.put("address", info.getAddress());

        if (info.getInfoId() == 0) { // 新增
            return db.insert("user_info", null, values);
        } else { // 更新
            return db.update("user_info", values,
                    "info_id = ?", new String[]{String.valueOf(info.getInfoId())});
        }
    }
    // 删除个人信息
    public int deleteUserInfo(int infoId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("user_info",
                "info_id = ?", new String[]{String.valueOf(infoId)});
    }
    public void clearCurrentUserData(String username) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("user_info", "username=?", new String[]{username});
    }
    public List<UserInfo> getAllUserInfos(String username) {
        List<UserInfo> infos = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("user_info",
                new String[]{"info_id", "info_name", "real_name", "phone", "address"},
                "username = ?",
                new String[]{username},
                null, null, null);

        while (cursor.moveToNext()) {
            infos.add(new UserInfo(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    null,
                    cursor.getString(3),
                    cursor.getString(4)
            ));
        }
        cursor.close();
        return infos;
    }

}