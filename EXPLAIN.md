# Анализ EXPLAIN для поискового запроса

Этот документ содержит пример поискового запроса с использованием кастомной SQL-функции `search_documents_with_cursor`, вывод EXPLAIN ANALYZE и краткое объяснение индексов.

## Пример поискового запроса
Этот запрос ищет документы в статусе 'DRAFT' от автора 'author1', созданные между 2024-01-01 и 2026-12-31, с курсором NULL (первая страница) и лимитом 20. Фильтры по `created_at`.

```sql
SELECT * FROM entity_tables.search_documents_with_cursor(
    'DRAFT',         -- p_status
    'author1',       -- p_author (LIKE-матчинг)
    '2024-01-01'::timestamp,  -- p_from
    '2026-12-31'::timestamp,  -- p_to
    NULL,            -- p_cursor (начало)
    20               -- p_limit
);
```
### Вывод EXPLAIN ANALYZE
Для 1100+ строк в таблице анализ выдал:
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
