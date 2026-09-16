FROM gradle:9.4.0-jdk21 AS build

WORKDIR /workspace

COPY . .

RUN --mount=type=cache,id=hexo-gradle-launcher,target=/home/gradle/.gradle,sharing=locked \
    ./gradlew --no-daemon --parallel \
        :launcher:api:shadowJar \
        :launcher:discord:shadowJar


FROM eclipse-temurin:21-jre-alpine AS api

WORKDIR /app
COPY --from=build /workspace/launcher/api/build/libs/*-all.jar /app/launcher.jar

ENTRYPOINT ["java", "-jar", "/app/launcher.jar"]


FROM eclipse-temurin:21-jre-alpine AS discord

WORKDIR /app
COPY --from=build /workspace/launcher/discord/build/libs/*-all.jar /app/launcher.jar

ENTRYPOINT ["java", "-jar", "/app/launcher.jar"]
