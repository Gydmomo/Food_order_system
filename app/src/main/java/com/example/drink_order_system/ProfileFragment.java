package com.example.drink_order_system;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment implements ProfileAdapter.OnProfileClickListener {
    private String username;
    private String role;
    private RecyclerView rvProfiles;
    private ProfileAdapter profileAdapter;
    private DatabaseHelper dbHelper;

    public static ProfileFragment newInstance(String username, String role) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("username", username);
        args.putString("role", role);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DatabaseHelper(getActivity());
        // 添加以下代码获取参数
        if (getArguments() != null) {
            username = getArguments().getString("username");
            role = getArguments().getString("role");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        // 初始化基础视图
        TextView tvUsername = view.findViewById(R.id.tv_username);
        TextView tvRole = view.findViewById(R.id.tv_role);
        Button btnLogout = view.findViewById(R.id.btn_logout);
        Button btnAdd = view.findViewById(R.id.btn_add_profile);
        rvProfiles = view.findViewById(R.id.rv_profiles);

        // 显示用户基础信息
        tvUsername.setText("用户名：" + username);
        tvRole.setText("角色：" + ("merchant".equals(role) ? "商家" : "顾客"));
        // 配置RecyclerView
        setupRecyclerView();

        // 按钮事件
        btnLogout.setOnClickListener(v -> handleLogout());
        btnAdd.setOnClickListener(v -> handleAddProfile());
    }

    private void setupRecyclerView() {
        // 布局管理器
        rvProfiles.setLayoutManager(new LinearLayoutManager(getActivity()));

        // 分割线
        rvProfiles.addItemDecoration(
                new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL)
        );

        // 适配器
        profileAdapter = new ProfileAdapter(new ArrayList<>(), this);
        rvProfiles.setAdapter(profileAdapter);

        // 加载数据
        loadProfileData();
    }

    private void loadProfileData() {
        new Thread(() -> {
            List<UserInfo> infos = dbHelper.getAllUserInfos(username);
            getActivity().runOnUiThread(() ->
                    profileAdapter.updateData(infos)
            );
        }).start();
    }

    @Override
    public void onEditClick(UserInfo info) {
        showEditDialog(info);
    }

    @Override
    public void onDeleteClick(UserInfo info) {
        new android.app.AlertDialog.Builder(getActivity())
                .setTitle("确认删除")
                .setMessage("确定要删除 " + info.getInfoName() + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    dbHelper.deleteUserInfo(info.getInfoId());
                    loadProfileData();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void handleAddProfile() {
        UserInfo newInfo = new UserInfo(0, "新建信息", "", "", "", "");
        showEditDialog(newInfo);
    }

    private void showEditDialog(UserInfo info) {
        EditProfileDialog dialog = new EditProfileDialog(info);
        dialog.setOnSaveListener(savedInfo -> {
            dbHelper.saveUserInfo(savedInfo, username);
            loadProfileData();
        });
        dialog.show(getFragmentManager(), "EditProfileDialog");
    }

    private void handleLogout() {
        // 清除登录状态
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // 清理数据库
        dbHelper.clearCurrentUserData(username);

        // 跳转登录
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}