package com.example.drink_order_system;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class OrderedDrink {
	private Drinks drink;
	private Flavor flavor;
	private int quantity;
	private int shopId; // 新增店铺ID
	private String shopName; // 新增店铺名称
	private static List<OrderedDrink> cart = new ArrayList<>();

	// region 构造函数和基础方法
	public OrderedDrink(Drinks drink, Flavor flavor, int quantity) {
		this.drink = drink;
		this.flavor = flavor;
		this.quantity = quantity;
	}
	public OrderedDrink(Drinks drink, Flavor flavor, int quantity, int shopId, String shopName) {
		this.drink = drink;
		this.flavor = flavor;
		this.quantity = quantity;
		this.shopId = shopId;
		this.shopName = shopName;
	}

	// 添加 getter 和 setter
	public int getShopId() {
		return shopId;
	}

	public String getShopName() {
		return shopName;
	}
	public Drinks getDrink() { return drink; }
	public Flavor getFlavor() { return flavor; }
	public int getQuantity() { return quantity; }
	public void setQuantity(int quantity) { this.quantity = quantity; } // 新增
	// endregion

	// region 购物车操作（内存）
	public static void setCart(List<OrderedDrink> items) { // 新增方法
		cart.clear();
		if (items != null) cart.addAll(items);
	}

	public static void addToCart(OrderedDrink item) {
		for (OrderedDrink existing : cart) {
			if (existing.isSameItem(item)) { // 使用新方法判断
				existing.quantity += item.quantity;
				return;
			}
		}
		cart.add(item);
	}

	public static void updateQuantity(Context context, String userName, int position, int delta) {
		if (position < 0 || position >= cart.size()) return;
		OrderedDrink item = cart.get(position);
		int newQuantity = item.getQuantity() + delta;
		// 创建新对象避免引用问题
		OrderedDrink newItem = new OrderedDrink(
				item.getDrink(),
				item.getFlavor(),
				newQuantity,
				item.getShopId(),
				item.getShopName()
		);
		if (newQuantity > 0) {
			cart.set(position, newItem); // 替换旧对象
		} else {
			cart.remove(position);
		}

		saveCartToDatabase(context, userName); // 立即保存
	}

	public static void clearCart() {
		cart.clear();
	}

	public static List<OrderedDrink> getCart() {
		return new ArrayList<>(cart);
	}

	private boolean isSameItem(OrderedDrink other) { // 新增：判断是否为相同商品
		return this.drink.getDrinkId() == other.drink.getDrinkId()
				&& this.flavor.equals(other.flavor);
	}
	// endregion

	// region 数据库操作
	public static void saveCartToDatabase(Context context, String userName) {
		new Thread(() -> {
			DatabaseHelper dbHelper = new DatabaseHelper(context);
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			try {
				db.beginTransaction();

				// 清空旧数据
				db.delete(DatabaseHelper.TABLE_CART_ITEMS,
						DatabaseHelper.COLUMN_USERNAME + " = ?",
						new String[]{userName});
				// 批量插入新数据
				for (OrderedDrink item : cart) {
					ContentValues values = new ContentValues();
					values.put(DatabaseHelper.COLUMN_USERNAME, userName);
					values.put(DatabaseHelper.COLUMN_DRINK_ID, item.getDrink().getDrinkId());
					values.put(DatabaseHelper.COLUMN_SIZE, item.getFlavor().getSize());
					values.put(DatabaseHelper.COLUMN_TEMPERATURE, item.getFlavor().getTemperature());
					values.put(DatabaseHelper.COLUMN_SUGAR, item.getFlavor().getSugar());
					values.put(DatabaseHelper.COLUMN_QUANTITY, item.getQuantity());
					values.put("shop_id", item.getShopId());
					db.insert(DatabaseHelper.TABLE_CART_ITEMS, null, values);
				}

				db.setTransactionSuccessful();
			} catch (Exception e) {
				Log.e("DB_ERROR", "保存购物车失败", e);
			} finally {
				db.endTransaction();
				db.close();
			}
		}).start();
	}

	@SuppressLint("Range")
	public static List<OrderedDrink> loadCartFromDatabase(SQLiteDatabase db, String username) { // 修改方法
		List<OrderedDrink> cart = new ArrayList<>();
		Cursor cursor = db.query(
				DatabaseHelper.TABLE_CART_ITEMS,
				null,
				DatabaseHelper.COLUMN_USERNAME + " = ?",
				new String[]{username},
				null, null, null
		);

		while (cursor.moveToNext()) {
			int drinkId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_DRINK_ID));
			Drinks drink = Drinks.getById(db, drinkId); // 需要Drinks类的支持

			if (drink != null) {
				Flavor flavor = new Flavor(
						cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SIZE)),
						cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE)),
						cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SUGAR))
				);
				int quantity = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_QUANTITY));
				int shopId = cursor.getInt(cursor.getColumnIndex("shop_id"));
				String shopName = ""; // 这里可能需要从其他表获取店铺名称

				cart.add(new OrderedDrink(drink, flavor, quantity, shopId, shopName));
			}
		}
		cursor.close();
		return cart;
	}

	public void saveAsOrderItem(SQLiteDatabase db, String orderId) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.COLUMN_ORDER_ID, orderId);
		values.put(DatabaseHelper.COLUMN_DRINK_ID, drink.getDrinkId());
		values.put(DatabaseHelper.COLUMN_SIZE, flavor.getSize());
		values.put(DatabaseHelper.COLUMN_TEMPERATURE, flavor.getTemperature());
		values.put(DatabaseHelper.COLUMN_SUGAR, flavor.getSugar());
		values.put(DatabaseHelper.COLUMN_QUANTITY, quantity);
		db.insert(DatabaseHelper.TABLE_ORDER_ITEMS, null, values); // 移除了price和shop_id
	}

	// endregion

	// region 工具方法
	public static float calculateTotal() {
		float total = 0;
		for (OrderedDrink item : cart) {
			total += item.getDrink().getPrice() * item.quantity;
		}
		return total;
	}

	@Override
	public boolean equals(Object obj) { // 增强对象比较
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		OrderedDrink that = (OrderedDrink) obj;
		return quantity == that.quantity
				&& drink.equals(that.drink)
				&& flavor.equals(that.flavor);
	}
	public float getPrice() {
		return drink.getPrice();
	}

	// endregion
}
