# П8 — Telegram Bot: входящие сообщения + голос + Circular Pipeline

## Контекст

Сейчас dev-crew принимает задачи только через REST API. Для продуктивной работы
архитектора нужен Telegram-интерфейс: написал команду или надиктовал голосовое —
агенты выполнили, пришёл ответ в тот же чат. Уведомления в Telegram уже работают (✅),
нужно добавить приём входящих сообщений.

Дополнительно: текущий pipeline запускает одного агента на задачу. Для самостоятельной
работы над кодом нужен Circular Pipeline: BackendDev → QA → CodeReview → loop, если
нужно → DocWriter/DevOps по запросу.

## Проблема

1. **Нет Telegram Bot Adapter** — не принимаем входящие сообщения (text + voice)
2. **Нет голосовой транскрипции** — Whisper API не подключён
3. **Нет NLP-парсинга задачи** — свободный текст нужно превратить в `TaskModel`
4. **Нет Circular Pipeline** — один агент не может итеративно улучшать код

## Техническое решение

### Архитектура компонентов

```
TelegramBotAdapter (long-polling)
    ↓ text / voice
VoiceTranscriptionService (Whisper)
    ↓ text
TaskParserAgent (LangChain4j)
    ↓ CreateTaskRequest
AgentOrchestrator
    ↓
CircularAgentPipeline
    ├── BackendDevAgent → пишет/изменяет код
    ├── QaAgent         → пишет тесты, запускает ./gradlew test
    │       ↓ если тесты красные → возврат к BackendDevAgent (max 3 итерации)
    ├── CodeReviewAgent → APPROVE / REQUEST_CHANGES
    │       ↓ если REQUEST_CHANGES → возврат к BackendDevAgent
    ├── DocWriterAgent  (опционально, если запрошено)
    └── DevOpsAgent     (опционально, если запрошено)
    ↓ итоговый результат
TelegramNotificationAdapter → ответ в чат
```

### 1. Зависимости

```groovy
// build.gradle
implementation 'org.telegram:telegrambots-spring-boot-starter:6.9.7.1'
// Whisper — через OpenAI Java SDK или прямой HTTP-клиент
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

### 2. TelegramBotProperties

```java
// notification/app/config/TelegramProperties.java — добавить поля:
private boolean botEnabled = false;       // приём входящих (отдельно от enabled)
private String botUsername = "";          // @username бота для регистрации
private long allowedChatId = 0L;          // whitelist: только этот chatId принимается
```

```yaml
devcrew:
  notification:
    telegram:
      bot-enabled: ${TELEGRAM_BOT_ENABLED:false}
      bot-username: ${TELEGRAM_BOT_USERNAME:}
      allowed-chat-id: ${TELEGRAM_ALLOWED_CHAT_ID:0}
```

### 3. TelegramBotAdapter (long-polling)

```java
// notification/adapter/in/telegram/TelegramBotAdapter.java
@Component
@ConditionalOnProperty("devcrew.notification.telegram.bot-enabled")
public class TelegramBotAdapter extends TelegramLongPollingBot {

  private final TelegramProperties properties;
  private final TelegramInboundService inboundService;

  @Override
  public void onUpdateReceived(Update update) {
    if (!isAllowedChat(update)) return;

    if (update.hasMessage()) {
      var message = update.getMessage();
      if (message.hasText()) {
        inboundService.handleText(message.getChatId(), message.getText());
      } else if (message.hasVoice()) {
        inboundService.handleVoice(message.getChatId(), message.getVoice().getFileId());
      }
    }
  }

  private boolean isAllowedChat(Update update) {
    var chatId = update.getMessage().getChatId();
    return properties.getAllowedChatId() == 0 || chatId == properties.getAllowedChatId();
  }
}
```

### 4. TelegramInboundService

```java
// notification/app/service/TelegramInboundService.java
@Service
@RequiredArgsConstructor
public class TelegramInboundService {

  private final VoiceTranscriptionService transcriptionService;
  private final TaskParserAgent taskParserAgent;
  private final AgentOrchestrator agentOrchestrator;
  private final TelegramNotificationAdapter notificationAdapter;

  public void handleText(Long chatId, String text) {
    var request = taskParserAgent.parse(text);
    var taskId = agentOrchestrator.start(request);
    notificationAdapter.send("Задача принята: " + taskId + "\nОбрабатываю...");
  }

  public void handleVoice(Long chatId, String fileId) {
    var audioBytes = downloadFile(fileId);
    var text = transcriptionService.transcribe(audioBytes);
    handleText(chatId, text);
  }
}
```

### 5. VoiceTranscriptionService (Whisper)

```java
// notification/adapter/out/whisper/WhisperTranscriptionAdapter.java
@Service
@RequiredArgsConstructor
public class WhisperTranscriptionAdapter implements VoiceTranscriptionService {

  private final WhisperProperties properties;
  private final OkHttpClient httpClient;

  @Override
  public String transcribe(byte[] audioBytes) {
    var requestBody = new MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", "voice.ogg",
            RequestBody.create(audioBytes, MediaType.parse("audio/ogg")))
        .addFormDataPart("model", "whisper-1")
        .build();
    // POST https://api.openai.com/v1/audio/transcriptions
    // Authorization: Bearer ${OPENAI_API_KEY}
    ...
  }
}
```

### 6. TaskParserAgent (LangChain4j)

```java
// agent/domain/agent/TaskParserAgent.java — новый агент-парсер
public interface TaskParserAgent {
  /**
   * Парсит свободный текст от пользователя и возвращает структурированный запрос на задачу.
   */
  CreateTaskRequest parse(String userMessage);
}
```

Системный промпт: `src/main/resources/prompts/task-parser.md`

```markdown
You are a task parser for a dev-crew system.
Parse the user's free-form message into a structured task request.
Extract: title (short), description (detailed), agentRole (BACKEND_DEV/QA/CODE_REVIEWER/DEVOPS/DOC_WRITER).
Return valid JSON: {"title": "...", "description": "...", "agentRole": "BACKEND_DEV"}.
If agentRole is unclear, default to BACKEND_DEV.
```

### 7. CircularAgentPipeline

```java
// agent/app/service/execution/CircularAgentPipeline.java
@Service
@RequiredArgsConstructor
public class CircularAgentPipeline {

  private final AgentDispatcher dispatcher;
  private final AgentProperties properties;   // maxIterations

  public String execute(String task, UUID projectPath) {
    String lastResult = null;
    int iteration = 0;

    // Фаза 1: BackendDev пишет код
    lastResult = dispatcher.dispatch(AgentRole.BACKEND_DEV, task);

    while (iteration++ < properties.getMaxIterations()) {
      // Фаза 2: QA пишет и запускает тесты
      var qaResult = dispatcher.dispatch(AgentRole.QA, buildQaPrompt(task, lastResult));
      if (qaResult.contains("BUILD SUCCESSFUL")) break;

      // Тесты красные — возврат к BackendDev
      lastResult = dispatcher.dispatch(AgentRole.BACKEND_DEV, buildFixPrompt(task, qaResult));
    }

    // Фаза 3: CodeReview
    var reviewResult = dispatcher.dispatch(AgentRole.CODE_REVIEWER, lastResult);
    if (reviewResult.contains("REQUEST_CHANGES")) {
      lastResult = dispatcher.dispatch(AgentRole.BACKEND_DEV, buildReviewFixPrompt(reviewResult));
    }

    return lastResult;
  }
}
```

## Acceptance Criteria

- [ ] `TelegramBotAdapter` регистрируется только при `bot-enabled: true` (`@ConditionalOnProperty`)
- [ ] Текстовые сообщения → задача создаётся и запускается
- [ ] Голосовые сообщения → транскрибируются через Whisper → задача создаётся
- [ ] Только `allowedChatId` может отправлять задачи (безопасность)
- [ ] `TaskParserAgent` парсит свободный текст в `CreateTaskRequest`
- [ ] `CircularAgentPipeline` итерирует BackendDev → QA (max `maxIterations` раз)
- [ ] `CircularAgentPipeline` запускает `CodeReviewAgent` после успешных тестов
- [ ] Финальный результат отправляется в Telegram через `TelegramNotificationAdapter`
- [ ] Все новые сервисы покрыты unit-тестами (100% coverage)
- [ ] `./gradlew test` зелёный

## Тест-план

```bash
./gradlew test --tests "*.notification.*"
./gradlew test --tests "*.agent.*"

# Ручная проверка: отправить сообщение боту и убедиться, что задача создалась
```

## Зависимости

- ✅ `TelegramNotificationAdapter` — уже реализован
- ✅ `AgentOrchestrator` — уже реализован
- ✅ `AgentDispatcher` — уже реализован
- 🔲 Нужен `OPENAI_API_KEY` в `.env` для Whisper (или замена на Cloudflare AI / другой провайдер)
- 🔲 Нужен Telegram Bot Token с настроенным webhook / long-polling
