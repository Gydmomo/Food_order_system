package com.example.drink_order_system;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class Right_adapter extends RecyclerView.Adapter<Right_adapter.RightViewHolder> {

    private final ArrayList<Drinks> mDrinksList;
    private OnItemClickListener mClickListener;
    private final NumberFormat mCurrencyFormat;
    private boolean mIsMerchant;
    // 接口定义
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }
    // 修改构造方法
    private OnDeleteClickListener mDeleteListener;
    public Right_adapter(ArrayList<Drinks> drinksList, boolean isMerchant) {
        this.mDrinksList = drinksList;
        this.mCurrencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA);
        mCurrencyFormat.setMaximumFractionDigits(2);
        this.mIsMerchant = isMerchant; // 设置是否是商家
    }
    public Right_adapter(ArrayList<Drinks> drinksList) {
        this.mDrinksList = drinksList;
        this.mCurrencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA);
        mCurrencyFormat.setMaximumFractionDigits(2);
    }
    // 修改构造方法
    public Right_adapter(ArrayList<Drinks> drinksList, boolean isMerchant, OnDeleteClickListener deleteListener) {
        this.mDrinksList = drinksList;
        this.mCurrencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA);
        mCurrencyFormat.setMaximumFractionDigits(2);
        this.mIsMerchant = isMerchant;
        this.mDeleteListener = deleteListener; // 新增删除监听
        // 添加验证
        if (isMerchant && deleteListener == null) {
            throw new IllegalArgumentException("商家模式必须提供删除监听器");
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mClickListener = listener;
    }

    @NonNull
    @Override
    public RightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item, parent, false);
        return new RightViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull RightViewHolder holder, int position) {
        Drinks currentDrink = mDrinksList.get(position);
        // 获取图片路径并验证有效性
        String imagePath = currentDrink.getImagePath();
        boolean isValidPath = !TextUtils.isEmpty(imagePath) && new File(imagePath).exists();
        // 图片加载逻辑
        if (isValidPath) {
            Glide.with(holder.itemView.getContext())
                    .load(new File(imagePath))
                    .placeholder(R.drawable.ic_loading)
                    .error(R.drawable.ic_error)
                    .centerCrop()
                    .into(holder.drinkImg);
        } else {
            // 路径无效时直接显示加载状态
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.ic_loading)
                    .into(holder.drinkImg);
        }
        // 绑定数据
        holder.drinkName.setText(currentDrink.getName());
        holder.drinkPrice.setText(mCurrencyFormat.format(currentDrink.getPrice()));
        holder.drinkIntro.setText(currentDrink.getDescription());
        // 根据角色设置按钮
        if (mIsMerchant) {
            holder.chooseBt.setText("删除");
            holder.chooseBt.setBackgroundColor(Color.RED);
            holder.chooseBt.setOnClickListener(v -> {
                if (mDeleteListener != null) {
                    mDeleteListener.onDeleteClick(position);
                }
            });
        } else {
            holder.chooseBt.setText("选规格");
            holder.chooseBt.setBackgroundResource(R.drawable.round_rect);
            holder.chooseBt.setOnClickListener(v -> {
                if (mClickListener != null) {
                    mClickListener.onItemClick(holder.getAdapterPosition());
                }
            });
        }
        // 处理类型显示
        if (currentDrink.getType() != null && !currentDrink.getType().isEmpty()) {
            holder.drinkType.setText(currentDrink.getType());
            holder.drinkType.setVisibility(View.VISIBLE);
        } else {
            holder.drinkType.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mDrinksList.size();
    }

    static class RightViewHolder extends RecyclerView.ViewHolder {
        final TextView drinkType;
        final TextView drinkName;
        final TextView drinkIntro;
        final TextView drinkPrice;
        final ImageView drinkImg;
        final Button chooseBt;

        public RightViewHolder(@NonNull View itemView) {
            super(itemView);
            drinkType = itemView.findViewById(R.id.Text_drinkType);
            drinkName = itemView.findViewById(R.id.Text_drinkName);
            drinkIntro = itemView.findViewById(R.id.Text_drinkIntro);
            drinkPrice = itemView.findViewById(R.id.Text_drinkPrice);
            drinkImg = itemView.findViewById(R.id.img_drink);
            chooseBt = itemView.findViewById(R.id.BT_choose);
        }
    }
}