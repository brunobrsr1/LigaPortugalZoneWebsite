# STAGE 1: Build
#Use maven image and JDK 21 to compile the project
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source font and copile the project
COPY src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Run
# Use JRE image for executing the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/target/liga-portugal-zone-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]