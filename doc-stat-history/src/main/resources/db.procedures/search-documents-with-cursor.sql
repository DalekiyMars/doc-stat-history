CREATE OR REPLACE FUNCTION entity_tables.search_documents_with_cursor(
    p_status  VARCHAR,
    p_author  VARCHAR,
    p_from    TIMESTAMP,
    p_to      TIMESTAMP,
    p_cursor  BIGINT,
    p_limit   INT
)
RETURNS TABLE (
    id            BIGINT,
    author        VARCHAR,
    unique_number VARCHAR,
    doc_name      VARCHAR,
    status        VARCHAR,
    last_update   TIMESTAMP,
    created_at    TIMESTAMP,
    version       BIGINT
)
LANGUAGE sql
STABLE
AS $$
SELECT
    d.id,
    d.author,
    d.unique_number,
    d.doc_name,
    d.status,
    d.last_update,
    d.created_at,
    d.version
FROM entity_tables.documents d
WHERE (p_status IS NULL OR d.status      = p_status)
  AND (p_author IS NULL OR d.author LIKE  p_author)
  AND (p_from   IS NULL OR d.created_at >= p_from)
  AND (p_to     IS NULL OR d.created_at <= p_to)
  AND (p_cursor IS NULL OR d.id          > p_cursor)
ORDER BY d.id
    LIMIT p_limit;
$$;