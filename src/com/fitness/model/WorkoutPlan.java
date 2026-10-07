package com.fitness.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkoutPlan {
    private int id;
    private int coachId;
    private String coachName;
    private String title;
    private String description;
    private String category;
    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED
    private int durationWeeks;
    private String approvalStatus; // PENDING, APPROVED, REJECTED
    private String moderationNotes;
    private String createdAt;
    private List<PlanExercise> exercises = new ArrayList<>();
    private int enrolledCount;

    public WorkoutPlan() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCoachId() { return coachId; }
    public void setCoachId(int coachId) { this.coachId = coachId; }

    public String getCoachName() { return coachName; }
    public void setCoachName(String coachName) { this.coachName = coachName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getDurationWeeks() { return durationWeeks; }
    public void setDurationWeeks(int durationWeeks) { this.durationWeeks = durationWeeks; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getModerationNotes() { return moderationNotes; }
    public void setModerationNotes(String moderationNotes) { this.moderationNotes = moderationNotes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<PlanExercise> getExercises() { return exercises; }
    public void setExercises(List<PlanExercise> exercises) { this.exercises = exercises; }

    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("coach_id", coachId);
        map.put("coach_name", coachName);
        map.put("title", title);
        map.put("description", description);
        map.put("category", category);
        map.put("difficulty", difficulty);
        map.put("duration_weeks", durationWeeks);
        map.put("approval_status", approvalStatus);
        map.put("moderation_notes", moderationNotes);
        map.put("created_at", createdAt);
        map.put("enrolled_count", enrolledCount);

        List<Map<String, Object>> exList = new ArrayList<>();
        if (exercises != null) {
            for (PlanExercise ex : exercises) {
                exList.add(ex.toMap());
            }
        }
        map.put("exercises", exList);
        return map;
    }
}
