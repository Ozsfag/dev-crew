# App Flow — Как работает Dev Crew

## Концепция

Dev Crew — это бэкенд для команды ИИ-агентов. Архитектор (человек) ставит задачи
через любой интерфейс; агенты автономно выполняют их, используя реальные инструменты
(git, gradle, файловая система). Результат — готовая ветка с кодом, тестами и PR.

## Интерфейсы ввода

| Клиент | Статус | Как работает |
|--------|--------|--------------|
| **REST API** | ✅ Готово | `POST /api/tasks` напрямую |
| **Telegram Bot** | 🔲 П8 | Текст / голос → long-polling → `POST /api/tasks` |
| **IntelliJ IDEA Plugin** | 🔲 П9 | Kotlin plugin → `POST /api/tasks` → Tool Window |

## Схема потока данных

```
Пользователь (Архитектор)
    │
    ├── Telegram Bot (text / voice)
    │       ↓ VoiceTranscriptionService (Whisper)
    │       ↓ TaskParserAgent (LangChain4j)
    │
    ├── IntelliJ IDEA Plugin
    │       ↓ DevCrewApiClient (Kotlin/Ktor)
    │
    └── REST API (curl / Postman)
            │
            ▼
    POST /api/tasks  (JWT-аутентификация)
            │
            ▼
    AgentOrchestrator
    ├── создаёт TaskModel (status=PENDING)
    ├── проверяет лимит плана (PlanLimitPreRunCheck)
    └── запускает агентов @Async
            │
            ▼
    CircularAgentPipeline (П8)
    ┌────────────────────────────────────────┐
    │  [BackendDevAgent]                     │
    │      ↓ FileTools, GitTools             │
    │  [QaAgent]                             │
    │      ↓ GradleTools (./gradlew test)    │
    │      ↓ если тесты красные ──────────┐ │
    │  [CodeReviewAgent]           (loop) ─┘ │
    │      ↓ если REQUEST_CHANGES → loop     │
    │      ↓ если APPROVE                   │
    │  [DocWriterAgent]   (опционально)      │
    │  [DevOpsAgent]      (опционально)      │
    └────────────────────────────────────────┘
            │
            ▼
    TaskModel (status=COMPLETED, result=ветка/PR)
            │
            ▼
    TelegramNotificationAdapter
    → ответ архитектору в Telegram
```

## Что уже реализовано

| Компонент | Пакет | Статус |
|-----------|-------|--------|
| REST API (JWT, CRUD задач) | `task/`, `auth/` | ✅ |
| 5 агентов (BackendDev, QA, CodeReview, DevOps, DocWriter) | `agent/domain/agent/` | ✅ |
| FileTools, GitTools, GradleTools, DockerTools | `agent/adapter/out/tool/` | ✅ |
| AgentOrchestrator + AgentDispatcher | `agent/app/service/` | ✅ |
| Мультитенантность (Organization / Project) | `organization/` | ✅ |
| Биллинг (UsageRecord, TokenEstimationPolicy) | `billing/` | ✅ |
| Rate-limit recovery (RateLimitRetryScheduler) | `agent/bootstrap/` | ✅ |
| Telegram уведомления (исходящие) | `notification/` | ✅ |
| Аудит действий | `audit/` | ✅ |

## Что в Roadmap

| Компонент | План |
|-----------|------|
| Stripe webhook (subscription → plan update) | П7.3 |
| Telegram Bot (входящие сообщения + голос) | П8 |
| CircularAgentPipeline (итеративный цикл) | П8 |
| IntelliJ IDEA Plugin (Tool Window) | П9 |

## Deployment

### Рекомендуемая схема (ghcr.io + GitHub Actions)

```
GitHub Actions
├── push → main
├── ./gradlew test
├── docker build
├── docker push ghcr.io/<user>/dev-crew:latest
└── SSH → сервер → docker compose pull && up -d
```

Сервер: Ubuntu 22.04, 2 GB RAM, Docker — JDK не нужен (образ JRE).

### Конфигурация сервера

```
/opt/dev-crew/
├── docker-compose.yml
├── docker-compose.prod.yml
├── .env                 ← секреты (НЕ в git)
├── repos/               ← /projects mount (репозитории для агентов)
├── pgdata/              ← PostgreSQL данные
└── ssh/                 ← SSH ключ для git push от агентов
```

### .env шаблон

```env
ANTHROPIC_API_KEY=sk-ant-...
DB_PASSWORD=<strong>
DEVCREW_AUTH_JWT_SECRET=<32+ chars>
TELEGRAM_BOT_TOKEN=<bot_token>
TELEGRAM_CHAT_ID=<chat_id>
TELEGRAM_ENABLED=true
TELEGRAM_BOT_ENABLED=false
TELEGRAM_ALLOWED_CHAT_ID=0
STRIPE_WEBHOOK_SECRET=whsec_...
OPENAI_API_KEY=sk-...   # для Whisper (П8)
IMAGE_TAG=latest
```

### Telegram: long-polling vs webhook

Выбор: **long-polling** — не требует домена и HTTPS, достаточно для MVP.
Bot сам опрашивает Telegram API каждые 1-2 секунды.

## Безопасность

- JWT-аутентификация на всех `/api/**` endpoints
- `allowedChatId` — whitelist чата для Telegram Bot (один архитектор)
- Stripe webhook — проверка подписи через `Webhook.constructEvent()`
- `SandboxPolicy` — агенты работают только в `/projects/<name>/` (path traversal защита)
- Все секреты через env vars, ни один не хардкодится в коде
