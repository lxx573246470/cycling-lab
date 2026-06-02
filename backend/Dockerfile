# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw
RUN chmod +x mvnw || true

# Cache dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -DskipTests dependency:go-offline || \
    (apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/* && mvn -B -DskipTests dependency:go-offline)

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -DskipTests package || \
    mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/cycling-lab-backend.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
