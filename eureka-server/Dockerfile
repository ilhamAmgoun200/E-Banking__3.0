# Étape 1 : Build (facultatif si tu build avec Maven sur ton Mac avant)
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# On suppose que tu as lancé 'mvn clean package -DskipTests' avant
COPY target/*.jar app.jar

# On limite la mémoire vive pour que Render ne tue pas le processus
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseSerialGC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]