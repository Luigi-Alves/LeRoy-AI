FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests clean package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/leroy-ai-assistant-1.0.0.jar app.jar
ENV PORT=8080
EXPOSE 8080
CMD ["java", "--add-modules=jdk.httpserver", "-jar", "app.jar"]