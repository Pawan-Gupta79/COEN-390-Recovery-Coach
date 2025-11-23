package com.team11.smartgym.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;

    // Authentication (hashed password only)
    public String passwordHash;
    public String passwordSalt;
    public int passwordIterations;

    public String name;
    public String surname;
    public String email;
    public double height;
    public String gender;
    public int age;
    public double weight;

    public int activityFrequency;
}
