# П9 — IntelliJ IDEA Plugin: интеграция с dev-crew API

## Контекст

Существующий плагин **remote-sync-plugin** (https://github.com/Ozsfag/remote-sync-plugin)
синхронизирует файлы между локальной машиной и удалённым сервером. Планируется добавить
новую фичу: создание задач для dev-crew агентов прямо из IDE и отображение статуса
выполнения в Tool Window.

Разработка ведётся в **отдельной сессии Claude** от репозитория плагина.

## Проблема

Сейчас для создания задачи нужно выходить из IDE: открывать Telegram или Postman.
Цель: создать задачу прямо из IDE — выделил код, нажал кнопку, написал что нужно,
получил результат — не выходя из рабочего окна.

## Техническое решение

### Новая фича в remote-sync-plugin: DevCrew Tool Window

```
IntelliJ IDEA Plugin (Kotlin)
├── DevCrewToolWindowFactory.kt    ← регистрирует Tool Window
├── DevCrewPanel.kt                ← UI: поле ввода + список задач + статус
├── DevCrewApiClient.kt            ← HTTP клиент к dev-crew REST API
├── DevCrewSettings.kt             ← настройки: URL сервера, JWT токен
└── CreateTaskAction.kt            ← Action в контекстном меню (Right Click → Create Dev Task)
```

### API-эндпоинты dev-crew (уже существуют)

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST /api/auth/login` | Body: `{email, password}` | Получить JWT токен |
| `POST /api/tasks` | Body: `{title, description, agentRole}` | Создать задачу |
| `GET /api/tasks/{id}` | — | Получить статус задачи |
| `GET /api/tasks` | Query: `projectId`, `status` | Список задач |

### DevCrewApiClient.kt

```kotlin
class DevCrewApiClient(private val settings: DevCrewSettings) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    suspend fun createTask(title: String, description: String, role: String): TaskResponse {
        return client.post("${settings.serverUrl}/api/tasks") {
            header("Authorization", "Bearer ${settings.jwtToken}")
            contentType(ContentType.Application.Json)
            setBody(CreateTaskRequest(title, description, role))
        }.body()
    }

    suspend fun getTaskStatus(taskId: String): TaskResponse {
        return client.get("${settings.serverUrl}/api/tasks/$taskId") {
            header("Authorization", "Bearer ${settings.jwtToken}")
        }.body()
    }
}
```

### DevCrewSettings (Persistent State)

```kotlin
@State(name = "DevCrewSettings", storages = [Storage("devCrew.xml")])
class DevCrewSettings : PersistentStateComponent<DevCrewSettings> {
    var serverUrl: String = "http://localhost:8081"
    var jwtToken: String = ""
    var orgId: String = ""
    var projectId: String = ""
}
```

### CreateTaskAction.kt (контекстное меню)

```kotlin
class CreateTaskAction : AnAction("Create Dev Task") {
    override fun actionPerformed(e: AnActionEvent) {
        val selectedText = e.getData(PlatformDataKeys.EDITOR)
            ?.selectionModel?.selectedText ?: ""
        // открыть диалог с предзаполненным description = selectedText
        CreateTaskDialog(e.project, selectedText).show()
    }
}
```

### Polling статуса

Плагин поллит `GET /api/tasks/{id}` каждые 5 секунд через `ScheduledExecutorService`
пока статус не `COMPLETED` или `FAILED`. Результат отображается в Tool Window.

### plugin.xml — добавить к существующему

```xml
<!-- Добавить в существующий plugin.xml -->
<extensions defaultExtensionNs="com.intellij">
    <toolWindow id="Dev Crew" anchor="right"
                factoryClass="com.github.ozsfag.devcrew.DevCrewToolWindowFactory"/>
    <applicationService
        serviceImplementation="com.github.ozsfag.devcrew.DevCrewSettings"/>
    <applicationConfigurable
        instance="com.github.ozsfag.devcrew.DevCrewConfigurable"
        displayName="Dev Crew"/>
</extensions>
<actions>
    <action id="DevCrew.CreateTask"
            class="com.github.ozsfag.devcrew.CreateTaskAction"
            text="Create Dev Task">
        <add-to-group group-id="EditorPopupMenu" anchor="last"/>
    </action>
</actions>
```

## Acceptance Criteria

- [ ] Tool Window "Dev Crew" открывается в IDE
- [ ] Настройки: Server URL, JWT Token, OrgId, ProjectId сохраняются между сессиями
- [ ] `Create Dev Task` появляется в контекстном меню редактора
- [ ] Выделенный текст автоматически попадает в поле описания задачи
- [ ] Задача создаётся через `POST /api/tasks` с JWT авторизацией
- [ ] Статус задачи поллится и отображается в Tool Window
- [ ] При `COMPLETED` — результат (ветка/PR ссылка) отображается в Tool Window
- [ ] При `FAILED` — сообщение об ошибке
- [ ] Плагин совместим с IntelliJ IDEA 2024.1+

## Тест-план

```bash
# В репозитории плагина:
./gradlew test
./gradlew runIde    # запуск в sandbox IDE для ручной проверки
```

## Зависимости

- dev-crew должен быть развёрнут (локально или на сервере)
- `POST /api/auth/login` должен возвращать JWT для ввода в настройки
- Разработка в **отдельной сессии Claude** от репозитория https://github.com/Ozsfag/remote-sync-plugin

## Заметки для отдельной сессии

1. Клонировать `https://github.com/Ozsfag/remote-sync-plugin`
2. Прочитать существующую структуру плагина (plugin.xml, существующие компоненты)
3. Добавить новую фичу без изменения существующей sync-функциональности
4. Использовать Kotlin Coroutines + Ktor Client для HTTP вызовов
5. Tool Window — стандартный JPanel с Swing или IntelliJ UI DSL
