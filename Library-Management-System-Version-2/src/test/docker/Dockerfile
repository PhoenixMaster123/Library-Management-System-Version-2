# Use an official OpenJDK runtime as the base image
FROM openjdk:21

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file to the container
COPY target/LibraryMS-0.0.1-SNAPSHOT.jar /app/app.jar

# Expose the port your Spring Boot application runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
