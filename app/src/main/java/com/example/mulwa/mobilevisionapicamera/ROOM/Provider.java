package com.example.mulwa.mobilevisionapicamera.ROOM;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class Provider {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String providerName;
    private int topUpCode;
    private int balanceCode;

//    constructor with id
    public Provider(int id, String providerName, int topUpCode, int balanceCode) {
        this.id = id;
        this.providerName = providerName;
        this.topUpCode = topUpCode;
        this.balanceCode = balanceCode;
    }

    public Provider(String providerName, int topUpCode, int balanceCode) {
        this.providerName = providerName;
        this.topUpCode = topUpCode;
        this.balanceCode = balanceCode;
    }

    public int getId() {
        return id;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public int getTopUpCode() {
        return topUpCode;
    }

    public void setTopUpCode(int topUpCode) {
        this.topUpCode = topUpCode;
    }

    public int getBalanceCode() {
        return balanceCode;
    }

    public void setBalanceCode(int balanceCode) {
        this.balanceCode = balanceCode;
    }
}
