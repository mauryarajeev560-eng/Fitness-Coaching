FROM openjdk:17-jdk-slim
WORKDIR /app
COPY . .
RUN chmod +x run.sh
CMD ["./run.sh"]
