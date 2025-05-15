package com.example.drink_order_system;

public class Shop {
    private int shopId;
    private String shopName;
    private String username;

    public Shop(int shopId, String shopName, String username) {
        this.shopId = shopId;
        this.shopName = shopName;
        this.username = username;
    }

    // Getters and Setters
    public int getShopId() { return shopId; }
    public String getShopName() { return shopName; }
    public String getUsername() { return username; }
}