# ---------- build stage ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY common/pom.xml common/pom.xml
COPY monitoring-service/pom.xml monitoring-service/pom.xml
COPY incident-service/pom.xml incident-service/pom.xml
COPY chatops-service/pom.xml chatops-service/pom.xml
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B -pl monitoring-service -am
COPY common/src common/src
COPY monitoring-service/src monitoring-service/src
RUN ./mvnw package -DskipTests -B -pl monitoring-service -am

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/monitoring-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
