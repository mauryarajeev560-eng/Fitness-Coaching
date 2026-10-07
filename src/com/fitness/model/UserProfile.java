package com.fitness.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class UserProfile {
    private int userId;
    private int age;
    private String gender;
    private double height;
    private double currentWeight;
    private double targetWeight;
    private String fitnessGoal;
    private String updatedAt;

    public UserProfile() {}

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public double getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(double currentWeight) { this.currentWeight = currentWeight; }

    public double getTargetWeight() { return targetWeight; }
    public void setTargetWeight(double targetWeight) { this.targetWeight = targetWeight; }

    public String getFitnessGoal() { return fitnessGoal; }
    public void setFitnessGoal(String fitnessGoal) { this.fitnessGoal = fitnessGoal; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", userId);
        map.put("age", age);
        map.put("gender", gender);
        map.put("height", height);
        map.put("current_weight", currentWeight);
        map.put("target_weight", targetWeight);
        map.put("fitness_goal", fitnessGoal);
        map.put("updated_at", updatedAt);
        return map;
    }
}
