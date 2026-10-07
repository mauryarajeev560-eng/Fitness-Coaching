package com.fitness.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Feedback {
    private int id;
    private int userId;
    private String userName;
    private String userEmail;
    private String category;
    private String subject;
    private String message;
    private int rating;
    private String status; // PENDING, REVIEWED
    private String createdAt;

    public Feedback() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("user_id", userId);
        map.put("user_name", userName);
        map.put("user_email", userEmail);
        map.put("category", category);
        map.put("subject", subject);
        map.put("message", message);
        map.put("rating", rating);
        map.put("status", status);
        map.put("created_at", createdAt);
        return map;
    }
}
