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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    // 修改这里，使用正确的适配器类型
                    if (billListView.getAdapter() instanceof ShopGroupedOrderAdapter) {
                        ((ShopGroupedOrderAdapter) billListView.getAdapter()).updateData(newData);
                    }
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

        // 使用新的分组适配器
        ShopGroupedOrderAdapter adapter = new ShopGroupedOrderAdapter(OrderedDrink.getCart());
        // 原代码修改后：
        adapter.setOnOrderActionListener(new ShopGroupedOrderAdapter.OnOrderActionListener() {
            @Override
            public void onAddClick(int groupedPosition) {
                // 获取分组后的实际数据项
                Object item = adapter.getGroupedItems().get(groupedPosition);

                if (item instanceof OrderedDrink) {
                    // 在原始购物车中查找匹配项
                    int cartIndex = -1;
                    List<OrderedDrink> cart = OrderedDrink.getCart();
                    for (int i = 0; i < cart.size(); i++) {
                        if (cart.get(i).equals(item)) {
                            cartIndex = i;
                            break;
                        }
                    }

                    if (cartIndex != -1) {
                        OrderedDrink.updateQuantity(
                                getActivity(),
                                userName,
                                cartIndex,
                                1
                        );
                        refreshData();
                    }
                }
            }
            @Override
            public void onSubtractClick(int groupedPosition) {
                // 相同逻辑处理减少操作
                Object item = adapter.getGroupedItems().get(groupedPosition);

                if (item instanceof OrderedDrink) {
                    int cartIndex = -1;
                    List<OrderedDrink> cart = OrderedDrink.getCart();
                    for (int i = 0; i < cart.size(); i++) {
                        if (cart.get(i).equals(item)) {
                            cartIndex = i;
                            break;
                        }
                    }

                    if (cartIndex != -1) {
                        OrderedDrink.updateQuantity(
                                getActivity(),
                                userName,
                                cartIndex,
                                -1
                        );
                        refreshData();
                    }
                }
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
        // 按店铺分组计算费用
        Map<Integer, Float> shopTotals = new HashMap<>();
        Map<Integer, String> shopNames = new HashMap<>();

        for (OrderedDrink item : OrderedDrink.getCart()) {
            int shopId = item.getShopId();
            float itemTotal = item.getPrice() * item.getQuantity();

            if (!shopTotals.containsKey(shopId)) {
                shopTotals.put(shopId, 0f);
                shopNames.put(shopId, item.getShopName());
            }

            shopTotals.put(shopId, shopTotals.get(shopId) + itemTotal);
        }

        // 构建显示文本
        StringBuilder costText = new StringBuilder();
        totalDrinkCost = 0;

        for (Map.Entry<Integer, Float> entry : shopTotals.entrySet()) {
            int shopId = entry.getKey();
            float shopTotal = entry.getValue();
            totalDrinkCost += shopTotal;

            costText.append(shopNames.get(shopId))
                    .append("：￥")
                    .append(String.format("%.1f", shopTotal))
                    .append("\n");
        }

        // 添加服务费
        costText.append("服务费：￥").append(String.format("%.1f", serviceCost));

        tvCost.setText(costText.toString());
    }


    private void showPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialogView = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialogue_buy, null);

        TextView tvTotal = dialogView.findViewById(R.id.textView_allCost);

        // 按店铺分组计算费用
        Map<Integer, Float> shopTotals = new HashMap<>();
        Map<Integer, String> shopNames = new HashMap<>();
        float grandTotal = 0;

        for (OrderedDrink item : OrderedDrink.getCart()) {
            int shopId = item.getShopId();
            float itemTotal = item.getPrice() * item.getQuantity();

            if (!shopTotals.containsKey(shopId)) {
                shopTotals.put(shopId, 0f);
                shopNames.put(shopId, item.getShopName());
            }

            shopTotals.put(shopId, shopTotals.get(shopId) + itemTotal);
            grandTotal += itemTotal;
        }

        // 构建显示文本
        StringBuilder costText = new StringBuilder("订单明细：\n");

        for (Map.Entry<Integer, Float> entry : shopTotals.entrySet()) {
            int shopId = entry.getKey();
            float shopTotal = entry.getValue();

            costText.append(shopNames.get(shopId))
                    .append("：￥")
                    .append(String.format("%.1f", shopTotal))
                    .append("\n");
        }

        // 添加服务费和总价
        costText.append("服务费：￥").append(String.format("%.1f", serviceCost)).append("\n");
        costText.append("总价：￥").append(String.format("%.1f", grandTotal + serviceCost));

        tvTotal.setText(costText.toString());

        dialogView.findViewById(R.id.button_quit).setOnClickListener(v -> buyDialog.dismiss());
        Spinner infoSpinner = dialogView.findViewById(R.id.info_spinner);
        List<UserInfo> userInfos = loadUserInfos();
        ArrayAdapter<UserInfo> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, userInfos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        infoSpinner.setAdapter(adapter);
        dialogView.findViewById(R.id.button_bought).setOnClickListener(v -> {
            UserInfo selectedInfo = (UserInfo) infoSpinner.getSelectedItem();
            if(selectedInfo == null) {
                Toast.makeText(getActivity(), "请选择配送信息", Toast.LENGTH_SHORT).show();
                return;
            }
            processPayment(selectedInfo.getInfoId());
            buyDialog.dismiss();
        });

        builder.setView(dialogView);
        buyDialog = builder.create();
        buyDialog.show();
    }
    private List<UserInfo> loadUserInfos() {
        DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
        return dbHelper.getAllUserInfos(userName);
    }

    private void processPayment(int userInfoId) {
        new Thread(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            try {
                // 按店铺分组
                Map<Integer, List<OrderedDrink>> shopGroups = new HashMap<>();
                for (OrderedDrink item : OrderedDrink.getCart()) {
                    if (!shopGroups.containsKey(item.getShopId())) {
                        shopGroups.put(item.getShopId(), new ArrayList<>());
                    }
                    shopGroups.get(item.getShopId()).add(item);
                }

                // 为每个店铺创建一个订单
                for (Map.Entry<Integer, List<OrderedDrink>> entry : shopGroups.entrySet()) {
                    int shopId = entry.getKey();
                    List<OrderedDrink> shopItems = entry.getValue();

                    // 计算该店铺的订单总价
                    float shopTotal = 0;
                    for (OrderedDrink item : shopItems) {
                        shopTotal += item.getPrice() * item.getQuantity();
                    }

                    // 生成订单ID
                    String orderId = generateOrderId() + "-" + shopId;

                    // 保存订单
                    ContentValues orderValues = new ContentValues();
                    orderValues.put(DatabaseHelper.COLUMN_ORDER_ID, orderId);
                    orderValues.put(DatabaseHelper.COLUMN_USERNAME, userName);
                    orderValues.put(DatabaseHelper.COLUMN_TOTAL, shopTotal);
                    orderValues.put(DatabaseHelper.COLUMN_ORDER_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                    orderValues.put(DatabaseHelper.COLUMN_TAKE_AWAY, cbTakeAway.isChecked() ? 1 : 0);
                    orderValues.put("status", "备餐");
                    orderValues.put("shop_id", shopId); // 添加店铺ID
                    orderValues.put("user_info_id", userInfoId); // 添加个人信息ID
                    long orderRowId = db.insert(DatabaseHelper.TABLE_ORDERS, null, orderValues);

                    if (orderRowId == -1) {
                        throw new RuntimeException("Failed to insert order for shop: " + shopId);
                    }

                    // 保存订单明细
                    for (OrderedDrink item : shopItems) {
                        item.saveAsOrderItem(db, orderId);
                    }
                }

                // 清空购物车
                db.delete(DatabaseHelper.TABLE_CART_ITEMS,
                        DatabaseHelper.COLUMN_USERNAME + " = ?",
                        new String[]{userName});

                getActivity().runOnUiThread(() -> {
                    OrderedDrink.clearCart();
                    // 刷新适配器
                    ((ShopGroupedOrderAdapter) billListView.getAdapter()).updateData(new ArrayList<>());
                    updateCostDisplay();
                    Toast.makeText(getActivity(), "支付成功！", Toast.LENGTH_SHORT).show();

                    // 刷新账单页面
                    if (getActivity() instanceof RootActivity) {
                        ((RootActivity) getActivity()).refreshBillFragment();
                    }

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

            // 修改查询，加入店铺信息
            String query = "SELECT c.*, d.shop_id, s.shop_name " +
                    "FROM " + DatabaseHelper.TABLE_CART_ITEMS + " c " +
                    "JOIN " + DatabaseHelper.TABLE_DRINKS + " d ON c.drink_id = d.drink_id " +
                    "LEFT JOIN " + DatabaseHelper.TABLE_SHOPS + " s ON d.shop_id = s.shop_id " +
                    "WHERE c." + DatabaseHelper.COLUMN_USERNAME + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{username});

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // 安全获取列索引
                    int drinkIdColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_DRINK_ID);
                    int sizeColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SIZE);
                    int tempColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE);
                    int sugarColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_SUGAR);
                    int quantityColIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_QUANTITY);
                    int shopIdColIndex = cursor.getColumnIndex("shop_id");
                    int shopNameColIndex = cursor.getColumnIndex("shop_name");

                    // 处理不存在的列
                    int drinkId = (drinkIdColIndex != -1) ? cursor.getInt(drinkIdColIndex) : -1;
                    String size = (sizeColIndex != -1) ? cursor.getString(sizeColIndex) : "标准";
                    String temp = (tempColIndex != -1) ? cursor.getString(tempColIndex) : "常温";
                    String sugar = (sugarColIndex != -1) ? cursor.getString(sugarColIndex) : "标准糖";
                    int quantity = (quantityColIndex != -1) ? cursor.getInt(quantityColIndex) : 1;
                    int shopId = (shopIdColIndex != -1) ? cursor.getInt(shopIdColIndex) : 0;
                    String shopName = (shopNameColIndex != -1) ? cursor.getString(shopNameColIndex) : "未知店铺";

                    if (drinkId != -1) {
                        Drinks drink = getDrinkById(drinkId);
                        if (drink != null) {
                            cart.add(new OrderedDrink(drink, new Flavor(size, temp, sugar), quantity, shopId, shopName));
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
            String query = "SELECT d.*, s.shop_name " +
                    "FROM " + DatabaseHelper.TABLE_DRINKS + " d " +
                    "LEFT JOIN " + DatabaseHelper.TABLE_SHOPS + " s ON d.shop_id = s.shop_id " +
                    "WHERE d." + DatabaseHelper.COLUMN_DRINK_ID + " = ?";

            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(drinkId)});

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

                // 设置店铺ID
                int shopIdIndex = cursor.getColumnIndex("shop_id");
                if (shopIdIndex != -1) {
                    drink.setShopId(cursor.getInt(shopIdIndex));
                }

                // 设置店铺名称
                int shopNameIndex = cursor.getColumnIndex("shop_name");
                if (shopNameIndex != -1) {
                    drink.setShopName(cursor.getString(shopNameIndex));
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            return drink;
        } finally {
            db.close();
        }
    }

    private Map<Integer, List<OrderedDrink>> loadCartGroupedByShop() {
        Map<Integer, List<OrderedDrink>> shopGroups = new HashMap<>();
        List<OrderedDrink> cart = loadCartFromDatabase();

        for (OrderedDrink item : cart) {
            int shopId = item.getDrink().getShopId();
            if (!shopGroups.containsKey(shopId)) {
                shopGroups.put(shopId, new ArrayList<>());
            }
            shopGroups.get(shopId).add(item);
        }
        return shopGroups;
    }
    private void refreshData() {
        new Thread(() -> {
            // 从数据库重新加载最新数据
            List<OrderedDrink> newData = loadCartFromDatabase();

            getActivity().runOnUiThread(() -> {
                OrderedDrink.setCart(newData);

                if (billListView.getAdapter() != null) {
                    ((ShopGroupedOrderAdapter) billListView.getAdapter()).updateData(newData);
                    updateCostDisplay();
                }
            });
        }).start();
    }


}