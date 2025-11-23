package com.team11.smartgym.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Insert
    void insertUser(User user);

    @Query("SELECT * FROM User WHERE username = :username")
    User getUserByUsername(String username);

    @Query("SELECT * FROM User WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT * FROM User WHERE id = :id LIMIT 1")
    User getUserById(int id);

    @Query("SELECT COUNT(*) FROM User WHERE email = :email")
    int countUsersByEmail(String email);

    @Update
    void updateUser(User user);
}
