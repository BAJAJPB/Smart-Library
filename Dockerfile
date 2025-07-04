FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy the JAR file from the target directory
COPY target/*.jar app.jar

# Expose the application port
EXPOSE 8088

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.datasource.url=jdbc:mysql://shinkansen.proxy.rlwy.net:20177/railway", "--spring.datasource.username=root", "--spring.datasource.password=OKUyLfqFgbkYJKZMhrJRbjbyxZktzJGk", "--server.port=8088"]
