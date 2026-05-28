FROM openjdk:17-ea-17-slim
COPY   ./app/build/libs/*-SNAPSHOT.jar   /evaluation.jar
EXPOSE 8082
CMD ["java", "-jar", "/evaluation.jar"]
