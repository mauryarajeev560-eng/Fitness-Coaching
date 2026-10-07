package com.fitness.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlanExercise {
    private int id;
    private int planId;
    private String dayOfWeek;
    private String exerciseName;
    private int sets;
    private int reps;
    private int durationMins;
    private int restSeconds;
    private String notes;

    public PlanExercise() {}

    public PlanExercise(int id, int planId, String dayOfWeek, String exerciseName, int sets, int reps, int durationMins, int restSeconds, String notes) {
        this.id = id;
        this.planId = planId;
        this.dayOfWeek = dayOfWeek;
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
        this.durationMins = durationMins;
        this.restSeconds = restSeconds;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPlanId() { return planId; }
    public void setPlanId(int planId) { this.planId = planId; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public int getDurationMins() { return durationMins; }
    public void setDurationMins(int durationMins) { this.durationMins = durationMins; }

    public int getRestSeconds() { return restSeconds; }
    public void setRestSeconds(int restSeconds) { this.restSeconds = restSeconds; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("plan_id", planId);
        map.put("day_of_week", dayOfWeek);
        map.put("exercise_name", exerciseName);
        map.put("sets", sets);
        map.put("reps", reps);
        map.put("duration_mins", durationMins);
        map.put("rest_seconds", restSeconds);
        map.put("notes", notes);
        return map;
    }
}
