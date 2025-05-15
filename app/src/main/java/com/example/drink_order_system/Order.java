package com.example.drink_order_system;
import java.util.ArrayList;

public class Order {
	private final String orderNumber;
	private final String orderDate;
	private final String totalAmount;
	private final String orderType;
	private final String orderDetails;
	private String status; // 新增状态字段

	public Order(String number, String date, String total, String type, String details) {
		this.orderNumber = number;
		this.orderDate = date;
		this.totalAmount = total;
		this.orderType = type;
		this.orderDetails = details;
	}

	public Order(String orderNumber, String orderDate, String totalAmount, String orderType, String orderDetails, String status) {
		this.orderNumber = orderNumber;
		this.orderDate = orderDate;
		this.totalAmount = totalAmount;
		this.orderType = orderType;
		this.orderDetails = orderDetails;
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	// Getter方法
	public String getOrderNumber() { return orderNumber; }
	public String getOrderDate() { return orderDate; }
	public String getTotalAmount() { return totalAmount; }
	public String getOrderType() { return orderType; }
	public String getOrderDetails() { return orderDetails; }
}
