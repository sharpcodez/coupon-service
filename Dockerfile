# ---- build ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run in CI / locally via ./mvnw verify; the image build must be reproducible and fast.
RUN mvn -B -q package -DskipTests

# ---- extract Boot layers (better Docker layer caching: dependencies change rarely) ----
FROM eclipse-temurin:25-jre-alpine AS extract
WORKDIR /extract
COPY --from=build /build/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- runtime ----
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /application
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
