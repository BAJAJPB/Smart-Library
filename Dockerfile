# Stage 1: Build the app using Maven
FROM maven:3.9-eclipse-temurin-17 as build

WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Package the application
RUN mvn clean package -DskipTests

# Stage 2: Run the app
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy the built JAR from the first stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8088

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:mysql://shinkansen.proxy.rlwy.net:20177/railway", "--spring.datasource.username=root", "--spring.datasource.password=OKUyLfqFgbkYJKZMhrJRbjbyxZktzJGk", "--server.port=8088"]
