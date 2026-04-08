package com.example.attendease.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Query("SELECT * FROM users WHERE student_or_staff_id = :staffId LIMIT 1")
    User getUserByStaffId(String staffId);

    @Query("SELECT * FROM users WHERE student_or_staff_id = :staffId LIMIT 1")
    LiveData<User> observeUserByStaffId(String staffId);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    User getUserById(long id);

    @Query("SELECT * FROM users WHERE card_lid = :lid LIMIT 1")
    User getUserByLid(String lid);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(User user);

    @Update
    void update(User user);
}
