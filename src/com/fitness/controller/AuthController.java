package com.fitness.controller;

import com.fitness.dao.UserDao;
import com.fitness.model.User;
import com.fitness.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Map;

public class AuthController implements HttpHandler {
    private final UserDao userDao = new UserDao();

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
            if ("/api/auth/login".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else if ("/api/auth/register".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleRegister(exchange);
            } else if ("/api/auth/me".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleMe(exchange);
            } else if ("/api/auth/seed".equals(path)) {
                com.fitness.db.DatabaseManager.getInstance().seedIfEmpty();
                HttpUtil.sendSuccessResponse(exchange, "Sample database tables and demo seed data verified.", null);
            } else {
                HttpUtil.sendErrorResponse(exchange, 404, "Endpoint not found: " + path);
            }
        } catch (Exception e) {
            HttpUtil.sendErrorResponse(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        String email = body.get("email") != null ? body.get("email").toString().trim() : "";
        String password = body.get("password") != null ? body.get("password").toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "Email and password are required.");
            return;
        }

        User user = userDao.authenticate(email, password);
        if (user == null) {
            // Check if users table was empty and seed
            com.fitness.db.DatabaseManager.getInstance().seedIfEmpty();
            user = userDao.authenticate(email, password);
        }

        if (user == null) {
            HttpUtil.sendErrorResponse(exchange, 401, "Invalid email or password.");
            return;
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            HttpUtil.sendErrorResponse(exchange, 403, "Account is inactive. Please contact system administrator.");
            return;
        }

        HttpUtil.sendSuccessResponse(exchange, "Login successful. Welcome back, " + user.getName() + "!", user.toMap(false));
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, Object> body = HttpUtil.parseJsonBody(exchange);
        String name = body.get("name") != null ? body.get("name").toString().trim() : "";
        String email = body.get("email") != null ? body.get("email").toString().trim() : "";
        String password = body.get("password") != null ? body.get("password").toString().trim() : "";
        String role = body.get("role") != null ? body.get("role").toString().trim().toUpperCase() : "USER";
        String bio = body.get("bio") != null ? body.get("bio").toString().trim() : "";

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "Name, email, and password are required.");
            return;
        }

        if (!"USER".equals(role) && !"COACH".equals(role)) {
            role = "USER";
        }

        User existing = userDao.findByEmail(email);
        if (existing != null) {
            HttpUtil.sendErrorResponse(exchange, 409, "User with this email already exists.");
            return;
        }

        User newUser = new User(0, name, email, password, role, "ACTIVE", bio);
        long newId = userDao.createUser(newUser);
        newUser.setId((int) newId);

        HttpUtil.sendSuccessResponse(exchange, "Registration successful! You can now log in.", newUser.toMap(false));
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        Map<String, String> query = HttpUtil.parseQueryParams(exchange);
        String userIdStr = query.get("userId");
        if (userIdStr == null || userIdStr.isEmpty()) {
            HttpUtil.sendErrorResponse(exchange, 400, "Missing userId parameter.");
            return;
        }

        try {
            int userId = Integer.parseInt(userIdStr);
            User user = userDao.findById(userId);
            if (user == null) {
                HttpUtil.sendErrorResponse(exchange, 404, "User not found.");
                return;
            }
            HttpUtil.sendSuccessResponse(exchange, "User details retrieved.", user.toMap(false));
        } catch (NumberFormatException e) {
            HttpUtil.sendErrorResponse(exchange, 400, "Invalid userId format.");
        }
    }
}
