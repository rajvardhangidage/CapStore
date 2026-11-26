FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp verify

FROM eclipse-temurin:17-jre
WORKDIR /app

ARG VERSION=dev
ARG VCS_REF=unknown
LABEL org.opencontainers.image.title="CapStore backend" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}"

RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid 10001 --no-create-home --shell /usr/sbin/nologin app \
    && mkdir -p /app/uploads \
    && chown -R 10001:10001 /app

COPY --from=build --chown=10001:10001 /workspace/target/*.jar /app/app.jar

USER 10001:10001
EXPOSE 8080 8081
VOLUME ["/app/uploads"]
STOPSIGNAL SIGTERM

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8081/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:InitialRAMPercentage=25.0", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-Djava.io.tmpdir=/tmp", "-jar", "/app/app.jar"]
