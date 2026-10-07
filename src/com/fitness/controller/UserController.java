package com.fitness.controller;

import com.fitness.dao.*;
import com.fitness.model.Feedback;
import com.fitness.model.User;
import com.fitness.model.UserProfile;
import com.fitness.model.WorkoutLog;
import com.fitness.model.WorkoutPlan;
import com.fitness.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

public class UserController implements HttpHandler {
    private final WorkoutPlanDao planDao = new WorkoutPlanDao();
    private final WorkoutLogDao logDao = new WorkoutLogDao();
    private final MessageDao messageDao = new MessageDao();
    private final UserDao userDao = new UserDao();
    private final FeedbackDao feedbackDao = new FeedbackDao();

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
            // Workout Plan Access
            if (path.equals("/api/user/plans") && "GET".equalsIgnoreCase(method)) {
                handleBrowsePlans(exchange);
            } else if (path.equals("/api/user/plans/active") && "GET".equalsIgnoreCase(method)) {
                handleGetActivePlans(exchange);
            } else if (path.equals("/api/user/plans/enroll") && "POST".equalsIgnoreCase(method)) {
                handleEnrollPlan(exchange);
            } else if (path.equals("/api/user/plans/unenroll") && "POST".equalsIgnoreCase(method)) {
                handleUnenrollPlan(exchange);
            } else if (path.equals("/api/user/plan/details") && "GET".equalsIgnoreCase(method)) {
                handlePlanDetails(exchange);
            }
            // Fitness Progress Tracker
            else if (path.equals("/api/user/progress/log")) {
                if ("POST".equalsIgnoreCase(method)) {
                    handleLogWorkout(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeleteLog(exchange);
                } else {
                    HttpUtil.sendErrorResponse(exchange, 405, "Method not allowed");
                }
            } else if (path.equals("/api/user/progress/logs") && "GET".equalsIgnoreCase(method)) {
                handleGetWorkoutLogs(exchange);
            } else if (path.equals("/api/user/progress/metrics") && "GET".equalsIgnoreCase(method)) {
                handleGetProgressMetrics(exchange);
            } else if (path.equals("/api/user/achievements") && "GET".equalsIgnoreCase(method)) {
                handleGetAchievements(exchange);
            }
            // Coach Interaction
            else if (path.equals("/api/user/coaches") && "GET".equalsIgnoreCase(method)) {
                handleListCoaches(exchange);
            } else if (path.equals("/api/user/messages")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetConversation(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleSendMessage(exchange);
                } else {
                    HttpUtil.sendErrorResponse(exchange, 405, "Method not allowed");
                }
            }
            // Profile Management
            else if (path.equals("/api/user/profile")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetProfile(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleUpdateProfile(exchange);
                } else {
                    HttpUtil.sendErrorResponse(exchange, 405, "Method not allowed");
                }
            }
            // Feedback Submission
            else if (path.equals("/api/user/feedback") && "POST".equalsIgnoreCase(method)) {
                handleSubmitFeedback(exchange);
            } else {
                HttpUtil.sendErrorResponse(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (Exception e) {
            HttpUtil.sendErrorResponse(exchange, 500, "User service error: " + e.getMessage());
        }
    }

    private void handleBrowsePlans(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        String category = query.get("category");
        String search = query.get("search");

        // Trainees only see APPROVED plans
        List<WorkoutPlan> plans = planDao.findAll("APPROVED", null, category, search);
        List<Map<String, Object>> resp = new ArrayList<>();
        for (WorkoutPlan p : plans) {
            resp.add(p.toMap());
        }
        HttpUtil.sendSuccessResponse(exchange, "Available workout plans retrieved.", resp);
    }

    private void handleGetActivePlans(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int userId = Integer.parseInt(query.get("userId"));
        List<WorkoutPlan> activePlans = planDao.getUserEnrolledPlans(userId);
        List<Map<String, Object>> resp = new ArrayList<>();
        for (WorkoutPlan p : activePlans) {
            resp.add(p.toMap());
        }
        HttpUtil.sendSuccessResponse(exchange, "Enrolled workout plans retrieved.", resp);
    }

    private void handleEnrollPlan(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int userId = Integer.parseInt(body.get("userId").toString());
        int planId = Integer.parseInt(body.get("planId").toString());

        WorkoutPlan plan = planDao.findById(planId);
        if (plan == null) {
            HttpUtil.sendErrorResponse(exchange, 404, "Workout plan not found.");
            return;
        }

        boolean ok = planDao.enrollUser(userId, planId);
        if (ok) {
            HttpUtil.sendSuccessResponse(exchange, "Confirmation: Successfully enrolled in plan '" + plan.getTitle() + "'. Let's crush your goals!", plan.toMap());
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to enroll in workout plan.");
        }
    }

    private void handleUnenrollPlan(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int userId = Integer.parseInt(body.get("userId").toString());
        int planId = Integer.parseInt(body.get("planId").toString());

        boolean ok = planDao.unenrollUser(userId, planId);
        if (ok) {
            HttpUtil.sendSuccessResponse(exchange, "Confirmation: Plan enrollment updated to inactive.", null);
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to update enrollment.");
        }
    }

    private void handlePlanDetails(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int planId = Integer.parseInt(query.get("id"));
        WorkoutPlan plan = planDao.findById(planId);
        if (plan == null) {
            HttpUtil.sendErrorResponse(exchange, 404, "Workout plan not found.");
            return;
        }
        HttpUtil.sendSuccessResponse(exchange, "Workout plan details retrieved.", plan.toMap());
    }

    private void handleLogWorkout(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int userId = Integer.parseInt(body.get("userId").toString());
        Integer planId = body.get("planId") != null && !body.get("planId").toString().isEmpty()
                ? Integer.parseInt(body.get("planId").toString()) : null;
        String logDate = String.valueOf(body.getOrDefault("logDate", ""));
        if (logDate.isEmpty()) {
            logDate = java.time.LocalDate.now().toString();
        }
        int durationMinutes = Integer.parseInt(body.getOrDefault("durationMinutes", 45).toString());
        int caloriesBurned = Integer.parseInt(body.getOrDefault("caloriesBurned", 300).toString());
        double weight = Double.parseDouble(body.getOrDefault("weight", 0.0).toString());
        String notes = String.valueOf(body.getOrDefault("notes", ""));

        WorkoutLog log = new WorkoutLog();
        log.setUserId(userId);
        log.setPlanId(planId);
        log.setLogDate(logDate);
        log.setDurationMinutes(durationMinutes);
        log.setCaloriesBurned(caloriesBurned);
        log.setWeight(weight);
        log.setNotes(notes);

        long id = logDao.createLog(log);
        log.setId((int) id);

        HttpUtil.sendSuccessResponse(exchange,
                "Confirmation: Workout session logged successfully! " + caloriesBurned + " kcal burned.",
                log.toMap());
    }

    private void handleDeleteLog(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int logId = Integer.parseInt(query.get("id"));
        int userId = Integer.parseInt(query.get("userId"));

        boolean ok = logDao.deleteLog(logId, userId);
        if (ok) {
            HttpUtil.sendSuccessResponse(exchange, "Confirmation: Workout log entry removed.", null);
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to remove workout log.");
        }
    }

    private void handleGetWorkoutLogs(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int userId = Integer.parseInt(query.get("userId"));
        List<WorkoutLog> logs = logDao.getUserLogs(userId);
        List<Map<String, Object>> resp = new ArrayList<>();
        for (WorkoutLog l : logs) {
            resp.add(l.toMap());
        }
        HttpUtil.sendSuccessResponse(exchange, "Workout history retrieved.", resp);
    }

    private void handleGetProgressMetrics(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int userId = Integer.parseInt(query.get("userId"));
        Map<String, Object> metrics = logDao.getUserProgressMetrics(userId);
        HttpUtil.sendSuccessResponse(exchange, "Fitness progress data loaded.", metrics);
    }

    private void handleGetAchievements(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int userId = Integer.parseInt(query.get("userId"));

        List<WorkoutLog> logs = logDao.getUserLogs(userId);
        int count = logs.size();
        int totalCalories = 0;
        int totalMinutes = 0;
        for (WorkoutLog l : logs) {
            totalCalories += l.getCaloriesBurned();
            totalMinutes += l.getDurationMinutes();
        }

        List<Map<String, Object>> badges = new ArrayList<>();

        badges.add(createBadge("First Step", "Logged first workout session", "badge-check", count >= 1, "Bronze"));
        badges.add(createBadge("Consistency Starter", "Completed 5 workout sessions", "award", count >= 5, "Silver"));
        badges.add(createBadge("Fitness Dedicated", "Completed 10 workout sessions", "shield", count >= 10, "Gold"));
        badges.add(createBadge("Calorie Crusher", "Burned 3,000+ total calories", "zap", totalCalories >= 3000, "Flame"));
        badges.add(createBadge("Century Hour", "Trained for over 500 total minutes", "clock", totalMinutes >= 500, "Titanium"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("badges", badges);
        data.put("total_workouts", count);
        data.put("total_calories", totalCalories);
        data.put("total_minutes", totalMinutes);

        HttpUtil.sendSuccessResponse(exchange, "Achievements and badges loaded.", data);
    }

    private Map<String, Object> createBadge(String title, String desc, String icon, boolean earned, String tier) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("title", title);
        b.put("description", desc);
        b.put("icon", icon);
        b.put("earned", earned);
        b.put("tier", tier);
        return b;
    }

    private void handleListCoaches(HttpExchange exchange) throws IOException {
        List<User> coaches = userDao.findAll("COACH", "ACTIVE", null);
        List<Map<String, Object>> resp = new ArrayList<>();
        for (User c : coaches) {
            resp.add(c.toMap(false));
        }
        HttpUtil.sendSuccessResponse(exchange, "Active coaches retrieved.", resp);
    }

    private void handleGetConversation(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int userId = Integer.parseInt(query.get("userId"));
        int coachId = Integer.parseInt(query.get("coachId"));

        List<?> list = messageDao.getConversation(userId, coachId);
        HttpUtil.sendSuccessResponse(exchange, "Messages loaded.", list);
    }

    private void handleSendMessage(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int senderId = Integer.parseInt(body.get("senderId").toString());
        int receiverId = Integer.parseInt(body.get("receiverId").toString());
        String text = String.valueOf(body.getOrDefault("messageText", "")).trim();

        if (text.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "Message text cannot be empty.");
            return;
        }

        long msgId = messageDao.sendMessage(senderId, receiverId, text);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", msgId);
        data.put("senderId", senderId);
        data.put("receiverId", receiverId);
        data.put("messageText", text);

        HttpUtil.sendSuccessResponse(exchange, "Confirmation: Message sent successfully to your coach.", data);
    }

    private void handleGetProfile(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int userId = Integer.parseInt(query.get("userId"));

        User user = userDao.findById(userId);
        if (user == null) {
            HttpUtil.sendErrorResponse(exchange, 404, "User not found.");
            return;
        }

        UserProfile profile = userDao.getUserProfile(userId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("user", user.toMap(false));
        resp.put("profile", profile != null ? profile.toMap() : null);

        HttpUtil.sendSuccessResponse(exchange, "Profile loaded.", resp);
    }

    private void handleUpdateProfile(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int userId = Integer.parseInt(body.get("userId").toString());

        User user = userDao.findById(userId);
        if (user == null) {
            HttpUtil.sendErrorResponse(exchange, 404, "User not found.");
            return;
        }

        if (body.get("name") != null) user.setName(body.get("name").toString());
        if (body.get("bio") != null) user.setBio(body.get("bio").toString());
        if (body.get("password") != null && !body.get("password").toString().isEmpty()) {
            user.setPassword(body.get("password").toString());
        }
        userDao.updateUser(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setAge(Integer.parseInt(body.getOrDefault("age", 25).toString()));
        profile.setGender(String.valueOf(body.getOrDefault("gender", "Not Specified")));
        profile.setHeight(Double.parseDouble(body.getOrDefault("height", 170.0).toString()));
        profile.setCurrentWeight(Double.parseDouble(body.getOrDefault("currentWeight", 70.0).toString()));
        profile.setTargetWeight(Double.parseDouble(body.getOrDefault("targetWeight", 68.0).toString()));
        profile.setFitnessGoal(String.valueOf(body.getOrDefault("fitnessGoal", "General Fitness")));

        userDao.saveOrUpdateProfile(profile);

        HttpUtil.sendSuccessResponse(exchange, "Confirmation: Profile and fitness goals updated successfully.", profile.toMap());
    }

    private void handleSubmitFeedback(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int userId = Integer.parseInt(body.get("userId").toString());
        String category = String.valueOf(body.getOrDefault("category", "General"));
        String subject = String.valueOf(body.getOrDefault("subject", ""));
        String message = String.valueOf(body.getOrDefault("message", ""));
        int rating = Integer.parseInt(body.getOrDefault("rating", 5).toString());

        if (subject.isEmpty() || message.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "Subject and feedback message are required.");
            return;
        }

        Feedback f = new Feedback();
        f.setUserId(userId);
        f.setCategory(category);
        f.setSubject(subject);
        f.setMessage(message);
        f.setRating(rating);
        f.setStatus("PENDING");

        long id = feedbackDao.create(f);
        f.setId((int) id);

        HttpUtil.sendSuccessResponse(exchange, "Confirmation: Thank you! Your feedback has been submitted successfully to administration.", f.toMap());
    }
}
