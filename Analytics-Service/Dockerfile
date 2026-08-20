# ---- build ------------------------------------------------------------------------------------
# The JDK, Maven and the source tree stay in this stage; none of it reaches the image that runs.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dependencies first, so a source-only change does not re-download the world.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Checkstyle, PMD and the tests run in CI; repeating them here only makes images slow to build.
RUN mvn -B -q clean package -DskipTests -Dcheckstyle.skip=true -Dpmd.skip=true \
    && mv target/*.jar /build/app.jar

# ---- run --------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# A JRE, not a JDK: no compiler, no jar tool, nothing to build with if someone gets a shell.
RUN addgroup -S library && adduser -S -G library -H -s /sbin/nologin library

WORKDIR /app
COPY --from=build --chown=root:root --chmod=444 /build/app.jar /app/app.jar

# Owned by root and read-only to the account that runs it: the process cannot rewrite its own jar.
USER library

EXPOSE 9095

# Container memory, not the host's, and fail fast rather than swapping.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:9095/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
