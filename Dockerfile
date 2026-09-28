# Using a Java 21 image
FROM eclipse-temurin:21-jdk-alpine AS builder

# Creating a work directory inside the container
WORKDIR /app

# Copying the pom.xml to download dependencies
COPY pom.xml . mvnw ./
COPY .mvn .mvn

RUN chmod +x ./mvnw

RUN ./mvnw dependency:go-offline -B

# Copying the source code to the container
COPY src ./src

# Execute the container without test
RUN ./mvnw clean package -DskipTests

# Descarding all and using a clean image only with JRE
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create an user without privileges
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copying the .jar generating the other steps and rename to app.jar
COPY --from=builder /app/target/*.jar app.jar

# Indicating which port exhibits the app outside the container
EXPOSE 8081

# Command that will be executed when turning on the container (con flags de memoria para contenedores)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]