# ---- build ------------------------------------------------------------------------------------
# The JDK, Maven and the source tree stay in this stage; none of it reaches the image that runs.
#
# The build context is the repository root, not this directory - see docker-compose.yml. Since the
# parent POM arrived, a module cannot be built on its own: it inherits from the root POM, and Maven
# loads every module named in <modules> before pruning the reactor to the one being built. So all
# four POMs are copied, and only this module's sources.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# POMs first, so a source-only change does not re-download the world.
COPY pom.xml .
COPY Library-Management-System-Version-2/pom.xml Library-Management-System-Version-2/
COPY Notification-Service/pom.xml Notification-Service/
COPY Analytics-Service/pom.xml Analytics-Service/
RUN mvn -B -q -pl Library-Management-System-Version-2 -am dependency:go-offline

COPY Library-Management-System-Version-2/src ./Library-Management-System-Version-2/src
# Checkstyle, PMD and the tests run in CI; repeating them here only makes images slow to build.
RUN mvn -B -q -pl Library-Management-System-Version-2 -am clean package \
        -DskipTests -Dcheckstyle.skip=true -Dpmd.skip=true \
    && mv Library-Management-System-Version-2/target/*.jar /build/app.jar

# ---- run --------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# A JRE, not a JDK: no compiler, no jar tool, nothing to build with if someone gets a shell.
RUN addgroup -S library && adduser -S -G library -H -s /sbin/nologin library

WORKDIR /app
COPY --from=build --chown=root:root --chmod=444 /build/app.jar /app/app.jar

# The H2 file lives here. Created now and owned by the app user so that a named volume mounted over
# it inherits that ownership - otherwise the volume arrives root-owned and the process cannot write.
RUN mkdir -p /app/data && chown library:library /app/data
VOLUME ["/app/data"]

# Owned by root and read-only to the account that runs it: the process cannot rewrite its own jar.
USER library

EXPOSE 9092

# Container memory, not the host's, and fail fast rather than swapping.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:9092/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
