# Use OpenJDK 23 runtime
FROM eclipse-temurin:23-jdk-alpine

ENV SERVER_ADDRESS=0.0.0.0

WORKDIR /app

# Copy the fat Spring Boot jar for PVP
COPY target/pvp-1.0-SNAPSHOT-exec.jar pvp.jar

# Different port than battle and PVE
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "pvp.jar"]