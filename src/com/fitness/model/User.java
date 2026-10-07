package com.fitness.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private String role; // ADMIN, COACH, USER
    private String status; // ACTIVE, INACTIVE
    private String bio;
    private String createdAt;
    private String updatedAt;

    public User() {}

    public User(int id, String name, String email, String password, String role, String status, String bio) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.bio = bio;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> toMap(boolean includePassword) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("email", email);
        if (includePassword) {
            map.put("password", password);
        }
        map.put("role", role);
        map.put("status", status);
        map.put("bio", bio);
        map.put("created_at", createdAt);
        map.put("updated_at", updatedAt);
        return map;
    }
}
