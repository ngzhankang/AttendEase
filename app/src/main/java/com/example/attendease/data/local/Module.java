package com.example.attendease.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "modules")
public class Module {

    @PrimaryKey(autoGenerate = true)
    public long moduleId;

    @NonNull
    @ColumnInfo(name = "module_name")
    public String moduleName;

    @NonNull
    @ColumnInfo(name = "lecturer_id")
    public String lecturerId;

    public Module(@NonNull String moduleName, @NonNull String lecturerId) {
        this.moduleName = moduleName;
        this.lecturerId = lecturerId;
    }
}
