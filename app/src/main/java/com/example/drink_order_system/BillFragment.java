package com.example.drink_order_system;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BillFragment extends android.app.Fragment {

    private String userName;
    private boolean isMerchant; // 新增角色状态
    private List<Order> orders = new ArrayList<>();
    private RecyclerView orderListView;
    private BillAdapter billAdapter;
    private DatabaseHelper dbHelper;

    public static BillFragment newInstance(String userName) {
        BillFragment fragment = new BillFragment();
        Bundle args = new Bundle();
        args.putString("userName", userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userName = getArguments().getString("userName");
        }
        dbHelper = new DatabaseHelper(getActivity());
        // 新增：从 SharedPreferences 获取角色
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        isMerchant = "merchant".equals(prefs.getString("role", "user"));
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bill, container, false);
        orderListView = view.findViewById(R.id.RV_bill);
        setupRecyclerView();
        return view;
    }

    private void setupRecyclerView() {
        orderListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        // 使用成员变量 orders 初始化 Adapter
        billAdapter = new BillAdapter(orders, getActivity());
        orderListView.setAdapter(billAdapter);
    }

    public List<Order> loadOrdersFromDatabase() {
        List<Order> orders = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                DatabaseHelper.COLUMN_ORDER_ID,
                DatabaseHelper.COLUMN_ORDER_DATE,
                DatabaseHelper.COLUMN_TOTAL,
                DatabaseHelper.COLUMN_TAKE_AWAY,
                "status",
                DatabaseHelper.COLUMN_USERNAME
        };

        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {userName};

        if (isMerchant) {
            // 商家查看所有订单
            selection = null;
            selectionArgs = null;
        } else {
            // 普通用户查看自己的订单
            selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
            selectionArgs = new String[]{userName};
        }
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_ORDERS,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                DatabaseHelper.COLUMN_ORDER_DATE + " DESC"
        );

        while (cursor.moveToNext()) {
            String orderId = cursor.getString(cursor.getColumnIndexOrThrow(
                    DatabaseHelper.COLUMN_ORDER_ID));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(
                    DatabaseHelper.COLUMN_ORDER_DATE));
            double total = cursor.getDouble(cursor.getColumnIndexOrThrow(
                    DatabaseHelper.COLUMN_TOTAL));
            int takeAway = cursor.getInt(cursor.getColumnIndexOrThrow(
                    DatabaseHelper.COLUMN_TAKE_AWAY));
            // 获取状态字段
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            String orderUser = cursor.getString(
                    cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME)
            );
            String status1 = takeAway == 1 ? "外带" : "堂食";
            String details = loadOrderDetails(db, orderId);
            String formattedTotal = String.format("%.1f", total);
            String dateText;
            if (isMerchant) {
                dateText = "用户: " + orderUser + "\n日期: " + date; // 商家显示用户信息
            } else {
                dateText = "日期: " + date; // 普通用户
            }
            orders.add(new Order(
                    "订单号: " + orderId,
                    dateText,
                    "￥" + formattedTotal,
                    status1,
                    details,
                    status // 修改这里，使用数据库中的状态值
            ));
        }
        cursor.close();
        db.close();
        return orders;
    }

    private String loadOrderDetails(SQLiteDatabase db, String orderId) {
        StringBuilder details = new StringBuilder();

        String query = "SELECT d." + DatabaseHelper.COLUMN_NAME +
                ", i." + DatabaseHelper.COLUMN_QUANTITY +
                ", i." + DatabaseHelper.COLUMN_SIZE +
                ", i." + DatabaseHelper.COLUMN_TEMPERATURE +
                ", i." + DatabaseHelper.COLUMN_SUGAR +
                " FROM " + DatabaseHelper.TABLE_ORDER_ITEMS + " i" +
                " INNER JOIN " + DatabaseHelper.TABLE_DRINKS + " d" +
                " ON i." + DatabaseHelper.COLUMN_DRINK_ID +
                " = d." + DatabaseHelper.COLUMN_DRINK_ID +
                " WHERE i." + DatabaseHelper.COLUMN_ORDER_ID + " = ?";

        Cursor cursor = db.rawQuery(query, new String[]{orderId});

        while (cursor.moveToNext()) {
            String drinkName = cursor.getString(0);
            int quantity = cursor.getInt(1);
            String size = cursor.getString(2);
            String temp = cursor.getString(3);
            String sugar = cursor.getString(4);

            details.append(String.format("%s x%d\n规格: %s\n温度: %s\n甜度: %s\n\n",
                    drinkName, quantity, size, temp, sugar));
        }
        cursor.close();
        return details.toString().trim();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            refreshData();
        }
    }

    // 修改refreshData方法为异步加载
    private void refreshData() {
        new Thread(() -> {
            List<Order> newOrders = loadOrdersFromDatabase();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // 清空并更新数据源
                    orders.clear();
                    orders.addAll(newOrders);

                    // 重要：重新设置 Adapter 引用
                    if (billAdapter == null) {
                        billAdapter = new BillAdapter(orders, getActivity());
                        orderListView.setAdapter(billAdapter);
                    } else {
                        billAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    // 添加公共刷新方法
    public void forceRefresh() {
        refreshData();
    }

}