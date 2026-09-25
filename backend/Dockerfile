# syntax=docker/dockerfile:1

# ---------- Étape 1 : compilation ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# D'abord le POM seul : profite du cache Docker pour les dépendances
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

# Puis les sources
COPY src ./src
RUN mvn -B -DskipTests package

# ---------- Étape 2 : image runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Utilisateur non-root (best practice)
RUN addgroup --system app && adduser --system --ingroup app --home /app app

COPY --from=build /app/target/*.jar /app/app.jar
RUN chown -R app:app /app

USER app
EXPOSE 8080

# La config se fait par variables d'environnement (voir docker-compose.yml)
ENV DB_URL=jdbc:postgresql://localhost:5433/relectures \
    DB_USER=relectures \
    DB_PASSWORD=relectures

ENTRYPOINT ["java", "-jar", "/app/app.jar"]