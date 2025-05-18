package com.example.drink_order_system;

import android.database.sqlite.SQLiteDatabase;

public class Drinks {
	private int drinkId;
	private String name;
	private String type;
	private float price;
	private String description;
	private String imagePath; // 替换原来的 imageResId
	private int shopId; // 新增字段
	private String shopName;
	public Drinks(int drinkId, String name, String type, float price, String description, String imagePath) {
		this.drinkId = drinkId;
		this.name = name;
		this.type = type;
		this.price = price;
		this.description = description;
		this.imagePath = imagePath;
	}

	public Drinks(int drinkId, String name, String type, float price, String description, String imagePath, int shopId) {
		this.drinkId = drinkId;
		this.name = name;
		this.type = type;
		this.price = price;
		this.description = description;
		this.imagePath = imagePath;
		this.shopId = shopId;
	}

	public int getShopId() {
		return shopId;
	}

	public void setShopId(int shopId) {
		this.shopId = shopId;
	}
	public String getShopName() {
		return shopName;
	}

	public void setShopName(String shopName) {
		this.shopName = shopName;
	}
	// region Getter Methods
	public int getDrinkId() { return drinkId; }
	public String getName() { return name; }
	public String getType() { return type; }
	public float getPrice() { return price; }
	public String getDescription() { return description; }

	public void setDrinkId(int drinkId) {
		this.drinkId = drinkId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setPrice(float price) {
		this.price = price;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getImagePath() {
		return imagePath;
	}

	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	// endregion

	// region Database Operations
	public static Drinks getById(SQLiteDatabase db, int drinkId) {
		// 实现从数据库查询的方法
		return null;
	}
	// endregion
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Drinks drinks = (Drinks) o;
		return drinkId == drinks.drinkId;
	}
}