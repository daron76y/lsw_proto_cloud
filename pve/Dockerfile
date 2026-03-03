# Use OpenJDK 23 runtime
FROM eclipse-temurin:23-jdk-alpine

ENV SERVER_ADDRESS=0.0.0.0

WORKDIR /app

# Copy the fat Spring Boot jar for PVE
COPY target/pve-1.0-SNAPSHOT-exec.jar pve.jar

# Use a different port than battle module
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "pve.jar"]