FROM gradle:9.4.0-jdk21 AS build

WORKDIR /workspace

ARG HDS_API_URL
ARG HMD_API_URL

# Kobweb's browser tooling needs these libraries while exporting the static site.
RUN apt-get update \
    && apt-get \
        install -y --no-install-recommends \
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

COPY . .

RUN --mount=type=cache,id=hexo-gradle,target=/home/gradle/.gradle,sharing=locked \
    ./gradlew --no-daemon --parallel \
        :web:kobwebExport \
        -PkobwebExportLayout=STATIC \
        -Pweb.hdsApiUrl=${HDS_API_URL} \
        -Pweb.hmdApiUrl=${HMD_API_URL} \
    && cp -a web/.kobweb/site /site

FROM nginx:1.31.1-alpine

COPY --from=build /site /usr/share/nginx/html
