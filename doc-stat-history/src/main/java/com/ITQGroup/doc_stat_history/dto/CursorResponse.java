package com.ITQGroup.doc_stat_history.dto;

import java.util.List;

public record CursorResponse<T>(
        List<T> content,
        Long nextCursor,   // id последнего элемента
        boolean hasNext    // false если вернулось меньше чем limit строк
) {
    public static <T> CursorResponse<T> of(List<T> content, int limit, Long lastId) {
        boolean hasNext = content.size() == limit;
        return new CursorResponse<>(content, hasNext ? lastId : null, hasNext);
    }
}