package com.fitness.dao;

import com.fitness.db.DatabaseManager;
import com.fitness.model.PlanExercise;
import com.fitness.model.WorkoutPlan;

import java.util.*;

public class WorkoutPlanDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public List<WorkoutPlan> findAll(String approvalStatus, Integer coachId, String category, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT p.*, u.name as coach_name, " +
                "(SELECT COUNT(*) FROM user_plan_enrollments e WHERE e.plan_id = p.id AND e.status = 'ACTIVE') as enrolled_count " +
                "FROM workout_plans p " +
                "JOIN users u ON p.coach_id = u.id " +
                "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (approvalStatus != null && !approvalStatus.trim().isEmpty() && !approvalStatus.equalsIgnoreCase("ALL")) {
            sql.append("AND p.approval_status = ? ");
            params.add(approvalStatus.toUpperCase());
        }
        if (coachId != null && coachId > 0) {
            sql.append("AND p.coach_id = ? ");
            params.add(coachId);
        }
        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("ALL")) {
            sql.append("AND p.category = ? ");
            params.add(category);
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(p.title) LIKE ? OR LOWER(p.description) LIKE ?) ");
            String like = "%" + search.toLowerCase().trim() + "%";
            params.add(like);
            params.add(like);
        }
        sql.append("ORDER BY p.id DESC;");

        List<Map<String, Object>> rows = db.query(sql.toString(), params);
        List<WorkoutPlan> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            WorkoutPlan plan = mapPlan(row);
            plan.setExercises(getExercisesForPlan(plan.getId()));
            list.add(plan);
        }
        return list;
    }

    public WorkoutPlan findById(int id) {
        String sql = "SELECT p.*, u.name as coach_name, " +
                "(SELECT COUNT(*) FROM user_plan_enrollments e WHERE e.plan_id = p.id AND e.status = 'ACTIVE') as enrolled_count " +
                "FROM workout_plans p " +
                "JOIN users u ON p.coach_id = u.id " +
                "WHERE p.id = ?;";
        Map<String, Object> row = db.queryOne(sql, Collections.singletonList(id));
        if (row != null) {
            WorkoutPlan plan = mapPlan(row);
            plan.setExercises(getExercisesForPlan(id));
            return plan;
        }
        return null;
    }

    public long create(WorkoutPlan plan) {
        String sql = "INSERT INTO workout_plans (coach_id, title, description, category, difficulty, duration_weeks, approval_status, moderation_notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        long id = db.executeInsert(sql, Arrays.asList(
                plan.getCoachId(),
                plan.getTitle(),
                plan.getDescription() != null ? plan.getDescription() : "",
                plan.getCategory(),
                plan.getDifficulty(),
                plan.getDurationWeeks(),
                plan.getApprovalStatus() != null ? plan.getApprovalStatus() : "PENDING",
                plan.getModerationNotes() != null ? plan.getModerationNotes() : ""
        ));

        if (plan.getExercises() != null && !plan.getExercises().isEmpty()) {
            saveExercises((int) id, plan.getExercises());
        }
        return id;
    }

    public boolean update(WorkoutPlan plan) {
        String sql = "UPDATE workout_plans SET title = ?, description = ?, category = ?, difficulty = ?, duration_weeks = ? " +
                "WHERE id = ? AND (coach_id = ? OR ? = 1);"; // allow coach or admin (when coach_id matches or admin flag)
        int aff = db.executeUpdate(sql, Arrays.asList(
                plan.getTitle(),
                plan.getDescription(),
                plan.getCategory(),
                plan.getDifficulty(),
                plan.getDurationWeeks(),
                plan.getId(),
                plan.getCoachId(),
                plan.getCoachId() == 0 ? 1 : 0
        ));

        if (aff > 0 && plan.getExercises() != null) {
            saveExercises(plan.getId(), plan.getExercises());
        }
        return aff > 0;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM workout_plans WHERE id = ?;";
        return db.executeUpdate(sql, Collections.singletonList(id)) > 0;
    }

    public boolean updateApprovalStatus(int planId, String status, String moderationNotes) {
        String sql = "UPDATE workout_plans SET approval_status = ?, moderation_notes = ? WHERE id = ?;";
        return db.executeUpdate(sql, Arrays.asList(status, moderationNotes != null ? moderationNotes : "", planId)) > 0;
    }

    public List<PlanExercise> getExercisesForPlan(int planId) {
        String sql = "SELECT * FROM plan_exercises WHERE plan_id = ? ORDER BY id ASC;";
        List<Map<String, Object>> rows = db.query(sql, Collections.singletonList(planId));
        List<PlanExercise> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            PlanExercise ex = new PlanExercise();
            if (r.get("id") instanceof Number) ex.setId(((Number) r.get("id")).intValue());
            ex.setPlanId(planId);
            ex.setDayOfWeek(r.get("day_of_week") != null ? r.get("day_of_week").toString() : "");
            ex.setExerciseName(r.get("exercise_name") != null ? r.get("exercise_name").toString() : "");
            if (r.get("sets") instanceof Number) ex.setSets(((Number) r.get("sets")).intValue());
            if (r.get("reps") instanceof Number) ex.setReps(((Number) r.get("reps")).intValue());
            if (r.get("duration_mins") instanceof Number) ex.setDurationMins(((Number) r.get("duration_mins")).intValue());
            if (r.get("rest_seconds") instanceof Number) ex.setRestSeconds(((Number) r.get("rest_seconds")).intValue());
            ex.setNotes(r.get("notes") != null ? r.get("notes").toString() : "");
            list.add(ex);
        }
        return list;
    }

    public void saveExercises(int planId, List<PlanExercise> exercises) {
        // clear existing exercises
        db.executeUpdate("DELETE FROM plan_exercises WHERE plan_id = ?;", Collections.singletonList(planId));
        for (PlanExercise ex : exercises) {
            String sql = "INSERT INTO plan_exercises (plan_id, day_of_week, exercise_name, sets, reps, duration_mins, rest_seconds, notes) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
            db.executeInsert(sql, Arrays.asList(
                    planId,
                    ex.getDayOfWeek(),
                    ex.getExerciseName(),
                    ex.getSets(),
                    ex.getReps(),
                    ex.getDurationMins(),
                    ex.getRestSeconds(),
                    ex.getNotes() != null ? ex.getNotes() : ""
            ));
        }
    }

    public boolean enrollUser(int userId, int planId) {
        String sql = "INSERT INTO user_plan_enrollments (user_id, plan_id, status) VALUES (?, ?, 'ACTIVE') " +
                "ON CONFLICT(user_id, plan_id) DO UPDATE SET status = 'ACTIVE', enrolled_at = CURRENT_TIMESTAMP;";
        return db.executeUpdate(sql, Arrays.asList(userId, planId)) > 0;
    }

    public boolean unenrollUser(int userId, int planId) {
        String sql = "UPDATE user_plan_enrollments SET status = 'DROPPED' WHERE user_id = ? AND plan_id = ?;";
        return db.executeUpdate(sql, Arrays.asList(userId, planId)) > 0;
    }

    public boolean isUserEnrolled(int userId, int planId) {
        String sql = "SELECT COUNT(*) as count FROM user_plan_enrollments WHERE user_id = ? AND plan_id = ? AND status = 'ACTIVE';";
        Map<String, Object> row = db.queryOne(sql, Arrays.asList(userId, planId));
        if (row != null && row.get("count") instanceof Number) {
            return ((Number) row.get("count")).intValue() > 0;
        }
        return false;
    }

    public List<WorkoutPlan> getUserEnrolledPlans(int userId) {
        String sql = "SELECT p.*, u.name as coach_name, " +
                "(SELECT COUNT(*) FROM user_plan_enrollments e2 WHERE e2.plan_id = p.id AND e2.status = 'ACTIVE') as enrolled_count " +
                "FROM workout_plans p " +
                "JOIN users u ON p.coach_id = u.id " +
                "JOIN user_plan_enrollments e ON e.plan_id = p.id " +
                "WHERE e.user_id = ? AND e.status = 'ACTIVE' " +
                "ORDER BY e.enrolled_at DESC;";
        List<Map<String, Object>> rows = db.query(sql, Collections.singletonList(userId));
        List<WorkoutPlan> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            WorkoutPlan p = mapPlan(row);
            p.setExercises(getExercisesForPlan(p.getId()));
            list.add(p);
        }
        return list;
    }

    public Map<String, Object> getPlanStatusCounts() {
        String sql = "SELECT approval_status, COUNT(*) as count FROM workout_plans GROUP BY approval_status;";
        List<Map<String, Object>> rows = db.query(sql, Collections.emptyList());
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("APPROVED", 0);
        counts.put("PENDING", 0);
        counts.put("REJECTED", 0);
        for (Map<String, Object> r : rows) {
            String status = String.valueOf(r.get("approval_status"));
            counts.put(status, r.get("count"));
        }
        return counts;
    }

    public List<Map<String, Object>> getCoachPlanAnalytics(int coachId) {
        String sql = "SELECT p.id, p.title, p.category, p.difficulty, p.approval_status, " +
                "COUNT(DISTINCT e.user_id) as total_enrolled, " +
                "COUNT(l.id) as total_workouts_logged, " +
                "COALESCE(AVG(l.calories_burned), 0) as avg_calories " +
                "FROM workout_plans p " +
                "LEFT JOIN user_plan_enrollments e ON p.id = e.plan_id AND e.status = 'ACTIVE' " +
                "LEFT JOIN workout_logs l ON p.id = l.plan_id " +
                "WHERE p.coach_id = ? " +
                "GROUP BY p.id " +
                "ORDER BY total_enrolled DESC, p.id DESC;";
        return db.query(sql, Collections.singletonList(coachId));
    }

    private WorkoutPlan mapPlan(Map<String, Object> row) {
        WorkoutPlan p = new WorkoutPlan();
        if (row.get("id") instanceof Number) p.setId(((Number) row.get("id")).intValue());
        if (row.get("coach_id") instanceof Number) p.setCoachId(((Number) row.get("coach_id")).intValue());
        p.setCoachName(row.get("coach_name") != null ? row.get("coach_name").toString() : "");
        p.setTitle(row.get("title") != null ? row.get("title").toString() : "");
        p.setDescription(row.get("description") != null ? row.get("description").toString() : "");
        p.setCategory(row.get("category") != null ? row.get("category").toString() : "");
        p.setDifficulty(row.get("difficulty") != null ? row.get("difficulty").toString() : "");
        if (row.get("duration_weeks") instanceof Number) p.setDurationWeeks(((Number) row.get("duration_weeks")).intValue());
        p.setApprovalStatus(row.get("approval_status") != null ? row.get("approval_status").toString() : "PENDING");
        p.setModerationNotes(row.get("moderation_notes") != null ? row.get("moderation_notes").toString() : "");
        p.setCreatedAt(row.get("created_at") != null ? row.get("created_at").toString() : "");
        if (row.get("enrolled_count") instanceof Number) p.setEnrolledCount(((Number) row.get("enrolled_count")).intValue());
        return p;
    }
}
