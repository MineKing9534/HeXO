FROM gradle:9.4.0-jdk21 AS build

WORKDIR /workspace
ARG WEB_API_PROXY

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libasound2t64 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libatspi2.0-0 \
        libcairo2 \
        libcups2 \
        libdbus-1-3 \
        libdrm2 \
        libgbm1 \
        libglib2.0-0 \
        libnspr4 \
        libnss3 \
        libpango-1.0-0 \
        libx11-6 \
        libxcb1 \
        libxcomposite1 \
        libxdamage1 \
        libxext6 \
        libxfixes3 \
        libxkbcommon0 \
        libxrandr2 \
    && rm -rf /var/lib/apt/lists/*

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY buildSrc ./buildSrc

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle,sharing=locked \
    ./gradlew --no-daemon :web:kobwebExport -PkobwebExportLayout=STATIC -Pweb.apiProxy=${WEB_API_PROXY}

FROM nginx:1.31.1-alpine

COPY --from=build /workspace/web/.kobweb/site /usr/share/nginx/html
