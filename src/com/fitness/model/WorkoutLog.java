package com.fitness.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorkoutLog {
    private int id;
    private int userId;
    private String userName;
    private Integer planId;
    private String planTitle;
    private String logDate;
    private int durationMinutes;
    private int caloriesBurned;
    private double weight;
    private String notes;
    private String createdAt;

    public WorkoutLog() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Integer getPlanId() { return planId; }
    public void setPlanId(Integer planId) { this.planId = planId; }

    public String getPlanTitle() { return planTitle; }
    public void setPlanTitle(String planTitle) { this.planTitle = planTitle; }

    public String getLogDate() { return logDate; }
    public void setLogDate(String logDate) { this.logDate = logDate; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(int caloriesBurned) { this.caloriesBurned = caloriesBurned; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("user_id", userId);
        map.put("user_name", userName);
        map.put("plan_id", planId);
        map.put("plan_title", planTitle);
        map.put("log_date", logDate);
        map.put("duration_minutes", durationMinutes);
        map.put("calories_burned", caloriesBurned);
        map.put("weight", weight);
        map.put("notes", notes);
        map.put("created_at", createdAt);
        return map;
    }
}
