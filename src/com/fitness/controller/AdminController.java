package com.fitness.controller;

import com.fitness.dao.*;
import com.fitness.model.Feedback;
import com.fitness.model.SystemSetting;
import com.fitness.model.User;
import com.fitness.model.WorkoutPlan;
import com.fitness.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

public class AdminController implements HttpHandler {
    private final UserDao userDao = new UserDao();
    private final WorkoutPlanDao planDao = new WorkoutPlanDao();
    private final WorkoutLogDao logDao = new WorkoutLogDao();
    private final FeedbackDao feedbackDao = new FeedbackDao();
    private final SystemSettingsDao settingsDao = new SystemSettingsDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtil.setCorsHeaders(exchange);
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String path = exchange.getRequestURI().getPath();

        try {
            // User Management
            if (path.equals("/api/admin/users")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleListUsers(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleCreateUser(exchange);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    handleUpdateUser(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteUser(exchange);
                } else {
                    HttpUtil.sendErrorResponse(exchange, 405, "Method not allowed");
                }
            } else if (path.equals("/api/admin/users/status") && "PUT".equalsIgnoreCase(method)) {
                handleToggleUserStatus(exchange);
            }
            // Content Moderation
            else if (path.equals("/api/admin/content") && "GET".equalsIgnoreCase(method)) {
                handleListContent(exchange);
            } else if (path.equals("/api/admin/content/moderate") && "POST".equalsIgnoreCase(method)) {
                handleModerateContent(exchange);
            }
            // System Settings
            else if (path.equals("/api/admin/settings")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetSettings(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleUpdateSettings(exchange);
                } else {
                    HttpUtil.sendErrorResponse(exchange, 405, "Method not allowed");
                }
            }
            // Content Overview & Statistics
            else if (path.equals("/api/admin/overview") && "GET".equalsIgnoreCase(method)) {
                handleOverviewStats(exchange);
            }
            // User Feedback
            else if (path.equals("/api/admin/feedback")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleListFeedback(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleUpdateFeedbackStatus(exchange);
                } else {
                    HttpUtil.sendErrorResponse(exchange, 405, "Method not allowed");
                }
            } else {
                HttpUtil.sendErrorResponse(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (Exception e) {
            HttpUtil.sendErrorResponse(exchange, 500, "Admin service error: " + e.getMessage());
        }
    }

    private void handleListUsers(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        String role = query.get("role");
        String status = query.get("status");
        String search = query.get("search");

        List<User> users = userDao.findAll(role, status, search);
        List<Map<String, Object>> resp = new ArrayList<>();
        for (User u : users) {
            resp.add(u.toMap(false));
        }
        HttpUtil.sendSuccessResponse(exchange, "Users retrieved successfully.", resp);
    }

    private void handleCreateUser(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        String email = String.valueOf(body.getOrDefault("email", "")).trim();
        String password = String.valueOf(body.getOrDefault("password", "")).trim();
        String role = String.valueOf(body.getOrDefault("role", "USER")).trim().toUpperCase();
        String status = String.valueOf(body.getOrDefault("status", "ACTIVE")).trim().toUpperCase();
        String bio = String.valueOf(body.getOrDefault("bio", "")).trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "User details missing: name, email, and password are required.");
            return;
        }

        if (userDao.findByEmail(email) != null) {
            HttpUtil.sendErrorResponse(exchange, 409, "User account with email '" + email + "' already exists.");
            return;
        }

        User u = new User(0, name, email, password, role, status, bio);
        long id = userDao.createUser(u);
        u.setId((int) id);

        HttpUtil.sendSuccessResponse(exchange, "User account '" + name + "' created successfully with role " + role + ".", u.toMap(false));
    }

    private void handleUpdateUser(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int id = 0;
        if (body.get("id") instanceof Number) {
            id = ((Number) body.get("id")).intValue();
        } else if (body.get("id") != null) {
            id = Integer.parseInt(body.get("id").toString());
        }

        if (id <= 0) {
            HttpUtil.sendErrorResponse(exchange, 400, "Valid user ID is required for update.");
            return;
        }

        User existing = userDao.findById(id);
        if (existing == null) {
            HttpUtil.sendErrorResponse(exchange, 404, "User not found.");
            return;
        }

        String name = body.get("name") != null ? body.get("name").toString().trim() : existing.getName();
        String email = body.get("email") != null ? body.get("email").toString().trim() : existing.getEmail();
        String role = body.get("role") != null ? body.get("role").toString().trim().toUpperCase() : existing.getRole();
        String status = body.get("status") != null ? body.get("status").toString().trim().toUpperCase() : existing.getStatus();
        String bio = body.get("bio") != null ? body.get("bio").toString().trim() : existing.getBio();
        String password = body.get("password") != null ? body.get("password").toString().trim() : "";

        existing.setName(name);
        existing.setEmail(email);
        existing.setRole(role);
        existing.setStatus(status);
        existing.setBio(bio);
        if (!password.isEmpty()) {
            existing.setPassword(password);
        }

        boolean updated = userDao.updateUser(existing);
        if (updated) {
            HttpUtil.sendSuccessResponse(exchange, "User '" + name + "' updated successfully.", existing.toMap(false));
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to update user account.");
        }
    }

    private void handleDeleteUser(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        String idStr = query.get("id");
        if (idStr == null) {
            Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
            if (body.get("id") != null) idStr = body.get("id").toString();
        }

        if (idStr == null || idStr.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "Missing user ID to delete.");
            return;
        }

        int id = Integer.parseInt(idStr);
        User existing = userDao.findById(id);
        if (existing == null) {
            HttpUtil.sendErrorResponse(exchange, 404, "User account not found.");
            return;
        }

        if ("ADMIN".equals(existing.getRole()) && id == 1) {
            HttpUtil.sendErrorResponse(exchange, 403, "Primary System Administrator account cannot be deleted.");
            return;
        }

        boolean deleted = userDao.deleteUser(id);
        if (deleted) {
            HttpUtil.sendSuccessResponse(exchange, "User account '" + existing.getName() + "' deleted successfully.", null);
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to delete user account.");
        }
    }

    private void handleToggleUserStatus(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int id = Integer.parseInt(body.get("id").toString());
        String status = body.get("status").toString().toUpperCase();

        boolean ok = userDao.updateStatus(id, status);
        if (ok) {
            HttpUtil.sendSuccessResponse(exchange, "User status updated to " + status + ".", null);
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to update status.");
        }
    }

    private void handleListContent(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        String status = query.get("status");
        String category = query.get("category");
        String search = query.get("search");

        List<WorkoutPlan> plans = planDao.findAll(status, null, category, search);
        List<Map<String, Object>> resp = new ArrayList<>();
        for (WorkoutPlan p : plans) {
            resp.add(p.toMap());
        }
        HttpUtil.sendSuccessResponse(exchange, "Content retrieved for moderation.", resp);
    }

    private void handleModerateContent(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int planId = Integer.parseInt(body.get("planId").toString());
        String status = body.get("status").toString().toUpperCase();
        String notes = body.get("moderationNotes") != null ? body.get("moderationNotes").toString() : "";

        if (!"APPROVED".equals(status) && !"REJECTED".equals(status) && !"PENDING".equals(status)) {
            HttpUtil.sendErrorResponse(exchange, 400, "Invalid moderation status. Must be APPROVED or REJECTED.");
            return;
        }

        boolean ok = planDao.updateApprovalStatus(planId, status, notes);
        if (ok) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("planId", planId);
            data.put("approvalStatus", status);
            data.put("moderationNotes", notes);
            HttpUtil.sendSuccessResponse(exchange, "Content moderation status updated to: " + status + ".", data);
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to update content approval status.");
        }
    }

    private void handleGetSettings(HttpExchange exchange) throws IOException {
        List<SystemSetting> settings = settingsDao.getAllSettings();
        List<Map<String, Object>> resp = new ArrayList<>();
        for (SystemSetting s : settings) {
            resp.add(s.toMap());
        }
        HttpUtil.sendSuccessResponse(exchange, "System settings loaded.", resp);
    }

    private void handleUpdateSettings(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        settingsDao.updateBatch(body);
        HttpUtil.sendSuccessResponse(exchange, "Confirmation: System settings have been updated successfully.", body);
    }

    private void handleOverviewStats(HttpExchange exchange) throws IOException {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("user_roles", userDao.getRoleCounts());
        overview.put("plan_status", planDao.getPlanStatusCounts());
        overview.put("workout_stats", logDao.getGlobalWorkoutStats());
        HttpUtil.sendSuccessResponse(exchange, "Overview statistics loaded.", overview);
    }

    private void handleListFeedback(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        String status = query.get("status");

        List<Feedback> list = feedbackDao.findAll(status);
        List<Map<String, Object>> resp = new ArrayList<>();
        for (Feedback f : list) {
            resp.add(f.toMap());
        }
        HttpUtil.sendSuccessResponse(exchange, "User feedback retrieved.", resp);
    }

    private void handleUpdateFeedbackStatus(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int id = Integer.parseInt(body.get("id").toString());
        String status = body.get("status").toString().toUpperCase();

        boolean ok = feedbackDao.updateStatus(id, status);
        if (ok) {
            HttpUtil.sendSuccessResponse(exchange, "Feedback marked as " + status + ".", null);
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to update feedback status.");
        }
    }
}
