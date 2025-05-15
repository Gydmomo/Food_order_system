package com.example.drink_order_system;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class OrderedDrink {
	private Drinks drink;
	private Flavor flavor;
	private int quantity;
	private static List<OrderedDrink> cart = new ArrayList<>();

	// region 构造函数和基础方法
	public OrderedDrink(Drinks drink, Flavor flavor, int quantity) {
		this.drink = drink;
		this.flavor = flavor;
		this.quantity = quantity;
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

	public static void updateQuantity(int position, int delta) {
		OrderedDrink item = cart.get(position);
		item.quantity = Math.max(0, item.quantity + delta); // 防止负数
		if (item.quantity == 0) cart.remove(position);
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
	public static void saveCartToDatabase(SQLiteDatabase db, String username) { // 新增方法
		db.delete(DatabaseHelper.TABLE_CART_ITEMS,
				DatabaseHelper.COLUMN_USERNAME + " = ?",
				new String[]{username});

		for (OrderedDrink item : cart) {
			ContentValues values = new ContentValues();
			values.put(DatabaseHelper.COLUMN_USERNAME, username);
			values.put(DatabaseHelper.COLUMN_DRINK_ID, item.drink.getDrinkId());
			values.put(DatabaseHelper.COLUMN_SIZE, item.flavor.getSize());
			values.put(DatabaseHelper.COLUMN_TEMPERATURE, item.flavor.getTemperature());
			values.put(DatabaseHelper.COLUMN_SUGAR, item.flavor.getSugar());
			values.put(DatabaseHelper.COLUMN_QUANTITY, item.quantity);
			db.insert(DatabaseHelper.TABLE_CART_ITEMS, null, values);
		}
	}

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
				cart.add(new OrderedDrink(drink, flavor, quantity));
			}
		}
		cursor.close();
		return cart;
	}

	public void saveAsOrderItem(SQLiteDatabase db, String orderId) { // 重命名更清晰
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.COLUMN_ORDER_ID, orderId);
		values.put(DatabaseHelper.COLUMN_DRINK_ID, drink.getDrinkId());
		values.put(DatabaseHelper.COLUMN_SIZE, flavor.getSize());
		values.put(DatabaseHelper.COLUMN_TEMPERATURE, flavor.getTemperature());
		values.put(DatabaseHelper.COLUMN_SUGAR, flavor.getSugar());
		values.put(DatabaseHelper.COLUMN_QUANTITY, quantity);
		db.insert(DatabaseHelper.TABLE_ORDER_ITEMS, null, values);
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
	// endregion
}