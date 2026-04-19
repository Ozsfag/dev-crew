# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Копируем только файлы для загрузки зависимостей — слой кешируется пока не изменится build.gradle
COPY ../gradlew settings.gradle build.gradle ./
COPY ../gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon --quiet

# Копируем исходники и собираем jar
COPY ../src ./src
RUN ./gradlew bootJar --no-daemon -x test --quiet

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# curl — healthcheck; git — агенты клонируют репозитории;
# nodejs + npm — Claude Code CLI (заменяет Anthropic API)
RUN apk add --no-cache curl git nodejs npm && \
    npm install -g @anthropic-ai/claude-code --quiet

# Копируем jar из builder-слоя
COPY --from=builder /app/build/libs/*.jar app.jar

# Директория для монтирования проектов
VOLUME /projects

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseContainerSupport", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
