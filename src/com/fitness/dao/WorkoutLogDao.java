package com.fitness.dao;

import com.fitness.db.DatabaseManager;
import com.fitness.model.WorkoutLog;

import java.util.*;

public class WorkoutLogDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public long createLog(WorkoutLog log) {
        String sql = "INSERT INTO workout_logs (user_id, plan_id, log_date, duration_minutes, calories_burned, weight, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?);";
        long id = db.executeInsert(sql, Arrays.asList(
                log.getUserId(),
                log.getPlanId(),
                log.getLogDate(),
                log.getDurationMinutes(),
                log.getCaloriesBurned(),
                log.getWeight(),
                log.getNotes() != null ? log.getNotes() : ""
        ));

        // If weight logged, update user_profile current_weight
        if (log.getWeight() > 0) {
            String updateWeight = "UPDATE user_profiles SET current_weight = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?;";
            db.executeUpdate(updateWeight, Arrays.asList(log.getWeight(), log.getUserId()));
        }

        return id;
    }

    public boolean deleteLog(int logId, int userId) {
        String sql = "DELETE FROM workout_logs WHERE id = ? AND (user_id = ? OR ? = 0);";
        return db.executeUpdate(sql, Arrays.asList(logId, userId, userId)) > 0;
    }

    public List<WorkoutLog> getUserLogs(int userId) {
        String sql = "SELECT l.*, u.name as user_name, p.title as plan_title " +
                "FROM workout_logs l " +
                "JOIN users u ON l.user_id = u.id " +
                "LEFT JOIN workout_plans p ON l.plan_id = p.id " +
                "WHERE l.user_id = ? " +
                "ORDER BY l.log_date DESC, l.id DESC;";
        List<Map<String, Object>> rows = db.query(sql, Collections.singletonList(userId));
        List<WorkoutLog> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            list.add(mapLog(r));
        }
        return list;
    }

    public Map<String, Object> getUserProgressMetrics(int userId) {
        Map<String, Object> metrics = new LinkedHashMap<>();

        // Aggregate totals
        String aggSql = "SELECT COUNT(*) as total_workouts, " +
                "COALESCE(SUM(duration_minutes), 0) as total_duration, " +
                "COALESCE(SUM(calories_burned), 0) as total_calories, " +
                "COALESCE(AVG(calories_burned), 0) as avg_calories " +
                "FROM workout_logs WHERE user_id = ?;";
        Map<String, Object> aggRow = db.queryOne(aggSql, Collections.singletonList(userId));
        metrics.put("totals", aggRow != null ? aggRow : new HashMap<>());

        // Chronological log history (for charts)
        String trendSql = "SELECT log_date, calories_burned, duration_minutes, weight " +
                "FROM workout_logs WHERE user_id = ? ORDER BY log_date ASC, id ASC;";
        List<Map<String, Object>> history = db.query(trendSql, Collections.singletonList(userId));
        metrics.put("history", history);

        // Profile weight comparison
        String profileSql = "SELECT current_weight, target_weight FROM user_profiles WHERE user_id = ?;";
        Map<String, Object> profileRow = db.queryOne(profileSql, Collections.singletonList(userId));
        metrics.put("profile", profileRow != null ? profileRow : new HashMap<>());

        return metrics;
    }

    public List<Map<String, Object>> getCoachTraineesProgress(int coachId) {
        String sql = "SELECT u.id as user_id, u.name as user_name, u.email as user_email, " +
                "up.current_weight, up.target_weight, up.fitness_goal, " +
                "wp.title as active_plan_title, " +
                "COUNT(wl.id) as workouts_logged, " +
                "COALESCE(SUM(wl.calories_burned), 0) as total_calories, " +
                "MAX(wl.log_date) as last_workout_date " +
                "FROM user_plan_enrollments e " +
                "JOIN workout_plans wp ON e.plan_id = wp.id " +
                "JOIN users u ON e.user_id = u.id " +
                "LEFT JOIN user_profiles up ON u.id = up.user_id " +
                "LEFT JOIN workout_logs wl ON u.id = wl.user_id AND wl.plan_id = wp.id " +
                "WHERE wp.coach_id = ? AND e.status = 'ACTIVE' " +
                "GROUP BY u.id, wp.id " +
                "ORDER BY last_workout_date DESC, workouts_logged DESC;";
        return db.query(sql, Collections.singletonList(coachId));
    }

    public Map<String, Object> getGlobalWorkoutStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        String aggSql = "SELECT COUNT(*) as total_workouts, " +
                "COALESCE(SUM(calories_burned), 0) as total_calories, " +
                "COALESCE(SUM(duration_minutes), 0) as total_minutes, " +
                "COUNT(DISTINCT user_id) as active_trainees " +
                "FROM workout_logs;";
        Map<String, Object> aggRow = db.queryOne(aggSql, Collections.emptyList());
        stats.put("overall", aggRow != null ? aggRow : new HashMap<>());

        // Daily workout distribution for past logs
        String dailySql = "SELECT log_date, COUNT(*) as workout_count, SUM(calories_burned) as total_calories " +
                "FROM workout_logs " +
                "GROUP BY log_date " +
                "ORDER BY log_date ASC LIMIT 14;";
        List<Map<String, Object>> daily = db.query(dailySql, Collections.emptyList());
        stats.put("daily_trends", daily);

        return stats;
    }

    private WorkoutLog mapLog(Map<String, Object> r) {
        WorkoutLog l = new WorkoutLog();
        if (r.get("id") instanceof Number) l.setId(((Number) r.get("id")).intValue());
        if (r.get("user_id") instanceof Number) l.setUserId(((Number) r.get("user_id")).intValue());
        l.setUserName(r.get("user_name") != null ? r.get("user_name").toString() : "");
        if (r.get("plan_id") instanceof Number) l.setPlanId(((Number) r.get("plan_id")).intValue());
        l.setPlanTitle(r.get("plan_title") != null ? r.get("plan_title").toString() : "Custom Routine");
        l.setLogDate(r.get("log_date") != null ? r.get("log_date").toString() : "");
        if (r.get("duration_minutes") instanceof Number) l.setDurationMinutes(((Number) r.get("duration_minutes")).intValue());
        if (r.get("calories_burned") instanceof Number) l.setCaloriesBurned(((Number) r.get("calories_burned")).intValue());
        if (r.get("weight") instanceof Number) l.setWeight(((Number) r.get("weight")).doubleValue());
        l.setNotes(r.get("notes") != null ? r.get("notes").toString() : "");
        l.setCreatedAt(r.get("created_at") != null ? r.get("created_at").toString() : "");
        return l;
    }
}
