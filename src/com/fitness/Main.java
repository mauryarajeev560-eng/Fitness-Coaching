package com.fitness;

import com.fitness.config.AppConfig;
import com.fitness.controller.*;
import com.fitness.db.DatabaseManager;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("    ONLINE FITNESS COACHING PLATFORM (JAVA & SQL)");
        System.out.println("=================================================");

        // 1. Initialize Database
        System.out.println("[Bootstrap] Initializing SQL Database...");
        DatabaseManager.getInstance();

        // 2. Start HTTP Server
        try {
            int port = AppConfig.PORT;
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

            // Register Handlers
            server.createContext("/api/auth", new AuthController());
            server.createContext("/api/admin", new AdminController());
            server.createContext("/api/coach", new CoachController());
            server.createContext("/api/user", new UserController());
            server.createContext("/", new StaticFileController());

            server.setExecutor(Executors.newFixedThreadPool(16));
            server.start();

            System.out.println("[Bootstrap] Server running at: http://localhost:" + port);
            System.out.println("  • Admin Login: admin@fitness.com / admin123");
            System.out.println("  • Coach Login: coach.marcus@fitness.com / coach123");
            System.out.println("  • User Login:  john.doe@gmail.com / user123");
            System.out.println("=================================================");
            System.out.println("Ready to serve requests. Press Ctrl+C to terminate.");

            // Keep alive
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[Shutdown] Stopping server...");
                server.stop(1);
                System.out.println("[Shutdown] Server stopped.");
            }));

        } catch (Exception e) {
            System.err.println("[Bootstrap] Fatal error starting server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
