package com.fitness.controller;

import com.fitness.dao.MessageDao;
import com.fitness.dao.WorkoutLogDao;
import com.fitness.dao.WorkoutPlanDao;
import com.fitness.model.PlanExercise;
import com.fitness.model.WorkoutPlan;
import com.fitness.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;

public class CoachController implements HttpHandler {
    private final WorkoutPlanDao planDao = new WorkoutPlanDao();
    private final WorkoutLogDao logDao = new WorkoutLogDao();
    private final MessageDao messageDao = new MessageDao();

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
            // Workout Plan Management
            if (path.equals("/api/coach/plans")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleListPlans(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleCreatePlan(exchange);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    handleUpdatePlan(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    handleDeletePlan(exchange);
                } else {
                    HttpUtil.sendErrorResponse(exchange, 405, "Method not allowed");
                }
            } else if (path.equals("/api/coach/plan/details") && "GET".equalsIgnoreCase(method)) {
                handleGetPlanDetails(exchange);
            }
            // User Interaction
            else if (path.equals("/api/coach/interactions") && "GET".equalsIgnoreCase(method)) {
                handleGetInteractions(exchange);
            } else if (path.equals("/api/coach/messages")) {
                if ("GET".equalsIgnoreCase(method)) {
                    handleGetConversation(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    handleSendMessage(exchange);
                } else {
                    HttpUtil.sendErrorResponse(exchange, 405, "Method not allowed");
                }
            } else if (path.equals("/api/coach/interaction-history") && "GET".equalsIgnoreCase(method)) {
                handleInteractionHistory(exchange);
            }
            // Track User Progress
            else if (path.equals("/api/coach/trainees") && "GET".equalsIgnoreCase(method)) {
                handleGetTrainees(exchange);
            } else if (path.equals("/api/coach/trainee/progress") && "GET".equalsIgnoreCase(method)) {
                handleGetTraineeProgress(exchange);
            }
            // Plan Analytics
            else if (path.equals("/api/coach/analytics") && "GET".equalsIgnoreCase(method)) {
                handleGetAnalytics(exchange);
            } else {
                HttpUtil.sendErrorResponse(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (Exception e) {
            HttpUtil.sendErrorResponse(exchange, 500, "Coach service error: " + e.getMessage());
        }
    }

    private void handleListPlans(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        String coachIdStr = query.get("coachId");
        if (coachIdStr == null || coachIdStr.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "Missing coachId parameter.");
            return;
        }

        int coachId = Integer.parseInt(coachIdStr);
        String category = query.get("category");
        String search = query.get("search");

        List<WorkoutPlan> plans = planDao.findAll(null, coachId, category, search);
        List<Map<String, Object>> resp = new ArrayList<>();
        for (WorkoutPlan p : plans) {
            resp.add(p.toMap());
        }
        HttpUtil.sendSuccessResponse(exchange, "Coach workout plans retrieved successfully.", resp);
    }

    private void handleGetPlanDetails(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        String idStr = query.get("id");
        if (idStr == null) {
            HttpUtil.sendErrorResponse(exchange, 400, "Missing plan id.");
            return;
        }

        WorkoutPlan plan = planDao.findById(Integer.parseInt(idStr));
        if (plan == null) {
            HttpUtil.sendErrorResponse(exchange, 404, "Workout plan not found.");
            return;
        }
        HttpUtil.sendSuccessResponse(exchange, "Workout plan details retrieved.", plan.toMap());
    }

    @SuppressWarnings("unchecked")
    private void handleCreatePlan(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int coachId = Integer.parseInt(body.get("coachId").toString());
        String title = String.valueOf(body.getOrDefault("title", "")).trim();
        String description = String.valueOf(body.getOrDefault("description", "")).trim();
        String category = String.valueOf(body.getOrDefault("category", "General Fitness")).trim();
        String difficulty = String.valueOf(body.getOrDefault("difficulty", "BEGINNER")).trim().toUpperCase();
        int durationWeeks = Integer.parseInt(body.getOrDefault("durationWeeks", 4).toString());

        if (title.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "Workout plan title is required.");
            return;
        }

        WorkoutPlan plan = new WorkoutPlan();
        plan.setCoachId(coachId);
        plan.setTitle(title);
        plan.setDescription(description);
        plan.setCategory(category);
        plan.setDifficulty(difficulty);
        plan.setDurationWeeks(durationWeeks);
        plan.setApprovalStatus("PENDING"); // Subject to Admin moderation
        plan.setModerationNotes("Pending admin approval before trainees can enroll.");

        // Parse exercises if provided
        List<PlanExercise> exercises = new ArrayList<>();
        if (body.get("exercises") instanceof List) {
            List<Object> rawList = (List<Object>) body.get("exercises");
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) obj;
                    PlanExercise ex = new PlanExercise();
                    ex.setDayOfWeek(String.valueOf(m.getOrDefault("dayOfWeek", "Day 1")));
                    ex.setExerciseName(String.valueOf(m.getOrDefault("exerciseName", "Exercise")));
                    ex.setSets(Integer.parseInt(m.getOrDefault("sets", 3).toString()));
                    ex.setReps(Integer.parseInt(m.getOrDefault("reps", 12).toString()));
                    ex.setDurationMins(Integer.parseInt(m.getOrDefault("durationMins", 0).toString()));
                    ex.setRestSeconds(Integer.parseInt(m.getOrDefault("restSeconds", 60).toString()));
                    ex.setNotes(String.valueOf(m.getOrDefault("notes", "")));
                    exercises.add(ex);
                }
            }
        }
        plan.setExercises(exercises);

        long planId = planDao.create(plan);
        plan.setId((int) planId);

        HttpUtil.sendSuccessResponse(exchange,
                "Confirmation: Workout plan '" + title + "' created successfully and submitted for moderation.",
                plan.toMap());
    }

    @SuppressWarnings("unchecked")
    private void handleUpdatePlan(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        int id = Integer.parseInt(body.get("id").toString());
        int coachId = Integer.parseInt(body.getOrDefault("coachId", 0).toString());
        String title = String.valueOf(body.getOrDefault("title", "")).trim();
        String description = String.valueOf(body.getOrDefault("description", "")).trim();
        String category = String.valueOf(body.getOrDefault("category", "General Fitness")).trim();
        String difficulty = String.valueOf(body.getOrDefault("difficulty", "BEGINNER")).trim().toUpperCase();
        int durationWeeks = Integer.parseInt(body.getOrDefault("durationWeeks", 4).toString());

        WorkoutPlan plan = new WorkoutPlan();
        plan.setId(id);
        plan.setCoachId(coachId);
        plan.setTitle(title);
        plan.setDescription(description);
        plan.setCategory(category);
        plan.setDifficulty(difficulty);
        plan.setDurationWeeks(durationWeeks);

        if (body.get("exercises") instanceof List) {
            List<PlanExercise> exercises = new ArrayList<>();
            List<Object> rawList = (List<Object>) body.get("exercises");
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    Map<String, Object> m = (Map<String, Object>) obj;
                    PlanExercise ex = new PlanExercise();
                    ex.setPlanId(id);
                    ex.setDayOfWeek(String.valueOf(m.getOrDefault("dayOfWeek", "Day 1")));
                    ex.setExerciseName(String.valueOf(m.getOrDefault("exerciseName", "Exercise")));
                    ex.setSets(Integer.parseInt(m.getOrDefault("sets", 3).toString()));
                    ex.setReps(Integer.parseInt(m.getOrDefault("reps", 12).toString()));
                    ex.setDurationMins(Integer.parseInt(m.getOrDefault("durationMins", 0).toString()));
                    ex.setRestSeconds(Integer.parseInt(m.getOrDefault("restSeconds", 60).toString()));
                    ex.setNotes(String.valueOf(m.getOrDefault("notes", "")));
                    exercises.add(ex);
                }
            }
            plan.setExercises(exercises);
        }

        boolean ok = planDao.update(plan);
        if (ok) {
            HttpUtil.sendSuccessResponse(exchange, "Confirmation: Workout plan '" + title + "' updated successfully.", plan.toMap());
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to update workout plan.");
        }
    }

    private void handleDeletePlan(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int id = Integer.parseInt(query.get("id"));
        boolean ok = planDao.delete(id);
        if (ok) {
            HttpUtil.sendSuccessResponse(exchange, "Confirmation: Workout plan deleted successfully.", null);
        } else {
            HttpUtil.sendErrorResponse(exchange, 500, "Failed to delete workout plan.");
        }
    }

    private void handleGetInteractions(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int coachId = Integer.parseInt(query.get("coachId"));
        List<Map<String, Object>> interactions = messageDao.getRecentInteractions(coachId);
        HttpUtil.sendSuccessResponse(exchange, "Recent interactions retrieved.", interactions);
    }

    private void handleGetConversation(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int coachId = Integer.parseInt(query.get("coachId"));
        int userId = Integer.parseInt(query.get("userId"));

        List<?> conversation = messageDao.getConversation(coachId, userId);
        HttpUtil.sendSuccessResponse(exchange, "Conversation loaded.", conversation);
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

        HttpUtil.sendSuccessResponse(exchange, "Confirmation: Message sent successfully to trainee.", data);
    }

    private void handleInteractionHistory(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int coachId = Integer.parseInt(query.get("coachId"));
        List<Map<String, Object>> history = messageDao.getCoachInteractionHistory(coachId);
        HttpUtil.sendSuccessResponse(exchange, "Coach interaction history loaded.", history);
    }

    private void handleGetTrainees(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int coachId = Integer.parseInt(query.get("coachId"));
        List<Map<String, Object>> trainees = logDao.getCoachTraineesProgress(coachId);
        HttpUtil.sendSuccessResponse(exchange, "Trainees progress loaded.", trainees);
    }

    private void handleGetTraineeProgress(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int traineeId = Integer.parseInt(query.get("traineeId"));
        Map<String, Object> progress = logDao.getUserProgressMetrics(traineeId);
        progress.put("logs", logDao.getUserLogs(traineeId));
        HttpUtil.sendSuccessResponse(exchange, "Progress report for trainee loaded.", progress);
    }

    private void handleGetAnalytics(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        int coachId = Integer.parseInt(query.get("coachId"));
        List<Map<String, Object>> analytics = planDao.getCoachPlanAnalytics(coachId);
        HttpUtil.sendSuccessResponse(exchange, "Plan analytics and user engagement data loaded.", analytics);
    }
}
