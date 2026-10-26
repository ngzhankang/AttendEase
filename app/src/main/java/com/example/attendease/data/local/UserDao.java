package com.example.attendease.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

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

    /** Students enrolled in a module who don't have a card linked yet. */
    @Query("SELECT u.* FROM users u " +
           "INNER JOIN enrollments e ON u.student_or_staff_id = e.student_id " +
           "WHERE e.module_id = :moduleId AND u.role = 'STUDENT' AND u.card_lid IS NULL " +
           "ORDER BY u.name")
    List<User> getUnlinkedStudentsForModule(long moduleId);

    @Query("UPDATE users SET card_lid = :lid WHERE student_or_staff_id = :studentId")
    void linkCard(String studentId, String lid);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(User user);

    @Update
    void update(User user);
}
