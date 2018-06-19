package com.example.mulwa.mobilevisionapicamera.ROOM;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface ProviderDao {
    @Query("SELECT * FROM provider")
    List<Provider> getAll();

    @Query("SELECT * FROM provider WHERE id = :id")
    Provider getProvider(int id);

    @Insert
    void insertProvider(Provider provider);
}
