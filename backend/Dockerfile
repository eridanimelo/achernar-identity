FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests
FROM eclipse-temurin:25-jre
RUN groupadd -g 10001 identity && useradd -u 10001 -g identity identity && mkdir /keys && chown identity:identity /keys
WORKDIR /app
COPY --from=build /app/target/identity-1.0.0.jar app.jar
USER identity
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
