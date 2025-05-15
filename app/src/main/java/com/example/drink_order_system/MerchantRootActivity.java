package com.example.drink_order_system;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MerchantRootActivity extends AppCompatActivity {
    private Fragment fg_drinks;     // 饮品管理
    private Fragment fg_orders;     // 商家订单
    private Fragment fg_profile;    // 商家资料
    private RadioGroup rg_tab_bar;
    private FragmentManager fManager;
    private String userName;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.merchant_root);

        userName = getIntent().getStringExtra("userName");
        System.out.println("username in oncreate in rootActivity "+userName);
        // 初始化商家信息
        SharedPreferences prefs = getSharedPreferences("merchant_config", MODE_PRIVATE);
        String shopId = prefs.getString("shop_id", "");

        fManager = getFragmentManager();
        rg_tab_bar = findViewById(R.id.rg_tab);
        rg_tab_bar.setOnCheckedChangeListener(this::onTabChanged);

        // 默认显示饮品管理界面
        RadioButton rbDrinks = findViewById(R.id.rb_drinks);
        rbDrinks.setChecked(true);
        initDefaultFragment(shopId);
    }

    private void initDefaultFragment(String shopId) {
        FragmentTransaction ft = fManager.beginTransaction();
        fg_drinks = new OrderFragment();
        ft.replace(R.id.ly_content, fg_drinks);
        ft.commit();
    }

    public void onTabChanged(RadioGroup group, int checkedId) {
        FragmentTransaction ft = fManager.beginTransaction();
        hideAllFragments(ft);

        switch (checkedId) {
            case R.id.rb_drinks:
                if (fg_drinks == null) {
                    fg_drinks = (Fragment) new OrderFragment();
                    ft.add(R.id.ly_content,fg_drinks);
                    ft.show(fg_drinks);
                }
                ft.show(fg_drinks);
                break;

            case R.id.rb_orders:
                if (fg_orders == null) {
                    fg_orders = BillFragment.newInstance(userName);
                    ft.add(R.id.ly_content,fg_orders);
                    ft.show(fg_orders);
                }
                ft.show(fg_orders);
                break;

            case R.id.rb_profile:
                if (fg_profile == null) {
                    // 从SharedPreferences获取用户信息
                    // 修改后
                    SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    String username = prefs.getString("current_username", "");
                    String role = prefs.getString("role", "customer");

                    fg_profile = ProfileFragment.newInstance(username, role);
                    ft.add(R.id.ly_content, fg_profile);
                    ft.show(fg_profile);
                } else {
                    ft.show(fg_profile);
                }
                break;
        }
        ft.commit();
    }

    private void hideAllFragments(FragmentTransaction ft) {
        if (fg_drinks != null) ft.hide(fg_drinks);
        if (fg_orders != null) ft.hide(fg_orders);
        if (fg_profile != null) ft.hide(fg_profile);
    }

    private String getShopId() {
        SharedPreferences prefs = getSharedPreferences("merchant_config", MODE_PRIVATE);
        return prefs.getString("shop_id", "");
    }
}