FROM gradle:9.4.0-jdk21 AS build

ARG LAUNCHER_TYPE

WORKDIR /workspace

COPY . .

RUN --mount=type=cache,id=hexo-gradle,target=/home/gradle/.gradle,sharing=locked \
    ./gradlew --no-daemon --parallel :launcher:${LAUNCHER_TYPE}:shadowJar \
    && cp launcher/${LAUNCHER_TYPE}/build/libs/*-all.jar /launcher.jar


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /launcher.jar /app/launcher.jar

ENTRYPOINT ["java", "-jar", "/app/launcher.jar"]
