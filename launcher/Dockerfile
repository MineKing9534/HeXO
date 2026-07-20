FROM gradle:9.4.0-jdk21 AS build

WORKDIR /workspace
COPY . .

RUN --mount=type=cache,id=hexo-gradle,target=/home/gradle/.gradle,sharing=locked \
    ./gradlew --no-daemon --parallel \
        :launcher:shadowJar


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/launcher/build/libs/launcher-*-all.jar /app/launcher.jar

ENTRYPOINT ["java", "-jar", "/app/launcher.jar"]
