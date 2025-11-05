package com.team11.smartgym.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;
    public String password;

    public String name;
    public String surname;
    public String email;
    public int height;
    public String gender;

    public int activityFrequency;
}
