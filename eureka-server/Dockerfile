# =========================
# Étape 1 : Build avec Maven
# =========================
FROM maven:3.9.2-eclipse-temurin-17 AS build
WORKDIR /app

# Copier uniquement les fichiers nécessaires pour le build
COPY pom.xml .
COPY src ./src

# Build du projet et création du jar (tests ignorés)
RUN mvn clean package -DskipTests

# =========================
# Étape 2 : Runtime
# =========================
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copier le jar construit depuis l'étape build
COPY --from=build /app/target/*.jar app.jar

# Exposer le port (doit correspondre au port utilisé par Spring Boot)
EXPOSE 8080

# Commande pour démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
