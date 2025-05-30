package com.example.drink_order_system;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;


public class RootActivity extends AppCompatActivity {
    // 添加成员变量
    private Fragment fg_profile; // 新增我的界面
    private RadioGroup rg_tab_bar;
    private RadioButton rb_order;
    private Fragment fg_order; //点单界面
    private Fragment fg_shop; //购物车界面
    private Fragment fg_bill; //查看历史订单界面
    private FragmentManager fManager;
    private String userName;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root);

        userName = getIntent().getStringExtra("userName");
        System.out.println("username in oncreate in rootActivity "+userName);
        fManager = getFragmentManager();
        rg_tab_bar = (RadioGroup) findViewById(R.id.rg_tab);
        rg_tab_bar.setOnCheckedChangeListener(this::onCheckedChanged); //对下方导航栏设置监听，实现界面切换
        //获取第一个单选按钮，并设置其为选中状态
        rb_order = (RadioButton) findViewById(R.id.rb_order);
        rb_order.setChecked(true);
        defaultFragment(); //将点单界面设为默认界面
    }

    public void defaultFragment()
    {
        FragmentTransaction fTransaction = fManager.beginTransaction();
        fg_order = new OrderFragment();
        fTransaction.add(R.id.ly_content,fg_order);
        fTransaction.show(fg_order);
        fTransaction.commit();
    }

    public void onCheckedChanged(RadioGroup group, int checkedId) { //界面切换函数
        FragmentTransaction fTransaction = fManager.beginTransaction();
        hideAllFragment(fTransaction); //先隐藏所有界面
        switch (checkedId){
            case R.id.rb_order:
                if(fg_order==null)
                {
                    fg_order = (Fragment) new OrderFragment();
                    fTransaction.add(R.id.ly_content,fg_order);
                    fTransaction.show(fg_order);
                }
                else
                    fTransaction.show(fg_order);
                break;
            case R.id.rb_bill:
                if(fg_bill==null)
                {
                    fg_bill = BillFragment.newInstance(userName);
                    fTransaction.add(R.id.ly_content,fg_bill);
                    fTransaction.show(fg_bill);
                }
                else
                    fTransaction.show(fg_bill);
                break;
            case R.id.rb_shop:
                if(fg_shop==null)
                {
                    fg_shop = ShopFragment.newInstance(userName);
                    fTransaction.add(R.id.ly_content,fg_shop);
                    fTransaction.show(fg_shop);
                }
                else
                    fTransaction.show(fg_shop);
                break;
            case R.id.rb_profile:
                if (fg_profile == null) {
                    // 从SharedPreferences获取用户信息
                    // 修改后
                    SharedPreferences prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
                    String username = prefs.getString("current_username", "");
                    String role = prefs.getString("role", "customer");

                    fg_profile = ProfileFragment.newInstance(username, role);
                    fTransaction.add(R.id.ly_content, fg_profile);
                    fTransaction.show(fg_profile);
                } else {
                    fTransaction.show(fg_profile);
                }
                break;
        }
        fTransaction.commit();
    }

    //隐藏所有Fragment
    private void hideAllFragment(FragmentTransaction fragmentTransaction){
        if(fg_order!=null)fragmentTransaction.hide(fg_order);
        if(fg_bill!=null)fragmentTransaction.hide(fg_bill);
        if(fg_shop!=null)fragmentTransaction.hide(fg_shop);
        if (fg_profile != null) fragmentTransaction.hide(fg_profile); // 新增
    }
    // 添加刷新方法
    public void refreshShopFragment() {
        if (fg_shop != null && fg_shop instanceof ShopFragment) {
            ((ShopFragment) fg_shop).refreshDataFromDatabase();
        }
    }
    // 添加订单刷新方法
    public void refreshBillFragment() {
        if (fg_bill != null && fg_bill instanceof BillFragment) {
            ((BillFragment) fg_bill).forceRefresh();
        }
    }
}