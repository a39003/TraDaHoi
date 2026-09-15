FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar

# Render injects PORT for web services. The command keeps local Docker usage
# working as well by falling back to the application's default port.
EXPOSE 8081
CMD ["sh", "-c", "exec java ${JAVA_OPTS:-} -Dserver.port=${PORT:-8081} -jar /app/app.jar"]
