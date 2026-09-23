FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew .
COPY gradlew.bat .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jdk
WORKDIR /app

RUN adduser --system --group spring
COPY --from=build /workspace/build/libs/finance-*.jar /app/app.jar

EXPOSE 8080
USER spring

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
