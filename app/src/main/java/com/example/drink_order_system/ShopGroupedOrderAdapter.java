package com.example.drink_order_system;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopGroupedOrderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SHOP_HEADER = 0;
    private static final int TYPE_DRINK_ITEM = 1;

    private List<Object> groupedItems = new ArrayList<>();
    private OnOrderActionListener listener;

    // 构造函数，接收原始购物车数据并进行分组
    public ShopGroupedOrderAdapter(List<OrderedDrink> cartItems) {
        groupItemsByShop(cartItems);
    }

    // 按店铺分组
    private void groupItemsByShop(List<OrderedDrink> cartItems) {
        groupedItems.clear();

        // 按店铺ID分组
        Map<Integer, List<OrderedDrink>> shopGroups = new HashMap<>();
        for (OrderedDrink item : cartItems) {
            if (!shopGroups.containsKey(item.getShopId())) {
                shopGroups.put(item.getShopId(), new ArrayList<>());
            }
            shopGroups.get(item.getShopId()).add(item);
        }

        // 将分组后的数据添加到列表中
        for (Map.Entry<Integer, List<OrderedDrink>> entry : shopGroups.entrySet()) {
            // 添加店铺头部
            if (!entry.getValue().isEmpty()) {
                groupedItems.add(new ShopHeader(entry.getKey(), entry.getValue().get(0).getShopName()));
                // 添加该店铺的所有饮品
                groupedItems.addAll(entry.getValue());
            }
        }
    }

    public void updateData(List<OrderedDrink> newData) {
        groupItemsByShop(newData);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return groupedItems.get(position) instanceof ShopHeader ? TYPE_SHOP_HEADER : TYPE_DRINK_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SHOP_HEADER) {
            View view = inflater.inflate(R.layout.item_shop_header, parent, false);
            return new ShopHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_cart_drink, parent, false);
            return new DrinkItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ShopHeaderViewHolder) {
            ShopHeader header = (ShopHeader) groupedItems.get(position);
            ((ShopHeaderViewHolder) holder).bind(header);
        } else if (holder instanceof DrinkItemViewHolder) {
            OrderedDrink drink = (OrderedDrink) groupedItems.get(position);
            ((DrinkItemViewHolder) holder).bind(drink, position, listener);
        }
    }

    @Override
    public int getItemCount() {
        return groupedItems.size();
    }

    public List<Object> getGroupedItems() {
        return groupedItems;
    }

    // 店铺头部数据类
    private static class ShopHeader {
        int shopId;
        String shopName;

        ShopHeader(int shopId, String shopName) {
            this.shopId = shopId;
            this.shopName = shopName;
        }
    }

    // 店铺头部ViewHolder
    private static class ShopHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvShopName;

        ShopHeaderViewHolder(View itemView) {
            super(itemView);
            tvShopName = itemView.findViewById(R.id.tv_shop_name);
        }

        void bind(ShopHeader header) {
            tvShopName.setText(header.shopName);
        }
    }

    // 饮品项ViewHolder
    private static class DrinkItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvFlavor, tvPrice, tvQuantity;
        ImageButton btnAdd, btnSubtract;

        DrinkItemViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.textView_name);
            tvFlavor = itemView.findViewById(R.id.textView_flavor);
            tvPrice = itemView.findViewById(R.id.textView_price);
            tvQuantity = itemView.findViewById(R.id.textView_quantity);
            btnAdd = itemView.findViewById(R.id.imageButton_add);
            btnSubtract = itemView.findViewById(R.id.imageButton_subtract);
        }

        void bind(OrderedDrink drink, int position, OnOrderActionListener listener) {
            tvName.setText(drink.getDrink().getName());
            tvFlavor.setText(drink.getFlavor().toString());
            tvPrice.setText(String.format("￥%.1f", drink.getDrink().getPrice()));
            tvQuantity.setText(String.valueOf(drink.getQuantity()));

            btnAdd.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddClick(position);
                }
            });

            btnSubtract.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSubtractClick(position);
                }
            });
        }
    }

    public interface OnOrderActionListener {
        void onAddClick(int position);
        void onSubtractClick(int position);
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.listener = listener;
    }
}
