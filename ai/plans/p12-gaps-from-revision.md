# П12 — Незакрытые пробелы из ревизии планов

## Контекст

Ревизия планов П10–П11 (апрель 2026) выявила пункты, где production-код реализован,
но тесты отсутствуют, либо фичи не завершены. Незакрытые тесты нарушают правило
100% coverage; незавершённые фичи — блокируют API-контракт.

---

## 12.1 Тест: ProcessBuilderCommandRunner таймаут

**Источник**: П11.1 (реализация есть, тест не написан)

**Файл**: `agent/adapter/out/llm/process/ProcessBuilderCommandRunner.java`

**Проблема**: `waitFor(timeoutSeconds, TimeUnit.SECONDS)` и `destroyForcibly()` реализованы,
но тест `run_returns_timeout_result_when_process_exceeds_limit` не существует.

**Решение**:
```java
@Test
void run_returns_timeout_result_when_process_exceeds_limit() {
  // Запустить sleep-команду дольше таймаута (переопределить свойства)
  var properties = new AgentProperties();
  properties.setCommandTimeoutSeconds(1); // 1 секунда
  var runner = new ProcessBuilderCommandRunner(properties);

  var result = runner.run(tempDir, "sleep", "10");

  assertThat(result.exitCode()).isEqualTo(-1);
  assertThat(result.output()).startsWith("TIMEOUT:");
}
```

**Acceptance Criteria**:
- [ ] `ProcessBuilderCommandRunnerTest.run_returns_timeout_result_when_process_exceeds_limit` — зелёный
- [ ] Тест использует таймаут 1 с и команду `sleep 10`

---

## 12.2 Тесты: SandboxPolicy symlink-атака

**Источник**: П11.2 (реализация есть, тесты не написаны)

**Файл**: `agent/app/policy/SandboxPolicy.java`

**Проблема**: `toRealPath()` реализован для защиты от symlink-атаки, но тесты
`validatePath_throws_when_symlink_points_outside_sandbox` и
`validatePath_allows_valid_nonexistent_path_inside_sandbox` отсутствуют.

**Решение**:
```java
@Test
void validatePath_throws_when_symlink_points_outside_sandbox(@TempDir Path sandbox,
                                                              @TempDir Path outside) throws Exception {
  var policy = new SandboxPolicy(sandbox.toString());
  var symlink = sandbox.resolve("evil");
  Files.createSymbolicLink(symlink, outside.resolve("secret.txt"));
  Files.createFile(outside.resolve("secret.txt"));

  assertThatThrownBy(() -> policy.validatePath(symlink.toString()))
      .isInstanceOf(DomainException.class);
}

@Test
void validatePath_allows_valid_nonexistent_path_inside_sandbox(@TempDir Path sandbox) {
  var policy = new SandboxPolicy(sandbox.toString());
  var nonExistent = sandbox.resolve("NewFile.java").toString();

  assertThatNoException().isThrownBy(() -> policy.validatePath(nonExistent));
}
```

**Acceptance Criteria**:
- [ ] Тест с symlink, указывающим наружу sandbox, — выбрасывает `DomainException`
- [ ] Тест с несуществующим, но корректным путём внутри sandbox, — не выбрасывает

---

## 12.3 Тест: JwtProperties валидация при коротком секрете

**Источник**: П11.3 (реализация есть, тест не написан)

**Файл**: `auth/app/config/JwtProperties.java`

**Проблема**: `@NotBlank @Size(min=32)` добавлены на поле `secret`, но тест
`JwtPropertiesValidationTest`, проверяющий, что приложение не стартует при коротком
секрете, не существует.

**Решение**:
```java
@ExtendWith(MockitoExtension.class)
class JwtPropertiesValidationTest {

  @Test
  void secret_fails_validation_when_blank() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var properties = new JwtProperties();
    properties.setSecret("");

    var violations = validator.validate(properties);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("secret"));
  }

  @Test
  void secret_fails_validation_when_shorter_than_32_chars() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var properties = new JwtProperties();
    properties.setSecret("short");

    var violations = validator.validate(properties);

    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("secret"));
  }

  @Test
  void secret_passes_validation_when_32_chars_or_longer() {
    var validator = Validation.buildDefaultValidatorFactory().getValidator();
    var properties = new JwtProperties();
    properties.setSecret("a".repeat(32));

    var violations = validator.validate(properties);

    assertThat(violations).isEmpty();
  }
}
```

**Acceptance Criteria**:
- [ ] `JwtPropertiesValidationTest` — все 3 сценария реализованы и зелёные

---

## 12.4 Тест: Actuator /prometheus недоступен без аутентификации

**Источник**: П11.4 (реализация есть, тест не написан)

**Файл**: `bootstrap/SecurityConfig.java`

**Проблема**: `/actuator/**` закрыт ролью `ARCHITECT`/`VIEWER`, но тест, проверяющий
возврат 401 для `/actuator/prometheus` без токена, отсутствует.

**Решение**: добавить тест в существующий `SecurityConfigTest` или создать новый:
```java
@Test
void GET_actuator_prometheus_returns_401_without_token() throws Exception {
  mockMvc.perform(get("/actuator/prometheus"))
      .andExpect(status().isUnauthorized());
}

@Test
void GET_actuator_health_returns_200_without_token() throws Exception {
  mockMvc.perform(get("/actuator/health"))
      .andExpect(status().isOk());
}
```

**Acceptance Criteria**:
- [ ] Тест `GET /actuator/prometheus` без токена → 401
- [ ] Тест `GET /actuator/health` без токена → 200

---

## 12.5 Тест: TaskJpaStore и AuditJpaStore ограничение размера страницы

**Источник**: П11.6 (реализация есть, тест не написан)

**Файлы**: `task/adapter/out/persistence/store/TaskJpaStore.java`,
`audit/adapter/out/persistence/store/AuditJpaStore.java`

**Проблема**: `MAX_PAGE_SIZE = 100` проверяется через `Math.min(size, MAX_PAGE_SIZE)`,
но тест, подтверждающий, что запрос с `size=99999` ограничивается 100, не написан.

**Решение**: unit-тест через мок `Repository`:
```java
@Test
void findByOrgId_clamps_size_to_100_when_requested_size_exceeds_limit() {
  when(taskRepository.findByOrgId(eq(orgId), any(Pageable.class)))
      .thenReturn(Page.empty());

  store.findByOrgId(orgId, 0, 99999);

  var captor = ArgumentCaptor.<Pageable>captor();
  verify(taskRepository).findByOrgId(eq(orgId), captor.capture());
  assertThat(captor.getValue().getPageSize()).isEqualTo(100);
}
```

Аналогично для `AuditJpaStore`.

**Acceptance Criteria**:
- [ ] `TaskJpaStoreTest.findByOrgId_clamps_size_to_100_when_requested_size_exceeds_limit` — зелёный
- [ ] `AuditJpaStoreTest` — аналогичный тест зелёный

---

## 12.6 БД: result_summary для tasks

**Источник**: П10.7.2 (не реализовано)

**Проблема**: `tasks.result` — TEXT без ограничений. Агент может записать несколько МБ.
List endpoint отдаёт полный `result` для каждой задачи — неэффективно.

**Решение**:
```sql
-- V15__add_result_summary_to_tasks.sql
ALTER TABLE tasks ADD COLUMN result_summary VARCHAR(2000);
```

`TaskCommandService` при `complete()` записывает первые 2000 символов в `result_summary`.
`GET /api/tasks` (list) возвращает `resultSummary`, `GET /api/tasks/{id}` — полный `result`.

**Acceptance Criteria**:
- [ ] Миграция `V15__add_result_summary_to_tasks.sql` создана
- [ ] `TaskModel` содержит поле `resultSummary`
- [ ] `TaskCommandService.complete()` заполняет `resultSummary` (первые 2000 символов)
- [ ] List endpoint возвращает `resultSummary`, не `result`
- [ ] Detail endpoint возвращает полный `result`
- [ ] Тесты для `TaskCommandService` и `TaskController` обновлены

---

## 12.7 API: фильтрация audit по actorId

**Источник**: П10.7.3 (FK создан, фильтрация не реализована)

**Проблема**: `audit_events.actor_id` — FK на users (V14), но
`GET /api/audit?actorId=...` возвращает все события, игнорируя параметр.

**Решение**:
```java
// AuditStore.java
PageResult<AuditEventModel> findByActorId(UUID actorId, int page, int size);

// AuditController.java
@GetMapping
public PageResult<AuditEventResponse> list(
    @RequestParam(required = false) UUID projectId,
    @RequestParam(required = false) UUID actorId,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) { ... }
```

**Acceptance Criteria**:
- [ ] `AuditStore.findByActorId()` — port-метод добавлен
- [ ] `AuditJpaStore.findByActorId()` — реализация через JPA derived query
- [ ] `GET /api/audit?actorId={uuid}` фильтрует по `actor_id`
- [ ] Тест контроллера: `GET_audit_with_actorId_calls_findByActorId`

---

## Приоритет выполнения

| ID   | Приоритет | Трудоёмкость | Тип              |
|------|-----------|--------------|------------------|
| 12.1 | High      | 30 мин       | тест             |
| 12.2 | High      | 30 мин       | тест             |
| 12.3 | Medium    | 20 мин       | тест             |
| 12.4 | Medium    | 20 мин       | тест             |
| 12.5 | Medium    | 20 мин       | тест             |
| 12.6 | Low       | 2 ч          | фича + тест      |
| 12.7 | Low       | 1 ч          | фича + тест      |

## Тест-план

```bash
./gradlew spotlessApply
./gradlew test
```

## Зависимости

- 12.1–12.5 независимы, можно выполнять в любом порядке
- 12.6 требует новой миграции — запустить `./gradlew test -Dspring.profiles.active=tc`
- 12.7 зависит от наличия V14 (уже есть)
