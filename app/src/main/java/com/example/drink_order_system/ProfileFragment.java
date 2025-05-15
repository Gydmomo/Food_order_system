package com.example.drink_order_system;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import android.app.Fragment;

public class ProfileFragment extends Fragment {
    private String username;
    private String role;

    public static ProfileFragment newInstance(String username, String role) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("username", username);
        args.putString("role", role);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 获取参数
        if (getArguments() != null) {
            username = getArguments().getString("username");
            role = getArguments().getString("role");
        }

        initViews(view);
        return view;
    }

    private void initViews(View view) {
        TextView tvUsername = view.findViewById(R.id.tv_username);
        TextView tvRole = view.findViewById(R.id.tv_role);
        Button btnLogout = view.findViewById(R.id.btn_logout);

        // 显示用户信息
        tvUsername.setText("用户名：" + username);
        tvRole.setText("角色：" + (role.equals("merchant") ? "商家" : "顾客"));

        // 退出登录
        btnLogout.setOnClickListener(v -> {
            // 清除登录状态
            SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            // 跳转到登录界面
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }
}