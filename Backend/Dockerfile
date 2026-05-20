FROM eclipse-temurin:17-jdk-jammy AS build
RUN apt-get update && apt-get install -y maven && apt-get clean
WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests
FROM gcr.io/distroless/java17
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]