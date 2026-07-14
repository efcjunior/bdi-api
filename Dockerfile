FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/bdi-api-*.jar

COPY ${JAR_FILE} /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
