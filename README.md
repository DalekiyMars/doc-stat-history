# Сервис управления статусами документов

Это backend-сервис для работы с документами со статусами (DRAFT, SUBMITTED, APPROVED), историей аудита и реестром утверждений. Включает фоновые workers для батч-обработки и утилиту для массового создания документов.

## Стек технологий
- Java + Spring Boot
- PostgreSQL (через Docker Compose)
- JPA/Hibernate
- Liquibase для миграций
- Gradle для сборки

## Предварительные требования
- Java 21+ (JDK)
- Gradle 7+ (для сборки)
- Docker (для Postgres)

## Как запустить сервис
1. **Запуск Postgres через Docker Compose**:
   - Перейдите в корень проекта.
   - Выполните: `docker-compose -f compose.yaml up -d`
   - Это запустит Postgres на порту 13000 (БД: document-util, пользователь: admin, пароль: secret).

2. **Сборка проекта**:
   - Выполните: `./gradlew build` (или `gradlew.bat build` на Windows).
   - Это создаст `build/libs/doc-stat-history-<version>.jar`. На данный момент version: 0.0.2

3. **Запуск сервиса**: УЖЕ СОБРАНО
   - Выполните: `java -jar build/libs/doc-stat-history-<version>.jar`
   - Или из IDE: Запустите основной класс `com.ITQGroup.doc_stat_history.DocStatHistoryApplication`.
   - Сервис стартует на http://localhost:8080 (порт по умолчанию Spring Boot).
   - Liquibase автоматически применит миграции при запуске (схемы, таблицы, индексы, SQL-функции).

4. **Конфигурация**:
   - Редактируйте `src/main/resources/application.yaml` для кастомных настроек (например, batch-size workers, задержки, префикс документов).
   - Логи: Просматривайте консоль или `logs/app.log`. Используйте уровни INFO/WARN для прогресса.

## Как запустить утилиту (массовое создание)
Утилита (`Main.java`) — standalone консольное приложение для создания батчей документов через API сервиса.

1. **Сборка/компиляция**:
   - Если отдельный модуль: `./gradlew build` в директории `doc-stat-util`.
   - Или вручную: `javac Main.java`.

2. **Файл конфигурации**:
   - Редактируйте `doc-stat-util/src/config.txt` (или передайте как аргумент):
      serviceUrl=http://localhost:8080/api/v1/documents/batch
      batches=10
      batchSize=100
      delayMs=1000
      author=utility-author
      docNamePrefix=TestDoc-
      initiator=system
    - `serviceUrl`: Эндпоинт API.
    - `batches`: Количество батчей.
    - `batchSize`: Документов в батче.
    - `delayMs`: Задержка между батчами.

3. **Запуск**:
- `java Main <config-file>` (например, `java Main doc-stat-util/src/config.txt`).
- По умолчанию: `doc-stat-util/src/config.txt`, если без аргумента.
- Вывод: Логи в консоль, типа "Batch 1/10: Created 100 documents in 500ms".

## Как проверить прогресс по логам
- **Логи сервиса**: 
- Workers: Ищите "SUBMIT-worker START" или "APPROVE-worker DONE". Показывают размер батча, оставшиеся документы, время выполнения, результаты (SUCCESS/CONFLICT и т.д.).
- Пример: "APPROVE-worker: processed=50, summary={SUCCESS=45, CONFLICT=5}, elapsed=200ms"
- Переходы: "Document 123 approved by system".
- Ошибки: Уровни WARN/ERROR.
- **Логи утилиты**: Консоль показывает прогресс по батчам, общее время, количество ответов.
- **Мониторинг**: Следите за логами через `tail -f logs/app.log`.
- **Проверка в БД**: Используйте pgAdmin/psql для запроса `entity_tables.documents`: `SELECT status, COUNT(*) FROM entity_tables.documents GROUP BY status;`.

## Примеры API
- Создание: `POST /api/v1/documents` с JSON `{ "author": "user", "docName": "Doc1", "initiator": "user" }`
- Поиск: `GET /api/v1/documents/search?status=DRAFT&author=user&limit=20`

## Тесты
Тесты включены (юнит/интеграционные). Запуск: `./gradlew test`.

## Возможные улучшения:
4. **Обработка запросов с 5000+ id**
   Текущая реализация ограничивает размер батчей до 1000 id (через @Size(max=1000) в валидации для BatchTransitionRequest и BatchCreateRequest), чтобы избежать перегрузки памяти и БД. Для уверенной работы с 5000+ id в одном запросе я бы внес изменения:
   - Асинхронная обработка: Разделить список id на подбатчи (например, по 1000) и обрабатывать их параллельно с помощью @Async или ExecutorService (как в ConcurrentApprovalService). Это распределит нагрузку и ускорит выполнение.
   - Очереди сообщений: Вынести обработку в очереди — запрос добавляет задачу в очередь, а background-воркер (расширенный от текущих) обрабатывает пачками. Это обеспечит отказоустойчивость и масштабируемость.
   - Мониторинг: Добавить метрики (Spring Actuator) для времени обработки и памяти, с лимитами на запрос (например, max 10000 id).
5. **Вынос реестра утверждений в отдельную систему**
   Текущий реестр (ApprovalRegistry) интегрирован в ту же БД (schema entity_tables). Для выноса в отдельную систему:
   - Отдельная БД: Настроить multi-datasource в Spring. Создать @Bean для второго DataSource. В TransitionExecutor инжектировать отдельный repository. Транзакции: Использовать @Transactional с ChainedTransactionManager для кросс-БД запросов.
   - Отдельный HTTP-сервис: Вынести реестр в микросервис (отдельный Spring Boot app). В основном сервисе использовать RestTemplate для POST /registry/approve с данными (documentId, initiator). Обеспечить идемпотентность unique key на documentId и retry.
