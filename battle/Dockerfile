# Use OpenJdk 23 runtime
FROM eclipse-temurin:23-jdk-alpine

# Make Spring Boot listen on all addresses
ENV SERVER_ADDRESS=0.0.0.0

# Set working directory
WORKDIR /app

# Copy the fat Spring Boot jar into the container
COPY target/battle-1.0-SNAPSHOT-exec.jar battle.jar

# Expose the port Spring Boot listens on
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "battle.jar"]