FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY src ./src
COPY lib ./lib
COPY sql ./sql
COPY web ./web

RUN mkdir -p bin && javac -cp "lib/*" -d bin $(find src -name "*.java")

CMD ["java", "-cp", "bin:lib/*", "com.fitness.Main"]
