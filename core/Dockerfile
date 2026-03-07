# base image with Java 23
FROM eclipse-temurin:23-jdk

# Working directory
WORKDIR /app

# Copy the core JAR
COPY target/core-1.0-SNAPSHOT.jar ./core.jar

CMD ["java", "-jar", "core.jar"]