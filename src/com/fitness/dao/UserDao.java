package com.fitness.dao;

import com.fitness.db.DatabaseManager;
import com.fitness.model.User;
import com.fitness.model.UserProfile;

import java.util.*;

public class UserDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public User authenticate(String email, String password) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?) AND password = ?;";
        Map<String, Object> row = db.queryOne(sql, Arrays.asList(email, password));
        if (row != null) {
            return mapUser(row);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?;";
        Map<String, Object> row = db.queryOne(sql, Collections.singletonList(id));
        if (row != null) {
            return mapUser(row);
        }
        return null;
    }

    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?);";
        Map<String, Object> row = db.queryOne(sql, Collections.singletonList(email));
        if (row != null) {
            return mapUser(row);
        }
        return null;
    }

    public List<User> findAll(String role, String status, String search) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("ALL")) {
            sql.append("AND role = ? ");
            params.add(role.toUpperCase());
        }
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            sql.append("AND status = ? ");
            params.add(status.toUpperCase());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(name) LIKE ? OR LOWER(email) LIKE ?) ");
            String like = "%" + search.toLowerCase().trim() + "%";
            params.add(like);
            params.add(like);
        }
        sql.append("ORDER BY id DESC;");

        List<Map<String, Object>> rows = db.query(sql.toString(), params);
        List<User> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            list.add(mapUser(row));
        }
        return list;
    }

    public long createUser(User user) {
        String sql = "INSERT INTO users (name, email, password, role, status, bio) VALUES (?, ?, ?, ?, ?, ?);";
        long id = db.executeInsert(sql, Arrays.asList(
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getStatus() != null ? user.getStatus() : "ACTIVE",
                user.getBio() != null ? user.getBio() : ""
        ));

        // Create empty profile if user is trainee
        if ("USER".equalsIgnoreCase(user.getRole())) {
            String profileSql = "INSERT OR IGNORE INTO user_profiles (user_id, age, gender, height, current_weight, target_weight, fitness_goal) " +
                    "VALUES (?, 25, 'Not Specified', 170.0, 70.0, 68.0, 'General Fitness');";
            db.executeUpdate(profileSql, Collections.singletonList(id));
        }
        return id;
    }

    public boolean updateUser(User user) {
        StringBuilder sql = new StringBuilder("UPDATE users SET name = ?, email = ?, role = ?, status = ?, bio = ?, updated_at = CURRENT_TIMESTAMP ");
        List<Object> params = new ArrayList<>(Arrays.asList(
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getBio()
        ));

        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            sql.append(", password = ? ");
            params.add(user.getPassword());
        }

        sql.append("WHERE id = ?;");
        params.add(user.getId());

        return db.executeUpdate(sql.toString(), params) > 0;
    }

    public boolean deleteUser(int id) {
        String sql = "DELETE FROM users WHERE id = ?;";
        return db.executeUpdate(sql, Collections.singletonList(id)) > 0;
    }

    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?;";
        return db.executeUpdate(sql, Arrays.asList(status, id)) > 0;
    }

    public UserProfile getUserProfile(int userId) {
        String sql = "SELECT * FROM user_profiles WHERE user_id = ?;";
        Map<String, Object> row = db.queryOne(sql, Collections.singletonList(userId));
        if (row != null) {
            UserProfile p = new UserProfile();
            p.setUserId(userId);
            if (row.get("age") instanceof Number) p.setAge(((Number) row.get("age")).intValue());
            p.setGender(row.get("gender") != null ? row.get("gender").toString() : "");
            if (row.get("height") instanceof Number) p.setHeight(((Number) row.get("height")).doubleValue());
            if (row.get("current_weight") instanceof Number) p.setCurrentWeight(((Number) row.get("current_weight")).doubleValue());
            if (row.get("target_weight") instanceof Number) p.setTargetWeight(((Number) row.get("target_weight")).doubleValue());
            p.setFitnessGoal(row.get("fitness_goal") != null ? row.get("fitness_goal").toString() : "");
            p.setUpdatedAt(row.get("updated_at") != null ? row.get("updated_at").toString() : "");
            return p;
        }
        return null;
    }

    public boolean saveOrUpdateProfile(UserProfile profile) {
        String sql = "INSERT INTO user_profiles (user_id, age, gender, height, current_weight, target_weight, fitness_goal, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP) " +
                "ON CONFLICT(user_id) DO UPDATE SET " +
                "age = excluded.age, gender = excluded.gender, height = excluded.height, " +
                "current_weight = excluded.current_weight, target_weight = excluded.target_weight, " +
                "fitness_goal = excluded.fitness_goal, updated_at = CURRENT_TIMESTAMP;";
        return db.executeUpdate(sql, Arrays.asList(
                profile.getUserId(),
                profile.getAge(),
                profile.getGender(),
                profile.getHeight(),
                profile.getCurrentWeight(),
                profile.getTargetWeight(),
                profile.getFitnessGoal()
        )) > 0;
    }

    public Map<String, Object> getRoleCounts() {
        String sql = "SELECT role, COUNT(*) as count FROM users GROUP BY role;";
        List<Map<String, Object>> rows = db.query(sql, Collections.emptyList());
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("ADMIN", 0);
        counts.put("COACH", 0);
        counts.put("USER", 0);
        for (Map<String, Object> r : rows) {
            String role = String.valueOf(r.get("role"));
            counts.put(role, r.get("count"));
        }
        return counts;
    }

    private User mapUser(Map<String, Object> row) {
        User u = new User();
        if (row.get("id") instanceof Number) u.setId(((Number) row.get("id")).intValue());
        u.setName(row.get("name") != null ? row.get("name").toString() : "");
        u.setEmail(row.get("email") != null ? row.get("email").toString() : "");
        u.setPassword(row.get("password") != null ? row.get("password").toString() : "");
        u.setRole(row.get("role") != null ? row.get("role").toString() : "");
        u.setStatus(row.get("status") != null ? row.get("status").toString() : "ACTIVE");
        u.setBio(row.get("bio") != null ? row.get("bio").toString() : "");
        u.setCreatedAt(row.get("created_at") != null ? row.get("created_at").toString() : "");
        u.setUpdatedAt(row.get("updated_at") != null ? row.get("updated_at").toString() : "");
        return u;
    }
}
