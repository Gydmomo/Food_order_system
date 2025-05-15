package com.example.drink_order_system;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<OrderedDrink> orderList;
    private final NumberFormat currencyFormat;
    private OnOrderActionListener actionListener;

    public interface OnOrderActionListener {
        void onAddClick(int position);
        void onSubtractClick(int position);
    }

    public OrderAdapter(List<OrderedDrink> orderList) {
        this.orderList = orderList;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA);
        currencyFormat.setMaximumFractionDigits(2);
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bill_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        OrderedDrink currentOrder = orderList.get(position);
        Drinks drink = currentOrder.getDrink();
        Flavor flavor = currentOrder.getFlavor();
        int quantity = currentOrder.getQuantity();
        String imagePath = drink.getImagePath();
        // 图片加载
        if (!TextUtils.isEmpty(imagePath) && new File(imagePath).exists()) {
            Glide.with(holder.itemView.getContext())
                    .load(new File(imagePath))
                    .placeholder(R.drawable.ic_loading)
                    .error(R.drawable.ic_error)
                    .into(holder.drinkImage);
        } else {
            Glide.with(holder.itemView.getContext())
                    .load(R.drawable.ic_loading)
                    .into(holder.drinkImage);
        }

        // 绑定数据
        holder.drinkName.setText(drink.getName());
        holder.drinkFlavor.setText(flavor.toString());
        holder.drinkQuantity.setText(String.valueOf(quantity));

        // 计算价格
        float basePrice = drink.getPrice();
        switch (flavor.getSize()) {
            case "小杯": basePrice -= 2; break;
            case "大杯": basePrice += 2; break;
        }
        holder.drinkPrice.setText(currencyFormat.format(basePrice * quantity));

        // 设置点击事件
        holder.btnAdd.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onAddClick(position);
        });

        holder.btnSubtract.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onSubtractClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView drinkName;
        ImageView drinkImage;
        TextView drinkFlavor;
        TextView drinkPrice;
        TextView drinkQuantity;
        ImageButton btnAdd;
        ImageButton btnSubtract;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            drinkName = itemView.findViewById(R.id.Text_drinkName);
            drinkImage = itemView.findViewById(R.id.img_drink);
            drinkFlavor = itemView.findViewById(R.id.Text_drinkIntro);
            drinkPrice = itemView.findViewById(R.id.Text_drinkPrice);
            drinkQuantity = itemView.findViewById(R.id.textView_drinkNumber);
            btnAdd = itemView.findViewById(R.id.button_add);
            btnSubtract = itemView.findViewById(R.id.button_subtract);
        }
    }
    // 在OrderAdapter类中添加
    public void updateData(List<OrderedDrink> newData) {
        this.orderList = newData;
        notifyDataSetChanged();
    }
}