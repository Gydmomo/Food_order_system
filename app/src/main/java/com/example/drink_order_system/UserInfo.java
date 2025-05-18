package com.example.drink_order_system;

public class UserInfo {
    private int infoId;
    private String infoName;
    private String realName;
    private String gender;
    private String phone;
    private String address;

    // 构造函数
    public UserInfo(int infoId, String infoName, String realName,
                    String gender, String phone, String address) {
        this.infoId = infoId;
        this.infoName = infoName;
        this.realName = realName;
        this.gender = gender;
        this.phone = phone;
        this.address = address;
    }

    @Override
    public String toString() {
        return
                "真实姓名 ='" + realName + '\'' +
                ", 性别='" + gender + '\'' +
                ", 电话号码 ='" + phone + '\'' +
                ", 地址='" + address + '\'';
    }

    public void setInfoId(int infoId) {
        this.infoId = infoId;
    }

    public void setInfoName(String infoName) {
        this.infoName = infoName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    // Getter 方法
    public int getInfoId() { return infoId; }
    public String getInfoName() { return infoName; }
    public String getRealName() { return realName; }
    public String getGender() { return gender != null ? gender : ""; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }

}
