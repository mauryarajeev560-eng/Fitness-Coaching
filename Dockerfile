# ==============================================================================
# Online Fitness Coaching Platform - Multi-stage Docker Build
# Compatible with Render, Railway, Fly.io, and Local Docker environments
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Java Bytecode with OpenJDK 21
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copy repository files into builder context
COPY . .

# Clean stale build artifacts and compile all Java classes
RUN rm -rf bin && mkdir -p bin data lib && \
    find src -name "*.java" > sources.txt && \
    if [ -d "lib" ] && [ -n "$(ls -A lib 2>/dev/null)" ]; then \
        javac -cp "lib/*" -d bin @sources.txt; \
    else \
        javac -d bin @sources.txt; \
    fi && \
    rm -f sources.txt

# ------------------------------------------------------------------------------
# Stage 2: Production Runtime with JRE 21 + SQLite3
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install sqlite3, ca-certificates, and curl
RUN apt-get update && \
    apt-get install -y --no-install-recommends sqlite3 ca-certificates curl && \
    rm -rf /var/lib/apt/lists/*

# Copy compiled classes and platform assets
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/web ./web
COPY --from=builder /app/sql ./sql
COPY --from=builder /app/lib ./lib
COPY --from=builder /app/data ./data

# Ensure data directory exists
RUN mkdir -p data

# Environment configuration (Render automatically injects PORT)
ENV PORT=8080
ENV SQLITE_BIN=/usr/bin/sqlite3

# Expose container port
EXPOSE 8080

# Launch server
CMD ["sh", "-c", "if [ -d 'lib' ] && [ -n \"$(ls -A lib 2>/dev/null)\" ]; then java -cp 'bin:lib/*' com.fitness.Main; else java -cp bin com.fitness.Main; fi"]
