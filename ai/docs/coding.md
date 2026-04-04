## Конфигурация: ConfigurationProperties

```java
// app/config/AgentProperties.java
@Data
@ConfigurationProperties(prefix = "devcrew.agent")
public class AgentProperties {
    private String model = "claude-sonnet-4-6";
    private int maxTokens = 8096;
    private int maxIterations = 20;
}

// app/config/AgentConfig.java
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentConfig {
}
```

**application.yml** — все свойства объявлены явно, даже если равны дефолту:

```yaml
devcrew:
  agent:
    model: claude-sonnet-4-6
    max-tokens: 8096
    max-iterations: 20
  notification:
    telegram:
      enabled: false
      bot-token: ${TELEGRAM_BOT_TOKEN:}
      chat-id: ${TELEGRAM_CHAT_ID:}
```

**Запрещено**: хардкодить модели, токены и флаги прямо в `@Service` / `@Component`.

---

## LangChain4j паттерны

### AiService — декларативный агент

```java
// domain-порт агента
public interface BackendDevAgent {
    String execute(String task);
}

// bootstrap — регистрация через LangChain4j
@Bean
public BackendDevAgent backendDevAgent(ChatLanguageModel model) {
    return AiServices.builder(BackendDevAgent.class)
            .chatLanguageModel(model)
            .tools(fileTools, gradleTools, gitTools)
            .build();
}
```

### Tools — инструменты агента

```java
// adapter/out/tool/
public class GradleTools {
    @Tool("Run Gradle tests and return output")
    public String runTests(String projectPath) { ... }

    @Tool("Build the Gradle project")
    public String buildProject(String projectPath) { ... }
}
```

### PostAgentHook — расширение без модификации

```java
// Agent-модуль объявляет интерфейс:
public interface PostAgentHook {
    void onAgentCompleted(UUID taskId, AgentRole role, String result);
}
// Notification реализует. Agent не знает о Notification.
```

### Маппинг в три слоя

```
domain Model ←→ PersistenceMapper ←→ Entity
domain Model ←→ AppMapper         ←→ AppDto
AppDto       ←→ WebMapper         ←→ Request/Response
```

### Транзакции

- `@Transactional` — только на `app/service/**` и `adapter/out/.../store/**`
- `@Transactional(readOnly = true)` — на query-сервисах
- В контроллерах — **никогда**

### Тестируемость времени

Используй `TimeProvider` вместо `Instant.now()` / `LocalDate.now()` напрямую.

---

## Принципы KISS / DRY / YAGNI

### KISS — Keep It Simple

- Простейшее решение, которое работает — предпочтительнее «умного»
- Метод длиннее ~20 строк — сигнал, что он делает слишком много; разбить
- Не обобщать до абстракции, пока нет второго конкретного случая

### DRY — Don't Repeat Yourself

- Повторяющаяся бизнес-логика → общий метод или сервис
- Повторяющаяся конфигурация → `*Properties`
- **Исключение**: дублирование в тестах допустимо — читаемость теста важнее DRY

### YAGNI — You Ain't Gonna Need It

- Реализовывать только то, что нужно прямо сейчас
- Не добавлять поля, флаги, параметры «на будущее» без конкретной задачи в backlog
- Не проектировать под гипотетические сценарии

**Запрещено**:
- Добавлять `enabled: boolean` / `version: int` без явной необходимости
- Создавать интерфейс, если есть только одна реализация и вторая не планируется — кроме port-интерфейсов в `domain/`
- Копировать блок кода в третий раз без выноса в метод
- Писать «универсальный» обработчик, когда нужен конкретный

---

## Стиль кода

### Форматирование

Форматтер: **Spotless + Google Java Format** (настроен в `build.gradle`).

```bash
./gradlew spotlessApply   # применить форматирование
./gradlew spotlessCheck   # проверить без изменений (запускается в CI)
```

- Отступы Java: **2 пробела** (Google Java Format)
- Отступы YAML / Gradle: 2 пробела
- Длина строки: **100 символов**
- Строки: LF, UTF-8, финальный newline

### var

```java
var task = taskStore.findById(id);           // ✅ тип очевиден
var result = mapper.toModel(entity);         // ✅ тип очевиден
Map<UUID, List<TaskModel>> index = new ...   // ✅ сложный generic — тип явно
```

### Lombok

| Аннотация                  | Когда использовать                               |
|----------------------------|--------------------------------------------------|
| `@RequiredArgsConstructor` | DI через конструктор — всегда                    |
| `@Data`                    | Только на `*Properties`-классах                  |
| `@Builder`                 | Там где нужен builder (Entity, фабричные методы) |
| `@UtilityClass`            | Утилитные классы без состояния                   |
| `@Slf4j`                   | Логирование                                      |
| `@SneakyThrows`            | **Запрещено** — скрывает checked exceptions      |

Domain-модели — **record**, не Lombok-классы.

### MapStruct

Маппинг между слоями — только через MapStruct, `componentModel = "spring"`.
Ручная конвертация в сервисах — **запрещена**.

### Логирование

```java
log.debug("Запуск агента: taskId={}, role={}", taskId, role);  // детали выполнения
log.info("Агент завершил задачу: taskId={}", taskId);          // бизнес-события
log.warn("Повторная попытка: attempt={}", attempt);            // нештатные ситуации
log.error("Ошибка выполнения задачи: taskId={}", taskId, e);  // исключения с трейсом
```

- `log.debug` — детали внутри методов, входные параметры
- `log.info` — завершение значимых операций (агент выполнен, задача создана)
- `log.error` — всегда с объектом исключения вторым аргументом

### Прочее

- Комментарии в коде — **на русском**
- Без `I`-префикса у интерфейсов (`AgentStore`, не `IAgentStore`)
