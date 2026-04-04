# CLAUDE.md — Dev Crew

## Что это за проект

Spring Boot 3.5 / Java 21 приложение. Оркестрирует команду ИИ-агентов для разработки программных проектов.
Архитектор (человек) согласовывает планы; агенты выполняют задачи автономно через LangChain4j + Claude API.

**Группа**: `org.blacksoil` | **Порт**: 8081 | **БД**: PostgreSQL + Flyway

---

@ai/docs/architecture.md

@ai/docs/coding.md

@ai/docs/testing.md

@ai/docs/infra.md

---

## Частые команды

```bash
./gradlew build                              # сборка + тесты
./gradlew test -Dspring.profiles.active=tc   # тесты с Testcontainers
./gradlew bootRun                            # локальный запуск
./gradlew spotlessApply                      # форматирование кода
./gradlew spotlessCheck                      # проверка форматирования (CI)

# Docker (все команды запускаются из корня проекта)
docker compose -f docker/docker-compose.yml up -d
docker compose -f docker/docker-compose.yml -f docker/docker-compose.prod.yml up -d
```
