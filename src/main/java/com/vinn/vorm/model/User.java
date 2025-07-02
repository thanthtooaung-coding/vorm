package com.vinn.vorm.model;

import com.vinn.vorm.annotations.Entity;
import com.vinn.vorm.annotations.Id;
import lombok.Data;

@Entity(tableName = "users")
@Data
public class User {

    @Id
    private Long id;
    private String username;
    private String email;

    public User() {}
    
    public User(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }
}