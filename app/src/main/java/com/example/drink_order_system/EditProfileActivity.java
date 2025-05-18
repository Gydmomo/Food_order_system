package com.example.drink_order_system;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class EditProfileActivity extends AppCompatActivity {
    private EditText etRealName, etPhone, etAddress;
    private RadioGroup rgGender;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        dbHelper = new DatabaseHelper(this);
        initViews();
        loadExistingData();
    }

    private void initViews() {
        etRealName = findViewById(R.id.et_real_name);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        rgGender = findViewById(R.id.rg_gender);
        Button btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadExistingData() {
        String username = getIntent().getStringExtra("username");
        Cursor cursor = dbHelper.getUserInfo(username);

        if (cursor.moveToFirst()) {
            etRealName.setText(cursor.getString(0));
            etPhone.setText(cursor.getString(2));
            etAddress.setText(cursor.getString(4));

            String gender = cursor.getString(1);
            if ("男".equals(gender)) {
                rgGender.check(R.id.rb_male);
            } else if ("女".equals(gender)) {
                rgGender.check(R.id.rb_female);
            }
        }
        cursor.close();
    }

    private void saveProfile() {
        String realName = etRealName.getText().toString();
        String phone = etPhone.getText().toString();
        String address = etAddress.getText().toString();
        String gender = ((RadioButton)findViewById(rgGender.getCheckedRadioButtonId()))
                .getText().toString();

        String username = getIntent().getStringExtra("username");
        dbHelper.insertOrUpdateUserInfo(
                username,
                realName,
                gender,
                phone,
                address,
                "" // 头像路径暂留空
        );

        finish(); // 返回上一页
    }
}