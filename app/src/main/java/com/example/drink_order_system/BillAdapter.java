package com.example.drink_order_system;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.ViewHolder> {

    private final List<Order> orders;
    private final DatabaseHelper dbHelper;

    public BillAdapter(List<Order> orders, Context context) {
        this.orders = orders;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);

        // 设置基础信息
        holder.tvOrderNumber.setText(order.getOrderNumber());
        holder.tvOrderDate.setText(order.getOrderDate());
        holder.tvTotalAmount.setText(order.getTotalAmount());
        holder.tvOrderType.setText(order.getOrderType());
        holder.tvOrderDetails.setText(order.getOrderDetails());

        // 设置状态信息
        holder.tvOrderStatus.setText("状态：" + order.getStatus());

        // 按钮逻辑
        if ("备餐".equals(order.getStatus())) {
            holder.btnConfirm.setVisibility(View.VISIBLE);
            holder.btnConfirm.setOnClickListener(v -> {
                // 解析出原始订单号（去掉"订单号: "前缀）
                String displayedId = order.getOrderNumber();
                String originalId = displayedId.replace("订单号: ", "").trim(); // 清除前缀和空格

                // 更新本地数据
                order.setStatus("已送达");

                // 更新数据库（传递正确的原始ID）
                updateOrderStatus(originalId);

                // 刷新当前项
                notifyItemChanged(holder.getAdapterPosition());
            });
        } else {
            holder.btnConfirm.setVisibility(View.GONE);
        }
    }

    private void updateOrderStatus(String orderId) {
        new Thread(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            try {
                ContentValues values = new ContentValues();
                values.put("status", "已送达");

                db.update(DatabaseHelper.TABLE_ORDERS,
                        values,
                        DatabaseHelper.COLUMN_ORDER_ID + " = ?",
                        new String[]{orderId});
            } finally {
                db.close();
            }
        }).start();
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNumber;
        TextView tvOrderDate;
        TextView tvTotalAmount;
        TextView tvOrderType;
        TextView tvOrderDetails;
        TextView tvOrderStatus;
        Button btnConfirm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderNumber = itemView.findViewById(R.id.tv_order_number);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvTotalAmount = itemView.findViewById(R.id.tv_total_amount);
            tvOrderType = itemView.findViewById(R.id.tv_order_type);
            tvOrderDetails = itemView.findViewById(R.id.tv_order_details);

            // 新增视图绑定
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            btnConfirm = itemView.findViewById(R.id.btn_confirm);
        }
    }
}