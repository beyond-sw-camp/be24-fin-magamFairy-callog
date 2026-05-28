FROM openjdk:17-ea-17-slim
COPY   ./app/build/libs/*-SNAPSHOT.jar   /app.jar
EXPOSE 8083
CMD ["java", "-jar", "/app.jar"]
