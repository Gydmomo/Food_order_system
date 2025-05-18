package com.example.drink_order_system;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;

@SuppressLint("ValidFragment")
public class EditProfileDialog extends DialogFragment {

    public interface OnSaveListener {
        void onSave(UserInfo savedInfo);
    }

    private UserInfo info;
    private OnSaveListener saveListener;

    public EditProfileDialog(UserInfo info) {
        this.info = info;
    }

    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_edit_profile, null);

        EditText etInfoName = view.findViewById(R.id.et_info_name);
        EditText etRealName = view.findViewById(R.id.et_real_name);
        RadioGroup rgGender = view.findViewById(R.id.rg_gender);
        EditText etPhone = view.findViewById(R.id.et_phone);
        EditText etAddress = view.findViewById(R.id.et_address);
        // 其他字段...

        // 填充现有数据
        if (info != null) {
            etInfoName.setText(info.getInfoName());
            etRealName.setText(info.getRealName());

            // 处理性别选择
            if("男".equals(info.getGender())) {
                rgGender.check(R.id.rb_male);
            } else {
                rgGender.check(R.id.rb_female);
            }

            etPhone.setText(info.getPhone());
            etAddress.setText(info.getAddress());
        }

        builder.setView(view)
                .setTitle("编辑信息")
                .setPositiveButton("保存", (dialog, id) -> {
                    // 获取所有输入值
                    info.setInfoName(etInfoName.getText().toString());
                    info.setRealName(etRealName.getText().toString());
                    info.setGender(rgGender.getCheckedRadioButtonId() == R.id.rb_male ? "男" : "女");
                    info.setPhone(etPhone.getText().toString());
                    info.setAddress(etAddress.getText().toString());

                    if (saveListener != null) {
                        saveListener.onSave(info);
                    }
                })
                .setNegativeButton("取消", (dialog, id) -> dismiss());
        return builder.create();
    }
}
