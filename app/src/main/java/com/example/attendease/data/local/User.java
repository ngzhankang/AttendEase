package com.example.attendease.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users",
        indices = {@Index(value = {"student_or_staff_id"}, unique = true)})
public class User {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "student_or_staff_id")
    public String studentOrStaffId;

    @NonNull
    public String name;

    @NonNull
    @ColumnInfo(name = "password_hash")
    public String passwordHash;

    @NonNull
    public String role;

    @ColumnInfo(name = "card_lid")
    public String cardLid;

    public User(@NonNull String studentOrStaffId, @NonNull String name,
                @NonNull String passwordHash, @NonNull String role, String cardLid) {
        this.studentOrStaffId = studentOrStaffId;
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.cardLid = cardLid;
    }
}
