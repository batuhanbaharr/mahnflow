# build the jar inside docker so you do not need maven on your machine.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# download deps first so this layer is cached when only code changes
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# small runtime image with just the jre and the jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/mahnflow-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
