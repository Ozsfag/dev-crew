---
name: new-spring-component
description: Скаффолд типового Spring-компонента dev-crew — @ConfigurationProperties или @Scheduled-задача — по конвенциям проекта. Вызывать при добавлении конфиг-проперти или scheduled-задачи. Экономит перечитывание coding.md.
---

# /new-spring-component — Properties / Scheduled

Канон — [coding.md](../../docs/coding.md).

## @ConfigurationProperties

- В `<context>/app/config/`. Пара классов:
  ```java
  @Data
  @ConfigurationProperties(prefix = "devcrew.<context>")
  public class FooProperties {
    private String model = "claude-sonnet-4-6";   // дефолт прямо в поле
  }

  @Configuration
  @EnableConfigurationProperties(FooProperties.class)
  public class FooConfig {}
  ```
- Все свойства объявить явно в `application.yml`, **даже если равны дефолту**.
- Секреты/модели/токены/флаги — только через эти проперти, **никогда** хардкод в
  `@Service`/`@Component`.
- Тест — через `new FooProperties()` (без Spring-контекста).

## @Scheduled-задача

- Класс в `<context>/bootstrap/` (рядом с `RateLimitRetryScheduler`).
- `@Scheduled(fixedDelayString = "${devcrew....:30000}")` — интервал из проперти,
  не хардкод-число.
- Если задача работает с агентом/задачей — положить `taskId`/контекст в MDC в
  try/finally (как `AgentExecutionService`), иначе теряется корреляция в логах.
- Бизнес-логику вынести в отдельный метод/сервис (тестируемость).
- Время — через `TimeProvider`, не `Instant.now()`.

## Тест + review

- Покрыть **каждую** ветку (100%). Scheduled — тест логики напрямую/через мок-сервис.
- Метрики — `new SimpleMeterRegistry()`, не `@SpringBootTest`.
- Review: `java-reviewer` + `silent-failure-hunter` (если есть `catch{}`/async-путь).
