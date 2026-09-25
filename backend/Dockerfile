# Image du backend : build Maven (wrapper) puis exécution sur un JRE 21.
# Contexte de build = racine du dépôt (docker-compose.yml) : le contrat api/contrat.yaml est embarqué
# pour Swagger UI (#96), sans copie versionnée dans backend/.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app/backend
COPY backend/.mvn .mvn
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY api/contrat.yaml /app/api/contrat.yaml
COPY backend/src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
