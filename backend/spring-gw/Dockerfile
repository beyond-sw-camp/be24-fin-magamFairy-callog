FROM openjdk:17-ea-17-slim
COPY   ./app/build/libs/*-SNAPSHOT.jar   /gateway.jar
EXPOSE 8080
CMD ["java", "-jar", "/gateway.jar"]
