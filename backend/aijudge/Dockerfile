FROM openjdk:17-ea-17-slim
COPY   ./build/libs/*-SNAPSHOT.jar   /aijudge.jar
EXPOSE 8081
CMD ["java", "-jar", "/aijudge.jar"]
