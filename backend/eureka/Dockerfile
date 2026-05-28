FROM openjdk:17-ea-17-slim
COPY   ./app/build/libs/*-SNAPSHOT.jar   /eureka.jar
EXPOSE 9090
CMD ["java", "-jar", "/eureka.jar"]
