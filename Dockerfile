FROM maven:3.9.15-eclipse-temurin-26 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system bookshop \
    && useradd --system --gid bookshop --home-dir /app bookshop \
    && mkdir -p /app/upload/images \
    && chown -R bookshop:bookshop /app

COPY --from=build --chown=bookshop:bookshop /workspace/target/CT07-1.0.jar /app/bookshop.jar

USER bookshop

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/urandom"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/bookshop.jar"]
