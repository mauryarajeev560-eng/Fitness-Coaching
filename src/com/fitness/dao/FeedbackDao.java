package com.fitness.dao;

import com.fitness.db.DatabaseManager;
import com.fitness.model.Feedback;

import java.util.*;

public class FeedbackDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public long create(Feedback feedback) {
        String sql = "INSERT INTO user_feedback (user_id, category, subject, message, rating, status) " +
                "VALUES (?, ?, ?, ?, ?, ?);";
        return db.executeInsert(sql, Arrays.asList(
                feedback.getUserId(),
                feedback.getCategory(),
                feedback.getSubject(),
                feedback.getMessage(),
                feedback.getRating(),
                feedback.getStatus() != null ? feedback.getStatus() : "PENDING"
        ));
    }

    public List<Feedback> findAll(String statusFilter) {
        StringBuilder sql = new StringBuilder(
                "SELECT f.*, u.name as user_name, u.email as user_email " +
                "FROM user_feedback f " +
                "JOIN users u ON f.user_id = u.id " +
                "WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("ALL")) {
            sql.append("AND f.status = ? ");
            params.add(statusFilter.toUpperCase());
        }
        sql.append("ORDER BY f.id DESC;");

        List<Map<String, Object>> rows = db.query(sql.toString(), params);
        List<Feedback> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Feedback f = new Feedback();
            if (r.get("id") instanceof Number) f.setId(((Number) r.get("id")).intValue());
            if (r.get("user_id") instanceof Number) f.setUserId(((Number) r.get("user_id")).intValue());
            f.setUserName(r.get("user_name") != null ? r.get("user_name").toString() : "");
            f.setUserEmail(r.get("user_email") != null ? r.get("user_email").toString() : "");
            f.setCategory(r.get("category") != null ? r.get("category").toString() : "");
            f.setSubject(r.get("subject") != null ? r.get("subject").toString() : "");
            f.setMessage(r.get("message") != null ? r.get("message").toString() : "");
            if (r.get("rating") instanceof Number) f.setRating(((Number) r.get("rating")).intValue());
            f.setStatus(r.get("status") != null ? r.get("status").toString() : "PENDING");
            f.setCreatedAt(r.get("created_at") != null ? r.get("created_at").toString() : "");
            list.add(f);
        }
        return list;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE user_feedback SET status = ? WHERE id = ?;";
        return db.executeUpdate(sql, Arrays.asList(status, id)) > 0;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM user_feedback WHERE id = ?;";
        return db.executeUpdate(sql, Collections.singletonList(id)) > 0;
    }
}
