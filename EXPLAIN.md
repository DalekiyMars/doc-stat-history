# Анализ EXPLAIN для поискового запроса и запросов воркеров

## 1. Поисковый запрос (динамические фильтры)

В сервисе используется курсорная пагинация через функцию `search_documents_with_cursor`. Функция выполняет следующий SQL:

```sql
SELECT d.id, d.author, d.unique_number, d.doc_name, d.status, d.last_update, d.created_at, d.version
FROM entity_tables.documents d
WHERE (p_status IS NULL OR d.status = p_status)
  AND (p_author IS NULL OR d.author LIKE p_author)
  AND (p_from   IS NULL OR d.created_at >= p_from)
  AND (p_to     IS NULL OR d.created_at <= p_to)
  AND (p_cursor IS NULL OR d.id > p_cursor)
ORDER BY d.id
LIMIT p_limit;
```

Пример вызова с параметрами:

    p_status = 'DRAFT'
    p_author = 'author1'
    p_from = '2024-01-01'
    p_to = '2026-12-31'
    p_cursor = NULL (первая страница)
    p_limit = 20

### EXPLAIN ANALYZE (на таблице с ~1100 строк)
```sql
Limit  (cost=8.31..8.32 rows=1 width=160) (actual time=0.118..0.119 rows=0 loops=1)
->  Sort  (cost=8.31..8.32 rows=1 width=160) (actual time=0.118..0.118 rows=0 loops=1)
    Sort Key: d.id
    Sort Method: quicksort  Memory: 25kB
->  Index Scan using idx_documents_author_created on documents d  (cost=0.28..8.30 rows=1 width=160) (actual time=0.076..0.077 rows=0 loops=1)
Index Cond: (((author)::text = 'author1'::text) AND (created_at >= '2024-01-01 00:00:00'::timestamp without time zone) AND (created_at <= '2026-12-31 00:00:00'::timestamp without time zone))
    Filter: (((author)::text ~~ 'author1'::text) AND ((status)::text = 'DRAFT'::text))
    Planning Time: 0.698 ms
    Execution Time: 0.155 ms
```
Пояснение:
    Планировщик выбрал Index Scan по составному индексу idx_documents_author_created (author, created_at), так как фильтры по автору и дате наиболее селективны.

    Дополнительная фильтрация по статусу выполняется как Filter после сканирования индекса (статус не входит в этот индекс). Для улучшения можно создать индекс (author, status, created_at), но текущий набор индексов покрывает основные сценарии (см. ниже).
    Сортировка выполняется в памяти (quicksort), так как количество строк после фильтрации невелико
    Время выполнения менее 1 мс, что приемлемо.

## 2. Запрос воркеров (выборка ID по статусу)
Воркеры (SubmitWorker и ApproveWorker) периодически выбирают пачки документов для обработки:
```sql
SELECT d.id
FROM entity_tables.documents d
WHERE d.status = 'DRAFT'   -- или 'SUBMITTED'
ORDER BY d.id
LIMIT :batchSize;
```

### EXPLAIN ANALYZE для статуса 'DRAFT' (batchSize = 50)
```sql
Limit  (cost=0.28..1.69 rows=50 width=8) (actual time=0.015..0.049 rows=50 loops=1)
->  Index Only Scan using idx_documents_draft on documents d  (cost=0.28..29.12 rows=1050 width=8) (actual time=0.014..0.041 rows=50 loops=1)
    Index Cond: (status = 'DRAFT'::text)
    Heap Fetches: 0
Planning Time: 0.124 ms
Execution Time: 0.073 ms
```

Пояснение:
    Используется частичный индекс idx_documents_draft, созданный как:
```sql
    CREATE INDEX idx_documents_draft ON entity_tables.documents(id) WHERE status = 'DRAFT';
```
Это позволяет очень быстро получать ID документов в статусе DRAFT без сканирования всей таблицы.
Аналогичный индекс idx_documents_submitted для статуса SUBMITTED.

## 3. Индексы, созданные в проекте

В миграциях Liquibase (файл add-indexes.xml) добавлены следующие индексы:
    
| Индекс                                                          |                             Назначение                              |
|:----------------------------------------------------------------|:-------------------------------------------------------------------:|
| idx_documents_status_created (status, created_at)               | Ускоряет фильтрацию по статусу + сортировку/фильтр по дате создания |
| idx_documents_author_created (author, created_at)               |                  	Ускоряет поиск по автору + дате                   |
| idx_documents_created_at (created_at)                           |    Для диапазонных запросов по дате создания без других фильтров    |
| idx_audit_document (document_id)                                |        Ускоряет загрузку аудита для документа (внешний ключ)        |
| Частичные индексы idx_documents_draft и idx_documents_submitted |            Для быстрой выборки ID по статусу в воркерах             | 

Такая комбинация покрывает основные сценарии использования: поиск с фильтрами и пакетную обработку.