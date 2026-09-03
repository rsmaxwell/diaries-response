FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY diaries-responder ./diaries-responder

RUN mkdir -p diaries-web \
    && chmod +x gradlew \
    && ./gradlew :diaries-responder:shadowJar --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /opt/diaries

COPY --from=build \
    /workspace/diaries-responder/build/libs/diaries-responder-*-fat.jar \
    /opt/diaries/diaries-responder.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/opt/diaries/diaries-responder.jar"]
CMD ["--config", "/config/responder.json"]
