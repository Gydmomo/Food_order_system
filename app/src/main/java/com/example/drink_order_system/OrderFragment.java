package com.example.drink_order_system;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class OrderFragment extends Fragment {
    private long currentShopId = -1; // 新增：当前选中的商店ID
    private ArrayList<Drinks> filteredDrinks = new ArrayList<>(); // 新增：过滤后的饮品列表
    private ArrayList<LeftBean> shops_array = new ArrayList<>(); // 修改：存储商店信息
    private ArrayList<Drinks> drinks_array = new ArrayList<>();
    private ArrayList<LeftBean> titles_array = new ArrayList<>();
    private RecyclerView right_listView;
    private Right_adapter rightAdapter;
    private RecyclerView left_listView;
    private LinearLayoutManager right_llM;
    private TextView right_title;
    private SearchView searchView;
    private AlertDialog chooseDialog;
    private View view_choose;
    private Drinks selectedDrink;
    private Button btnAddDrink; // 新增

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order, container, false);

        // 初始化数据库
        DataInitializer.initializeDrinks(getActivity());

        // 初始化视图
        initViews(view, inflater);

        // 加载数据
        loadShopsFromDatabase();

        // 设置监听器
        setupScrollListener();
        setupSearchListener();

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        if (currentShopId != -1) {
            loadDrinksFromDatabase("", currentShopId);
        } else {
            loadShopsFromDatabase();
        }
        // 每次进入Fragment时刷新数据
        loadDrinksFromDatabase("",currentShopId);
    }
    // 新增：加载商店数据的方法
    @SuppressLint("Range")
    private void loadShopsFromDatabase() {
        new Thread(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            // 获取当前用户信息
            SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            String username = prefs.getString("current_username", "");
            String role = prefs.getString("role", "customer");
            boolean isMerchant = "merchant".equals(role);

            // 构建查询
            String selection = null;
            String[] selectionArgs = null;

            // 如果是商家，只显示自己的商店
            if (isMerchant) {
                selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
                selectionArgs = new String[]{username};
            }

            // 查询商店表
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_SHOPS,
                    null, selection, selectionArgs,
                    null, null, null
            );

            ArrayList<LeftBean> tempShops = new ArrayList<>();
            int position = 0;

            while (cursor.moveToNext()) {
                long shopId = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_SHOP_ID));
                String shopName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SHOP_NAME));
                tempShops.add(new LeftBean(position++, shopName, shopId));
            }

            cursor.close();
            db.close();

            // 更新UI
            getActivity().runOnUiThread(() -> {
                shops_array.clear();
                shops_array.addAll(tempShops);

                // 设置第一个商店为选中状态
                if (!shops_array.isEmpty()) {
                    shops_array.get(0).setSelect(true);
                    currentShopId = shops_array.get(0).getShopId();
                    loadDrinksFromDatabase("", currentShopId);
                    right_title.setText(shops_array.get(0).getTitle());
                }

                // 设置左侧适配器
                setupLeftAdapter();
            });
        }).start();
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            loadDrinksFromDatabase("",currentShopId); // 触发数据刷新
        }
    }
    private void initViews(View view, LayoutInflater inflater) {
        right_title = view.findViewById(R.id.Top_drinkType);
        right_listView = view.findViewById(R.id.rec_right);
        left_listView = view.findViewById(R.id.rec_left);
        searchView = view.findViewById(R.id.my_search);
        btnAddDrink = view.findViewById(R.id.btn_add_drink);
        // 新增：调用按钮初始化方法
        setupAddButton(); // <--- 添加这行代码
        // 初始化对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        view_choose = inflater.inflate(R.layout.dialogue_choose, null, false);
        builder.setView(view_choose).setCancelable(false);
        chooseDialog = builder.create();

        setupDialogButtons(view_choose);
        // 根据角色显示按钮
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        boolean isMerchant = "merchant".equals(prefs.getString("role", "user"));
        btnAddDrink.setVisibility(isMerchant ? View.VISIBLE : View.GONE);
    }
    // 新增：跳转到添加饮品界面
    private void setupAddButton() {
        btnAddDrink.setOnClickListener(v -> {
            // 使用 startActivityForResult
            Intent intent = new Intent(getActivity(), AddDrinkActivity.class);
            startActivityForResult(intent, 1001); // 请求码可自定义
        });
    }
    private void setupDialogButtons(View dialogView) {
        // 退出按钮
        dialogView.findViewById(R.id.button_exit).setOnClickListener(v -> chooseDialog.dismiss());

        // 购买按钮
        dialogView.findViewById(R.id.button_buy).setOnClickListener(v -> submitOrder());

        // 数量增减按钮
        dialogView.findViewById(R.id.button_subtract).setOnClickListener(v -> adjustQuantity(-1));
        dialogView.findViewById(R.id.button_add).setOnClickListener(v -> adjustQuantity(1));
    }

    private void submitOrder() {
        String size = getSelectedOption(R.id.radioGroup_size, "中杯");
        String temperature = getSelectedOption(R.id.radioGroup_ice, "全冰");
        String sugar = getSelectedOption(R.id.radioGroup_sugar, "全糖");
        int quantity = getCurrentQuantity();

        if (selectedDrink != null) {
            // 创建 Flavor 对象
            Flavor flavor = new Flavor(size, temperature, sugar);

            // 将饮品添加到购物车
            OrderedDrink orderedDrink = new OrderedDrink(selectedDrink, flavor, quantity);
            OrderedDrink.addToCart(orderedDrink);

            new Thread(() -> {
                try {
                    saveOrderToDatabase(size, temperature, sugar, quantity);
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "已加入购物车!", Toast.LENGTH_SHORT).show());
                } catch (Exception e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "操作失败!", Toast.LENGTH_SHORT).show());
                }
                chooseDialog.dismiss();
            }).start();
        }
        chooseDialog.dismiss();

        // 新增：通知购物车刷新
        if (getActivity() != null && getActivity() instanceof RootActivity) {
            ((RootActivity) getActivity()).refreshShopFragment();
        }
    }

    private void saveOrderToDatabase(String size, String temp, String sugar, int quantity) {
        DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String username = prefs.getString("current_username", "guest");

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_DRINK_ID, selectedDrink.getDrinkId());
        values.put(DatabaseHelper.COLUMN_SIZE, size);
        values.put(DatabaseHelper.COLUMN_TEMPERATURE, temp);
        values.put(DatabaseHelper.COLUMN_SUGAR, sugar);
        values.put(DatabaseHelper.COLUMN_QUANTITY, quantity);

        db.insert(DatabaseHelper.TABLE_CART_ITEMS, null, values);
        db.close();
    }

    // 修改：加载饮品数据的方法，增加shopId参数
    private void loadDrinksFromDatabase(String query, long shopId) {
        new Thread(() -> {
            SQLiteDatabase db = new DatabaseHelper(getActivity()).getReadableDatabase();
            String selection = DatabaseHelper.COLUMN_SHOP_ID + " = ?";
            String[] selectionArgs = {String.valueOf(shopId)};

            // 如果有搜索查询，添加名称条件
            if (!TextUtils.isEmpty(query)) {
                selection += " AND " + DatabaseHelper.COLUMN_NAME + " LIKE ?";
                selectionArgs = new String[]{String.valueOf(shopId), "%" + query + "%"};
            }
            Cursor cursor = db.query(
                    DatabaseHelper.TABLE_DRINKS,
                    null, selection, selectionArgs,
                    null, null, null
            );
            ArrayList<Drinks> tempList = new ArrayList<>();
            while (cursor.moveToNext()) {
                tempList.add(new Drinks(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getFloat(3),
                        cursor.getString(4),
                        cursor.getString(5)
                ));
            }
            cursor.close();
            db.close();
            // 更新UI
            getActivity().runOnUiThread(() -> {
                drinks_array.clear();
                drinks_array.addAll(tempList);
                filteredDrinks.clear();
                filteredDrinks.addAll(tempList);
                setupRightAdapter();
            });
        }).start();
    }
    // 新增：设置左侧适配器的方法
    private void setupLeftAdapter() {
        LeftAdapter leftAdapter = new LeftAdapter(shops_array);
        left_listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        left_listView.setAdapter(leftAdapter);
        // 点击事件
        leftAdapter.setOnItemClickListener(position -> {
            // 更新当前选中的商店ID
            currentShopId = shops_array.get(position).getShopId();
            right_title.setText(shops_array.get(position).getTitle());

            // 加载该商店的饮品
            loadDrinksFromDatabase("", currentShopId);
        });
    }
    // 新增：设置右侧适配器的方法
    private void setupRightAdapter() {
        // 获取当前用户角色
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String role = prefs.getString("role", "customer");
        boolean isMerchant = "merchant".equals(role);

        // 右侧适配器
        right_llM = new LinearLayoutManager(getActivity());
        right_listView.setLayoutManager(right_llM);
        rightAdapter = new Right_adapter(
                filteredDrinks,
                isMerchant,
                isMerchant ? position -> showDeleteConfirmDialog(position) : null
        );
        right_listView.setAdapter(rightAdapter);
        // 点击事件
        rightAdapter.setOnItemClickListener(position -> {
            selectedDrink = filteredDrinks.get(position);
            showOrderDialog(selectedDrink);
        });
    }


    private void updateCategoryTitles() {
        titles_array.clear();
        String currentType = "";
        for (int i = 0; i < drinks_array.size(); i++) {
            Drinks drink = drinks_array.get(i);
            if (drink.getType() != null && !drink.getType().equals(currentType)) {
                titles_array.add(new LeftBean(i, drink.getType()));
                currentType = drink.getType();
            }
        }
        if (!titles_array.isEmpty()) {
            titles_array.get(0).setSelect(true);
        }
    }

    private void setupAdapters() {
        // 获取当前用户角色
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String role = prefs.getString("role", "customer");
        boolean isMerchant = "merchant".equals(role);
        // 右侧适配器
        right_llM = new LinearLayoutManager(getActivity());
        right_listView.setLayoutManager(right_llM);
        rightAdapter = new Right_adapter(
                drinks_array,
                isMerchant,
                isMerchant ? new Right_adapter.OnDeleteClickListener() {
                    @Override
                    public void onDeleteClick(int position) {
                        showDeleteConfirmDialog(position);
                    }
                } : null // 非商家模式不需要监听器
        );
        right_listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        right_listView.setAdapter(rightAdapter);

        // 左侧适配器
        LeftAdapter leftAdapter = new LeftAdapter(titles_array);
        left_listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        left_listView.setAdapter(leftAdapter);

        // 点击事件
        rightAdapter.setOnItemClickListener(position -> {
            selectedDrink = drinks_array.get(position);
            showOrderDialog(selectedDrink);
        });

        leftAdapter.setOnItemClickListener(position ->
                right_llM.scrollToPositionWithOffset(position, 0));
    }
    private void showDeleteConfirmDialog(int position) {
        new AlertDialog.Builder(getActivity())
                .setTitle("确认删除")
                .setMessage("确定要删除这个饮品吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteDrink(position))
                .setNegativeButton("取消", null)
                .show();
    }
    // 修改：删除饮品方法
    private void deleteDrink(int position) {
        Drinks drinkToDelete = filteredDrinks.get(position);
        new Thread(() -> {
            DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            // 删除数据库记录
            int deletedRows = db.delete(
                    DatabaseHelper.TABLE_DRINKS,
                    DatabaseHelper.COLUMN_DRINK_ID + " = ?",
                    new String[]{String.valueOf(drinkToDelete.getDrinkId())}
            );
            db.close();
            getActivity().runOnUiThread(() -> {
                if (deletedRows > 0) {
                    // 更新UI
                    filteredDrinks.remove(position);
                    rightAdapter.notifyItemRemoved(position);
                    Toast.makeText(getActivity(), "删除成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "删除失败", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    private void showOrderDialog(Drinks drink) {
        if (view_choose != null) {
            ImageView drinkImage = view_choose.findViewById(R.id.choose_drink_img);
            String imagePath = drink.getImagePath();

            // 添加路径有效性验证
            if (imagePath != null && !imagePath.isEmpty()) {
                Glide.with(this)
                        .load(new File(imagePath))
                        .placeholder(R.drawable.ic_loading)
                        .error(R.drawable.ic_error)
                        .into(drinkImage);
            } else {
                Glide.with(this)
                        .load(R.drawable.ic_loading)
                        .into(drinkImage);
                Log.e("OrderFragment", "无效图片路径: " + imagePath);
            }
            ((TextView) view_choose.findViewById(R.id.choose_drinkName))
                    .setText(drink.getName() + "  #" + drink.getDrinkId());
            ((TextView) view_choose.findViewById(R.id.choose_drinkIntro))
                    .setText(drink.getDescription());
            ((TextView) view_choose.findViewById(R.id.textView_drinkNumber))
                    .setText("1");
        }
        chooseDialog.show();
    }

    // 辅助方法
    private String getSelectedOption(int radioGroupId, String defaultValue) {
        RadioGroup group = view_choose.findViewById(radioGroupId);
        int selectedId = group.getCheckedRadioButtonId();
        return selectedId != -1 ? ((RadioButton) view_choose.findViewById(selectedId)).getText().toString() : defaultValue;
    }

    private int getCurrentQuantity() {
        return Integer.parseInt(((TextView) view_choose.findViewById(R.id.textView_drinkNumber)).getText().toString());
    }

    private void adjustQuantity(int delta) {
        TextView tv = view_choose.findViewById(R.id.textView_drinkNumber);
        int current = Integer.parseInt(tv.getText().toString());
        int newValue = Math.max(1, Math.min(100, current + delta));
        tv.setText(String.valueOf(newValue));
    }

    private String generateOrderId() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date())
                + UUID.randomUUID().toString().substring(0, 4);
    }

    private String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(new Date());
    }

    private void setupScrollListener() {
        right_listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int firstVisible = right_llM.findFirstVisibleItemPosition();
                if (firstVisible != RecyclerView.NO_POSITION) {
                    right_title.setText(drinks_array.get(firstVisible).getType());
                }
            }
        });
    }

    // 修改：搜索监听器
    private void setupSearchListener() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (currentShopId != -1) {
                    loadDrinksFromDatabase(newText, currentShopId);
                }
                return true;
            }
        });
    }

}