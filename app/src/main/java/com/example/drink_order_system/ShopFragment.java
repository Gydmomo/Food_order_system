package com.example.drink_order_system;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.text.TextWatcher; // 添加这行

public class ShopFragment extends Fragment {
    private RecyclerView billListView;
    private LinearLayoutManager layoutManager;
    private TextView tvCost;
    private EditText etPeople;
    private CheckBox cbTakeAway;
    private Button btnBuy;
    private float totalDrinkCost;
    private float serviceCost = 0.2f;

    private AlertDialog buyDialog;
    private String userName;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 从参数获取用户名
        if (getArguments() != null) {
            userName = getArguments().getString("userName");
        }

        // 兜底：从 SharedPreferences 获取
        if (userName == null) {
            SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            userName = prefs.getString("current_username", "guest");
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        refreshDataFromDatabase();
    }

    public void refreshDataFromDatabase() {
        new Thread(() -> {
            List<OrderedDrink> newData = loadCartFromDatabase();
            getActivity().runOnUiThread(() -> {
                OrderedDrink.setCart(newData);
                if (billListView.getAdapter() != null) {
                    ((OrderAdapter) billListView.getAdapter()).updateData(newData);
                    updateCostDisplay();
                }
            });
        }).start();
    }
    private static class TextWatcherAdapter implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {}
    }
    public static ShopFragment newInstance(String userName) {
        ShopFragment fragment = new ShopFragment();
        Bundle args = new Bundle();
        args.putString("userName", userName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop, container, false);
        initViews(view);
        setupOrderList();
        setupEventListeners();
        return view;
    }

    private void initViews(View view) {
        billListView = view.findViewById(R.id.RV_bill);
        layoutManager = new LinearLayoutManager(getActivity());
        billListView.setLayoutManager(layoutManager);

        tvCost = view.findViewById(R.id.textView_cost);
        etPeople = view.findViewById(R.id.editText_people);
        cbTakeAway = view.findViewById(R.id.checkBox);
        btnBuy = view.findViewById(R.id.button_buy);

        view.findViewById(R.id.button_delete).setOnClickListener(v -> clearCart());
    }

    private void setupOrderList() {
        List<OrderedDrink> cartItems = loadCartFromDatabase();
        OrderedDrink.setCart(cartItems); // 更新内存中的购物车
        OrderAdapter adapter = new OrderAdapter(OrderedDrink.getCart());
        adapter.setOnOrderActionListener(new OrderAdapter.OnOrderActionListener() {
            @Override
            public void onAddClick(int position) {
                OrderedDrink.updateQuantity(position, 1);
                refreshData();
            }

            @Override
            public void onSubtractClick(int position) {
                OrderedDrink.updateQuantity(position, -1);
                refreshData();
            }
        });
        billListView.setAdapter(adapter);
        updateCostDisplay();
    }

    private void setupEventListeners() {
        etPeople.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                updateServiceCost();
                updateCostDisplay();
            }
        });

        btnBuy.setOnClickListener(v -> {
            if (OrderedDrink.getCart().isEmpty()) {
                Toast.makeText(getContext(), "请先选购饮品再结账！", Toast.LENGTH_SHORT).show();
            } else {
                showPaymentDialog();
            }
        });
    }

    private void updateServiceCost() {
        try {
            int peopleCount = Integer.parseInt(etPeople.getText().toString());
            serviceCost = 0.2f * peopleCount;
        } catch (NumberFormatException e) {
            serviceCost = 0.2f;
        }
    }

    private void updateCostDisplay() {
        totalDrinkCost = OrderedDrink.calculateTotal();
        String costText = String.format("饮料费：￥%.1f\n服务费：￥%.1f",
                totalDrinkCost, serviceCost);
        tvCost.setText(costText);
    }

    private void showPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialogue_buy, null);

        TextView tvTotal = dialogView.findViewById(R.id.textView_allCost);
        tvTotal.setText(String.format("总价：￥%.1f", totalDrinkCost + serviceCost));

        dialogView.findViewById(R.id.button_quit).setOnClickListener(v -> buyDialog.dismiss());

        dialogView.findViewById(R.id.button_bought).setOnClickListener(v -> {
            processPayment();
            buyDialog.dismiss();
        });

        builder.setView(dialogView);
        buyDialog = builder.create();
        buyDialog.show();
    }

    private void processPayment() {
        new Thread(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            try {
                // 1. 保存订单
                String orderId = generateOrderId();
                saveOrderToDatabase(db, orderId);

                // 2. 清空购物车
                db.delete(DatabaseHelper.TABLE_CART_ITEMS,
                        DatabaseHelper.COLUMN_USERNAME + " = ?",
                        new String[]{userName});

                getActivity().runOnUiThread(() -> {
                    OrderedDrink.clearCart();
                    // 此处调用刷新方法，并更新 Adapter 数据源
                    List<OrderedDrink> newCart = OrderedDrink.getCart(); // 应该为空
                    ((OrderAdapter) billListView.getAdapter()).updateData(newCart);
                    updateCostDisplay();
                    Toast.makeText(getActivity(), "支付成功！", Toast.LENGTH_SHORT).show();
                    // 1. 直接刷新 BillFragment
                    if (getActivity() instanceof RootActivity) {
                        ((RootActivity) getActivity()).refreshBillFragment();
                    }

                    // 2. 通过 View 查找确保刷新
                    FragmentManager fm = getActivity().getFragmentManager();
                    Fragment billFragment = fm.findFragmentByTag("bill_tag");
                    if (billFragment instanceof BillFragment) {
                        ((BillFragment) billFragment).forceRefresh();
                    }
                });

            } finally {
                db.close();
            }

        }).start();
    }
    // OrderFragment.java 中保存购物车的方法
    private void saveCartToDatabase() {
        new Thread(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            // 清空旧数据
            db.delete(DatabaseHelper.TABLE_CART_ITEMS,
                    DatabaseHelper.COLUMN_USERNAME + " = ?",
                    new String[]{userName});

            // 插入新数据
            for (OrderedDrink item : OrderedDrink.getCart()) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_USERNAME, userName);
                values.put(DatabaseHelper.COLUMN_DRINK_ID, item.getDrink().getDrinkId());
                values.put(DatabaseHelper.COLUMN_SIZE, item.getFlavor().getSize());
                values.put(DatabaseHelper.COLUMN_TEMPERATURE, item.getFlavor().getTemperature());
                values.put(DatabaseHelper.COLUMN_SUGAR, item.getFlavor().getSugar());
                values.put(DatabaseHelper.COLUMN_QUANTITY, item.getQuantity());
                db.insert(DatabaseHelper.TABLE_CART_ITEMS, null, values);
            }

            db.close();
        }).start();
    }
    private void clearCartFromDatabase(SQLiteDatabase db) {
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String username = prefs.getString("current_username", "guest");

        db.delete(
                DatabaseHelper.TABLE_CART_ITEMS,
                DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username}
        );
    }

    private void saveOrderToDatabase(SQLiteDatabase db, String orderId) {
        // 首先检查用户是否存在
        Cursor userCursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COLUMN_USERNAME},
                DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{userName},
                null, null, null
        );

        if (!userCursor.moveToFirst()) {
            // 用户不存在，先插入用户
            ContentValues userValues = new ContentValues();
            userValues.put(DatabaseHelper.COLUMN_USERNAME, userName);
            // 其他用户字段...
            db.insert(DatabaseHelper.TABLE_USERS, null, userValues);
        }
        userCursor.close();

        // 保存主订单
        ContentValues orderValues = new ContentValues();
        orderValues.put(DatabaseHelper.COLUMN_ORDER_ID, orderId);
        orderValues.put(DatabaseHelper.COLUMN_USERNAME, userName);
        orderValues.put(DatabaseHelper.COLUMN_TOTAL, totalDrinkCost + serviceCost);
        orderValues.put(DatabaseHelper.COLUMN_ORDER_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        orderValues.put(DatabaseHelper.COLUMN_TAKE_AWAY, cbTakeAway.isChecked() ? 1 : 0);
        orderValues.put("status", "备餐");
        long orderRowId = db.insert(DatabaseHelper.TABLE_ORDERS, null, orderValues);

        if (orderRowId == -1) {
            throw new RuntimeException("Failed to insert order");
        }

        // 保存订单明细
        for (OrderedDrink item : OrderedDrink.getCart()) {
            // 检查饮品是否存在
            Cursor drinkCursor = db.query(
                    DatabaseHelper.TABLE_DRINKS,
                    new String[]{DatabaseHelper.COLUMN_DRINK_ID},
                    DatabaseHelper.COLUMN_DRINK_ID + " = ?",
                    new String[]{String.valueOf(item.getDrink().getDrinkId())},
                    null, null, null
            );

            if (!drinkCursor.moveToFirst()) {
                throw new RuntimeException("Drink not found: " + item.getDrink().getDrinkId());
            }
            drinkCursor.close();

            item.saveAsOrderItem(db, orderId); // 使用正确的方法名
        }
    }

    private String generateOrderId() {
        String safeUsername = (userName != null) ? userName : "guest";
        return System.currentTimeMillis() + "-" + safeUsername.hashCode();
    }

    private void clearCart() {
        OrderedDrink.clearCart();
        refreshData();
        Toast.makeText(getContext(), "购物车已清空！", Toast.LENGTH_SHORT).show();
    }
    @SuppressLint("Range")
    private List<OrderedDrink> loadCartFromDatabase() {
        List<OrderedDrink> cart = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            String username = this.userName != null ? this.userName : "guest";
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_CART_ITEMS,
                    null,
                    DatabaseHelper.COLUMN_USERNAME + " = ?",
                    new String[]{username},
                    null, null, null
            );
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // 安全获取列索引
                    int drinkIdColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DRINK_ID);
                    int sizeColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SIZE);
                    int tempColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE);
                    int sugarColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUGAR);
                    int quantityColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_QUANTITY);

                    // 处理不存在的列
                    int drinkId = (drinkIdColIndex != -1) ? cursor.getInt(drinkIdColIndex) : -1;
                    String size = (sizeColIndex != -1) ? cursor.getString(sizeColIndex) : "标准";
                    String temp = (tempColIndex != -1) ? cursor.getString(tempColIndex) : "常温";
                    String sugar = (sugarColIndex != -1) ? cursor.getString(sugarColIndex) : "标准糖";
                    int quantity = (quantityColIndex != -1) ? cursor.getInt(quantityColIndex) : 1;

                    if (drinkId != -1) {
                        Drinks drink = getDrinkById(drinkId);
                        if (drink != null) {
                            cart.add(new OrderedDrink(drink, new Flavor(size, temp, sugar), quantity));
                        }
                    }
                }
                cursor.close();
            }
            return cart;
        } finally {
            db.close();
        }
    }
    @SuppressLint("Range")
    private Drinks getDrinkById(int drinkId) {
        DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try {
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_DRINKS,
                    null,
                    DatabaseHelper.COLUMN_DRINK_ID + " = ?",
                    new String[]{String.valueOf(drinkId)},
                    null, null, null
            );

            Drinks drink = null;
            if (cursor != null && cursor.moveToFirst()) {
                // 安全获取image_path列索引
                int imagePathIndex = cursor.getColumnIndex("image_path");
                String imagePath = (imagePathIndex != -1) ? cursor.getString(imagePathIndex) : "";

                // 处理空值
                if (imagePath == null) imagePath = "";

                drink = new Drinks(
                        cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_DRINK_ID)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TYPE)),
                        cursor.getFloat(cursor.getColumnIndex(DatabaseHelper.COLUMN_PRICE)),
                        cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_DESCRIPTION)),
                        imagePath
                );
            }
            if (cursor != null) {
                cursor.close();
            }
            return drink;
        } finally {
            db.close();
        }
    }
    private void refreshData() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (billListView.getAdapter() != null) {
                    ((OrderAdapter) billListView.getAdapter()).notifyDataSetChanged();
                    updateCostDisplay();
                }
            });
        }
    }
}