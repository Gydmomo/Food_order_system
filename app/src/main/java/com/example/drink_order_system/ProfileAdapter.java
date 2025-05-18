package com.example.drink_order_system;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileAdapter.ViewHolder> {
    private List<UserInfo> infos;
    private final OnProfileClickListener listener;

    public interface OnProfileClickListener {
        void onEditClick(UserInfo info);
        void onDeleteClick(UserInfo info);
    }

    public ProfileAdapter(List<UserInfo> infos, OnProfileClickListener listener) {
        this.infos = infos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserInfo info = infos.get(position);
        holder.tvInfoName.setText(info.getInfoName());
        holder.tvDetails.setText(formatDetails(info));

        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(info));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(info));
    }

    private String formatDetails(UserInfo info) {
        List<String> details = new ArrayList<>();

        // 带标签的字段组装
        if (!info.getRealName().isEmpty()) {
            details.add("姓名：" + info.getRealName());
        }
        if (!info.getGender().isEmpty()) {
            details.add("性别：" + info.getGender());
        }
        if (!info.getPhone().isEmpty()) {
            details.add("电话：" + info.getPhone());
        }
        if (!info.getAddress().isEmpty()) {
            details.add("地址：" + info.getAddress());
        }

        // 使用中文顿号分隔
        return TextUtils.join(" ・ ", details);
    }

    public void updateData(List<UserInfo> newInfos) {
        infos.clear();
        infos.addAll(newInfos);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return infos.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInfoName, tvDetails;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInfoName = itemView.findViewById(R.id.tv_info_name);
            tvDetails = itemView.findViewById(R.id.tv_details);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}